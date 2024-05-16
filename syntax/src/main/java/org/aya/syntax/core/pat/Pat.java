// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.pat;

import kala.collection.SeqLike;
import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import kala.function.CheckedFunction;
import kala.function.CheckedSupplier;
import kala.tuple.Tuple2;
import kala.value.MutableValue;
import org.aya.generic.AyaDocile;
import org.aya.prettier.BasePrettier;
import org.aya.prettier.CorePrettier;
import org.aya.prettier.Tokens;
import org.aya.pretty.doc.Doc;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.ConDef;
import org.aya.syntax.core.repr.ShapeRecognition;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.ref.DefVar;
import org.aya.syntax.ref.GenerateKind;
import org.aya.syntax.ref.LocalCtx;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.Arg;
import org.aya.util.error.Panic;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.aya.util.prettier.PrettierOptions;
import org.jetbrains.annotations.Debug;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Patterns in the core syntax.
 *
 * @author kiva, ice1000, HoshinoTented
 */
@Debug.Renderer(text = "PatToTerm.visit(this).debuggerOnlyToDoc()")
public sealed interface Pat extends AyaDocile {
  @NotNull Pat descent(@NotNull UnaryOperator<Pat> patOp, @NotNull UnaryOperator<Term> termOp);

  /**
   * The order of bindings should be postorder, that is, {@code (Con0 a (Con1 b)) as c} should be {@code [a , b , c]}
   */
  void consumeBindings(@NotNull BiConsumer<LocalVar, Term> consumer);

  default ImmutableSeq<Tuple2<LocalVar, Term>> collectBindings() {
    var buffer = MutableList.<Tuple2<LocalVar, Term>>create();
    consumeBindings((var, type) -> buffer.append(kala.tuple.Tuple.of(var, type)));
    return buffer.toImmutableSeq();
  }

  static @NotNull ImmutableSeq<Tuple2<LocalVar, Term>> collectBindings(@NotNull SeqView<Pat> pats) {
    return pats.flatMap(Pat::collectBindings).toImmutableSeq();
  }

  /**
   * Replace {@link Pat.Meta} with {@link Pat.Meta#solution} (if there is) or {@link Pat.Bind}
   */
  @NotNull Pat inline(@NotNull BiConsumer<LocalVar, Term> bind);

  enum Absurd implements Pat {
    INSTANCE;

    @Override public @NotNull Pat descent(@NotNull UnaryOperator<Pat> patOp, @NotNull UnaryOperator<Term> termOp) {
      return this;
    }

    @Override public void consumeBindings(@NotNull BiConsumer<LocalVar, Term> consumer) { }
    @Override public @NotNull Pat inline(@NotNull BiConsumer<LocalVar, Term> bind) { return this; }
  }

  @Override default @NotNull Doc toDoc(@NotNull PrettierOptions options) {
    return new CorePrettier(options).pat(this, true, BasePrettier.Outer.Free);
  }

  record Bind(
    @NotNull LocalVar bind,
    @NotNull Term type
  ) implements Pat {
    public @NotNull Bind update(@NotNull Term type) {
      return this.type == type ? this : new Bind(bind, type);
    }

    @Override
    public @NotNull Pat descent(@NotNull UnaryOperator<Pat> patOp, @NotNull UnaryOperator<Term> termOp) {
      return update(termOp.apply(type));
    }

    @Override public void consumeBindings(@NotNull BiConsumer<LocalVar, Term> consumer) {
      consumer.accept(bind, type);
    }

    @Override public @NotNull Pat inline(@NotNull BiConsumer<LocalVar, Term> bind) { return this; }
  }

  record Tuple(@NotNull ImmutableSeq<Pat> elements) implements Pat {
    public @NotNull Tuple update(@NotNull ImmutableSeq<Pat> elements) {
      return this.elements.sameElements(elements, true) ? this : new Tuple(elements);
    }

    @Override
    public @NotNull Pat descent(@NotNull UnaryOperator<Pat> patOp, @NotNull UnaryOperator<Term> termOp) {
      return update(elements.map(patOp));
    }

    @Override public void consumeBindings(@NotNull BiConsumer<LocalVar, Term> consumer) {
      elements.forEach(e -> e.consumeBindings(consumer));
    }

    @Override public @NotNull Pat inline(@NotNull BiConsumer<LocalVar, Term> bind) {
      return update(elements.map(x -> x.inline(bind)));
    }
  }

  record Con(
    @NotNull DefVar<ConDef, TeleDecl.DataCon> ref,
    @NotNull ImmutableSeq<Pat> args,
    @Nullable ShapeRecognition typeRecog,
    @NotNull DataCall data
  ) implements Pat {
    public @NotNull Pat.Con update(@NotNull ImmutableSeq<Pat> args) {
      return this.args.sameElements(args, true) ? this : new Con(ref, args, typeRecog, data);
    }

    @Override public @NotNull Pat descent(@NotNull UnaryOperator<Pat> patOp, @NotNull UnaryOperator<Term> termOp) {
      return update(args.map(patOp));
    }

    @Override public void consumeBindings(@NotNull BiConsumer<LocalVar, Term> consumer) {
      args.forEach(arg -> arg.consumeBindings(consumer));
    }

    @Override public @NotNull Pat inline(@NotNull BiConsumer<LocalVar, Term> bind) {
      return update(args.map(x -> x.inline(bind)));
    }
  }

  /**
   * Meta for Hole
   *
   * @param solution the solution of this Meta.
   *                 Note that the solution (and its sub pattern) never contains a {@link Pat.Bind}.
   * @param fakeBind is used when inline if there is no solution.
   *                 So don't add this to {@link LocalCtx} too early
   *                 and remember to inline Meta in <code>ClauseTycker.checkLhs</code>
   */
  record Meta(
    @NotNull MutableValue<Pat> solution,
    @NotNull String fakeBind,
    @NotNull Term type,
    @NotNull SourcePos errorReport
  ) implements Pat {
    public @NotNull Meta update(@Nullable Pat solution, @NotNull Term type) {
      return solution == solution().get() && type == type()
        ? this : new Meta(MutableValue.create(solution), fakeBind, type, errorReport);
    }

    @Override public @NotNull Meta descent(@NotNull UnaryOperator<Pat> f, @NotNull UnaryOperator<Term> g) {
      var solution = solution().get();
      return solution == null ? update(null, g.apply(type)) : update(f.apply(solution), g.apply(type));
    }

    @Override public void consumeBindings(@NotNull BiConsumer<LocalVar, Term> consumer) {
      // We should call storeBindings after inline
      Panic.unreachable();
    }

    @Override public @NotNull Pat inline(@NotNull BiConsumer<LocalVar, Term> bind) {
      var solution = this.solution.get();
      if (solution == null) {
        var name = new LocalVar(fakeBind, errorReport, GenerateKind.Basic.Anonymous);
        bind.accept(name, type);
        solution = new Bind(name, type);
        // We need to set solution if no solution
        this.solution.set(solution);
        return solution;
      } else {
        return solution.inline(bind);
      }
    }

    public <R> @NotNull R map(@NotNull Function<Pat, R> mapper, @NotNull Supplier<R> aDefaults) {
      var solution = solution().get();
      if (solution == null) return aDefaults.get();
      return mapper.apply(solution);
    }

    public <R, Ex extends Throwable> @NotNull R mapChecked(
      @NotNull CheckedFunction<Pat, R, Ex> mapper,
      @NotNull CheckedSupplier<R, Ex> aDefaults
    ) throws Ex {
      var solution = solution().get();
      if (solution == null) return aDefaults.getChecked();
      return mapper.applyChecked(solution);
    }
  }

  /**
   * It's 'pre' because there are also impossible clauses, which are removed after tycking.
   *
   * @author ice1000
   */
  record Preclause<T extends AyaDocile>(
    @NotNull SourcePos sourcePos,
    @NotNull ImmutableSeq<Pat> pats,
    @Nullable WithPos<T> expr
  ) implements AyaDocile {
    @Override public @NotNull Doc toDoc(@NotNull PrettierOptions options) {
      var prettier = new CorePrettier(options);
      var doc = Doc.emptyIf(pats.isEmpty(), () -> Doc.cat(Doc.ONE_WS, Doc.commaList(
        pats.view().map(p -> prettier.pat(Arg.ofExplicitly(p), BasePrettier.Outer.Free)))));
      return expr == null ? doc : Doc.sep(doc, Tokens.FN_DEFINED_AS, expr.data().toDoc(options));
    }

    public static @NotNull Preclause<Term> weaken(@NotNull Term.Matching clause) {
      return new Preclause<>(clause.sourcePos(), clause.patterns(), WithPos.dummy(clause.body()));
    }

    public static @Nullable Term.Matching lift(@NotNull Preclause<Term> clause) {
      if (clause.expr == null) return null;
      return new Term.Matching(clause.sourcePos, clause.pats, clause.expr.data());
    }
  }
}
