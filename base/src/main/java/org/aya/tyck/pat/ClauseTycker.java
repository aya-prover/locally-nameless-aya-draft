// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.pat;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableMap;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import org.aya.prettier.AyaPrettierOptions;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.concrete.Pattern;
import org.aya.syntax.core.def.Signature;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.core.pat.PatToTerm;
import org.aya.syntax.core.term.MetaPatTerm;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.LocalCtx;
import org.aya.syntax.ref.LocalVar;
import org.aya.tyck.ExprTycker;
import org.aya.tyck.tycker.Problematic;
import org.aya.util.error.Panic;
import org.aya.util.error.WithPos;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

public record ClauseTycker(@NotNull ExprTycker exprTycker) implements Problematic {
  public record LhsResult(
    @NotNull LocalCtx localCtx,
    @NotNull Term type,
    @NotNull ImmutableSeq<Term> paramSubst,
    @NotNull ImmutableMap<LocalVar, Term> asSubst,
    @NotNull Pat.Preclause<Expr> clause,
    boolean hasError
  ) {

  }

  @Override
  public @NotNull Reporter reporter() {
    return exprTycker.reporter;
  }

  private @NotNull PatternTycker newPatternTycker(@NotNull SeqView<Param> telescope, @NotNull Term result) {
    return new PatternTycker(exprTycker, reporter(), telescope, result, MutableMap.create());
  }

  private @NotNull LhsResult checkLhs(
    @NotNull Signature<? extends Term> signature,
    @NotNull Pattern.Clause clause
  ) {
    var tycker = newPatternTycker(signature.param().view().map(WithPos::data), signature.result());
    return exprTycker.subscoped(() -> {
      // TODO: need some prework, see old project

      var patResult = tycker.tyck(clause.patterns.view(), null, clause.expr.getOrNull());
      var ctx = exprTycker.localCtx();   // No need to copy the context here

      clause.hasError |= patResult.hasError();
      patResult = inline(patResult, ctx);

      // TODO: inline types in localCtx

      clause.patterns.view().map(it -> it.term().data()).forEach(TermInPatInline::apply);
      // var patTele = patResult.wellTyped()
      // var mPatResult = patResult;
      //
      // UnaryOperator<Term> bodyPre = t -> {
      //   // [t] here has [LocalTerm]s that refer to the telescope
      //   // instantiate params: now we eliminate them
      //   t = t.instantiateTele(mPatResult.paramSubst().view());
      //   // instantiate as
      //   t = mPatResult.ass().view().zip(mPatResult.asSubst()).foldLeft(t, (acc, pair) ->
      //     acc.bind(pair.component1()).instantiate(pair.component2()));
      //
      //   // bind reference to [Pat.Bind] as [LocalTerm]
      //
      // };

      return new LhsResult(
        ctx,
        patResult.result(),
        patResult.paramSubst(),
        patResult.asSubst(),
        new Pat.Preclause<>(
          clause.sourcePos,
          patResult.wellTyped(),
          patResult.newBody() == null ? null : patResult.newBody().data()),
        patResult.hasError()
      );
    });
  }

  /**
   * <pre>
   *   def foo (a : Nat) (b : Nat) : Nat =>
   *   | 0, c => let a => 0, b => c in b
   *   | ...
   * </pre>
   */
  private @NotNull Pat.Preclause<Term> checkRhs(@NotNull LhsResult result) {
    return exprTycker.subscoped(() -> {
      throw new UnsupportedOperationException("TODO");
    });
  }

  private static final class TermInline {
    public static @NotNull Term apply(@NotNull Term term) {
      if (term instanceof MetaPatTerm metaPatTerm) {
        var solution = metaPatTerm.meta().solution().get();
        // TODO: What should we do if we are unable to inline a MetaPatTerm?
        if (solution == null) throw new Panic(STR."Unable to inline \{metaPatTerm.toDoc(AyaPrettierOptions.debug())}");
        // the solution may contains other MetaPatTerm
        return apply(PatToTerm.visit(solution));
      } else {
        return term.descent(TermInline::apply);
      }
    }
  }

  /**
   * Inline terms which in pattern
   */
  private static final class TermInPatInline {
    public static void apply(@NotNull Pattern pat) {
      var typeRef = switch (pat) {
        case Pattern.Bind bind -> bind.type();
        case Pattern.As as -> as.type();
        default -> null;
      };

      if (typeRef != null) typeRef.update(it -> it == null ? null : inlineTerm(it));

      pat.descent((_, p) -> {
        apply(p);
        return p;
      });
    }
  }

  private static @NotNull Term inlineTerm(@NotNull Term term) {
    return TermInline.apply(term);
  }

  /**
   * Inline terms in {@param result}, please do this after inline all patterns
   */
  private static @NotNull PatternTycker.TyckResult inline(@NotNull PatternTycker.TyckResult result, @NotNull LocalCtx ctx) {
    // inline {Pat.Meta} before inline {MetaPatTerm}s
    var wellTyped = result.wellTyped().map(x -> x.inline(ctx));
    // so that {MetaPatTerm}s can be inlined safely
    var paramSubst = result.paramSubst().map(ClauseTycker::inlineTerm);
    var asSubst = ImmutableMap.from(result.asSubst().view().mapValues((_, t) -> inlineTerm(t)));
    var resultTerm = inlineTerm(result.result());

    return new PatternTycker.TyckResult(wellTyped, paramSubst, resultTerm, asSubst, result.newBody(), result.hasError());
  }
}
