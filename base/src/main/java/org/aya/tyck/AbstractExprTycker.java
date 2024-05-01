// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import org.aya.syntax.concrete.Expr;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.LocalCtx;
import org.aya.tyck.error.UnifyError;
import org.aya.tyck.error.UnifyInfo;
import org.aya.tyck.tycker.AbstractTycker;
import org.aya.tyck.unify.TermComparator;
import org.aya.util.Ordering;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.aya.util.reporter.Problem;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public sealed abstract class AbstractExprTycker extends AbstractTycker permits ExprTycker {
  protected AbstractExprTycker(@NotNull TyckState state, @NotNull LocalCtx ctx, @NotNull Reporter reporter) {
    super(state, ctx, reporter);
  }

  protected abstract @NotNull TermComparator unifier(@NotNull SourcePos pos, @NotNull Ordering order);

  /**
   * Check whether {@param lower} is a subtype of {@param upper}
   *
   * @return failure data, null if success
   */
  protected final @Nullable TermComparator.FailureData unifyTy(
    @NotNull Term upper,
    @NotNull Term lower,
    @NotNull SourcePos pos
  ) {
    var unifier = unifier(pos, Ordering.Lt);
    var result = unifier.compare(lower, upper, null);
    if (!result) return unifier.getFailure();
    return null;
  }

  /**
   * @param pc a problem constructor
   * @see AbstractExprTycker#unifyTy(Term, Term, SourcePos)
   */
  protected final boolean unifyTyReported(
    @NotNull Term upper,
    @NotNull Term lower,
    @NotNull SourcePos pos,
    @NotNull Function<UnifyInfo.Comparison, Problem> pc
  ) {
    var result = unifyTy(upper, lower, pos);
    if (result != null) {
      // TODO: Ice Spell 「 Perfect Freeze 」 on [lower] and [upper]
      reporter.report(pc.apply(new UnifyInfo.Comparison(lower, upper, result)));
    }

    return result == null;
  }

  protected final boolean unifyTyReported(
    @NotNull Term upper,
    @NotNull Term lower,
    @NotNull WithPos<Expr> expr
  ) {
    return unifyTyReported(upper, lower, expr.sourcePos(),
      cp -> new UnifyError.Type(expr.data(), expr.sourcePos(), cp, new UnifyInfo(state)));
  }
}
