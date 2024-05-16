// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.tycker;

import org.aya.syntax.concrete.Expr;
import org.aya.syntax.core.term.Term;
import org.aya.tyck.error.UnifyError;
import org.aya.tyck.error.UnifyInfo;
import org.aya.unify.TermComparator;
import org.aya.util.Ordering;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.aya.util.reporter.Problem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface Unifiable extends Problematic, Stateful {
  @NotNull TermComparator unifier(@NotNull SourcePos pos, @NotNull Ordering order);

  /**
   * Check whether {@param lower} is a subtype of {@param upper} if {@param ordering} is {@link Ordering#Lt}.
   *
   * @param ordering by default should be {@link Ordering#Lt}, or {@link Ordering#Eq}
   * @return failure data, null if success
   */
  default @Nullable TermComparator.FailureData unifyTerm(
    @NotNull Term upper, @NotNull Term lower, @NotNull SourcePos pos, Ordering ordering
  ) {
    var unifier = unifier(pos, ordering);
    var result = unifier.compare(lower, upper, null);
    if (!result) return unifier.getFailure();
    return null;
  }

  /**
   * @param pc a problem constructor
   * @see Unifiable#unifyTerm
   */
  default boolean unifyTyReported(
    @NotNull Term upper, @NotNull Term lower, @NotNull SourcePos pos,
    @NotNull Function<UnifyInfo.Comparison, Problem> pc
  ) {
    var result = unifyTerm(upper, lower, pos, Ordering.Lt);
    if (result != null) fail(pc.apply(new UnifyInfo.Comparison(
      freezeHoles(lower), freezeHoles(upper), result)));
    return result == null;
  }

 default boolean unifyTermReported(
    @NotNull Term lhs, @NotNull Term rhs, @NotNull SourcePos pos,
    @NotNull Function<UnifyInfo.Comparison, Problem> pc
  ) {
    var result = unifyTerm(lhs, rhs, pos, Ordering.Eq);
    if (result != null) fail(pc.apply(new UnifyInfo.Comparison(
      freezeHoles(rhs), freezeHoles(lhs), result)));
    return result == null;
  }

  default boolean unifyTyReported(
    @NotNull Term upper, @NotNull Term lower, @NotNull WithPos<Expr> expr
  ) {
    return unifyTyReported(upper, lower, expr.sourcePos(),
      cp -> new UnifyError.Type(expr.data(), expr.sourcePos(), cp, new UnifyInfo(state())));
  }
}
