// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.unify;

import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.ConCallLike;
import org.aya.tyck.tycker.StateBased;
import org.aya.util.Pair;
import org.aya.util.error.InternalException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TermComparator implements StateBased {
  public boolean compare(@NotNull Term lhs, @NotNull Term rhs, @Nullable Term type) {
    // TODO
    if (type == null) return compareUntyped(lhs, rhs) != null;
    return doCompareTyped(lhs, rhs, type);
  }

  /**
   * Compare {@param lhs} and {@param rhs} with {@param type} information
   *
   * @param type the whnf type.
   * @return whether they are 'the same' and their types are {@param type}
   */
  private boolean doCompareTyped(@NotNull Term lhs, @NotNull Term rhs, @NotNull Term type) {
    boolean ret = switch (type) {
      case LamTerm _ -> throw new InternalException("LamTerm is never type");
      case ConCallLike _ -> throw new InternalException("ConCall is never type");
      case TupTerm _ -> throw new InternalException("TupTerm is never type");
      case ErrorTerm _ -> true;
      case PiTerm pi -> switch (new Pair<>(lhs, rhs)) {
        case Pair(LamTerm(var lbody), LamTerm(var rbody)) -> state().dCtx().with(pi.param(),
          () -> compare(lbody, rbody, pi.body()));
        case Pair(LamTerm lambda, _) -> compareLambda(lambda, rhs, pi);
        case Pair(_, LamTerm rambda) -> compareLambda(rambda, lhs, pi);
        default -> false;
      };
      case SigmaTerm(var paramSeq) -> {
        // We use view since we need to instantiate the remaining params after tyck some component.
        var params = paramSeq.view();
        var size = paramSeq.size();
        for (var i = 0; i < size; ++ i) {
          var l = ProjTerm.make(lhs, i);
          var r = ProjTerm.make(rhs, i);
          var param = params.getFirst();
          if (! compare(l, r, param)) yield false;
          params = params.drop(1).mapIndexed((j, term) ->
            term.replace(j, l));
        }
        yield true;
      }
      default -> false;
    };

    // TODO
    throw new UnsupportedOperationException("TODO");
  }

  /**
   * Compare {@param lhs} and {@param rhs} without type information.
   *
   * @return the type of {@param lhs} and {@param rhs} if they are 'the same', null otherwise.
   */
  private @Nullable Term compareUntyped(@NotNull Term lhs, @NotNull Term rhs) {
    // TODO
    return doCompareUntyped(lhs, rhs);
  }

  private @Nullable Term doCompareUntyped(@NotNull Term lhs, @NotNull Term rhs) {
    return switch (lhs) {
      case AppTerm(var f, var a) -> {
        if (!(rhs instanceof AppTerm(var g, var b))) yield null;
        var fTy = compareUntyped(f, g);
        if (fTy == null) yield null;
        if (!(whnf(fTy) instanceof PiTerm pi)) yield null;
        if (!compare(a, b, pi.param())) yield null;
        yield pi.body().instantiate(a);
      }
      case FreeTerm(var lvar) -> {
        if (rhs instanceof FreeTerm(var rvar) && lvar == rvar) yield state().ctx().get(lvar);
        yield null;
      }
      case LocalTerm(var ldx) -> {
        if (rhs instanceof LocalTerm(var rdx) && ldx == rdx) yield state().dCtx().get(ldx);
        yield null;
      }
      default -> throw new UnsupportedOperationException("TODO");
    };
  }

  /**
   * Compare {@param lambda} and {@param rhs} with {@param type}
   */
  private boolean compareLambda(@NotNull LamTerm lambda, @NotNull Term rhs, @NotNull PiTerm type) {
    return state().dCtx().with(type.param(), () -> {
      // 0 : type.param()
      var lhsBody = lambda.body();
      var rhsBody = AppTerm.make(rhs, new LocalTerm(0));
      return compare(lhsBody, rhsBody, type.body());
    });
  }
}
