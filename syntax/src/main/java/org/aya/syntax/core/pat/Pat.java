// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.pat;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableAnyList;
import kala.collection.mutable.MutableList;
import kala.tuple.Tuple;
import kala.tuple.Tuple2;
import kala.value.MutableValue;
import org.aya.generic.AyaDocile;
import org.aya.prettier.BasePrettier;
import org.aya.prettier.CorePrettier;
import org.aya.pretty.doc.Doc;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.CtorDef;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.DefVar;
import org.aya.syntax.ref.GenerateKind;
import org.aya.syntax.ref.LocalCtx;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.Arg;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.aya.util.prettier.PrettierOptions;
import org.jetbrains.annotations.Debug;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.*;

/**
 * Patterns in the core syntax.
 *
 * @author kiva, ice1000, HoshinoTented
 */
@Debug.Renderer(text = "toTerm().toDoc(AyaPrettierOptions.debug()).debugRender()")
public sealed interface Pat extends AyaDocile {
  @NotNull Pat descent(@NotNull UnaryOperator<Pat> patOp, @NotNull UnaryOperator<Term> termOp);

  /**
   * Puts bindings of this {@link Pat} to {@param ctx}
   */
  void consumeBindings(@NotNull BiConsumer<LocalVar, Term> consumer);

  default ImmutableSeq<Tuple2<LocalVar, Term>> collectBindings() {
    var buffer = MutableList.<Tuple2<LocalVar, Term>>create();

    consumeBindings((var, type) -> buffer.append(kala.tuple.Tuple.of(var, type)));

    return buffer.toImmutableSeq();
  }

  /**
   * Replace {@link Pat.Meta} with {@link Pat.Meta#solution} (if there is) or {@link Pat.Bind}
   */
  @NotNull Pat inline(@NotNull LocalCtx ctx);

  enum Absurd implements Pat {
    INSTANCE;

    @Override
    public @NotNull Pat descent(@NotNull UnaryOperator<Pat> patOp, @NotNull UnaryOperator<Term> termOp) {
      return this;
    }

    @Override
    public void consumeBindings(@NotNull BiConsumer<LocalVar, Term> consumer) {
    }

    @Override
    public @NotNull Pat inline(@NotNull LocalCtx ctx) {
      return this;
    }
  }

  @Override
  default @NotNull Doc toDoc(@NotNull PrettierOptions options) {
    throw new UnsupportedOperationException("TODO");
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

    @Override
    public void consumeBindings(@NotNull BiConsumer<LocalVar, Term> consumer) {
      consumer.accept(bind, type);
    }

    @Override
    public @NotNull Pat inline(@NotNull LocalCtx ctx) {
      return this;
    }
  }

  record Tuple(@NotNull ImmutableSeq<Pat> elements) implements Pat {
    public @NotNull Tuple update(@NotNull ImmutableSeq<Pat> elements) {
      return this.elements.sameElements(elements, true) ? this : new Tuple(elements);
    }

    @Override
    public @NotNull Pat descent(@NotNull UnaryOperator<Pat> patOp, @NotNull UnaryOperator<Term> termOp) {
      return update(elements.map(patOp));
    }

    @Override
    public void consumeBindings(@NotNull BiConsumer<LocalVar, Term> consumer) {
      elements.forEach(e -> e.consumeBindings(consumer));
    }

    @Override
    public @NotNull Pat inline(@NotNull LocalCtx ctx) {
      return update(elements.map(x -> x.inline(ctx)));
    }
  }

  record Ctor(
    @NotNull DefVar<CtorDef, TeleDecl.DataCtor> ref,
    @NotNull ImmutableSeq<Pat> args
  ) implements Pat {
    public @NotNull Ctor update(@NotNull ImmutableSeq<Pat> args) {
      return this.args.sameElements(args, true) ? this : new Ctor(ref, args);
    }


    @Override
    public @NotNull Pat descent(@NotNull UnaryOperator<Pat> patOp, @NotNull UnaryOperator<Term> termOp) {
      return update(args.map(patOp));
    }

    @Override
    public void consumeBindings(@NotNull BiConsumer<LocalVar, Term> consumer) {
      args.forEach(arg -> arg.consumeBindings(consumer));
    }

    @Override
    public @NotNull Pat inline(@NotNull LocalCtx ctx) {
      return update(args.map(x -> x.inline(ctx)));
    }
  }

  /**
   * Meta for Hole
   *
   * @param solution the solution of this Meta.
   *                 Note that the solution (and its sub pattern) never contains a {@link Pat.Bind}.
   * @param fakeBind is used when inline if there is no solution.
   *                 So don't add this to {@link LocalCtx} too early
   *                 and remember to inline Meta in {@link ClauseTycker#checkLhs}
   */
  record Meta(
    @NotNull MutableValue<Pat> solution,
    // TODO: do we really need this?
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

    @Override
    public void consumeBindings(@NotNull BiConsumer<LocalVar, Term> consumer) {
      // TODO: reconsider this, I don't fully understand the comment
      // Do nothing
      // This is safe because storeBindings is called only in extractTele which is
      // only used for constructor ownerTele extraction for simpler indexed types
    }

    @Override
    public @NotNull Pat inline(@NotNull LocalCtx ctx) {
      return getOrElse(() -> {
        var name = new LocalVar(fakeBind, errorReport, GenerateKind.Anonymous.INSTANCE);
        ctx.put(name, type);
        return new Bind(name, type);
      });
    }

    public <R> @NotNull R map(@NotNull Function<Pat, R> mapper, @NotNull Supplier<R> aDefaults) {
      var solution = solution().get();
      if (solution == null) return aDefaults.get();
      return mapper.apply(solution);
    }

    public @NotNull Pat getOrElse(@NotNull Supplier<Pat> orElse) {
      var solution = solution().get();
      if (solution == null) return orElse.get();
      return solution;
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
      // throw new UnsupportedOperationException("TODO");
      var prettier = new CorePrettier(options);
      ;
      var doc = Doc.emptyIf(pats.isEmpty(), () -> Doc.cat(Doc.ONE_WS, Doc.commaList(
        pats.view().map(p -> prettier.pat(Arg.ofExplicitly(p), BasePrettier.Outer.Free)))));
      return expr == null ? doc : Doc.sep(doc, Doc.symbol("=>"), expr.data().toDoc(options));
    }

    public static @NotNull Preclause<Term> weaken(@NotNull Term.Matching clause) {
      return new Preclause<>(clause.sourcePos(), clause.patterns(), WithPos.dummy(clause.body()));
    }

    // public static @Nullable Term.Matching lift(@NotNull Preclause<Term> clause) {
    //   return clause.expr.map(term -> new Term.Matching(clause.sourcePos, clause.patterns, term));
    // }
  }
}
