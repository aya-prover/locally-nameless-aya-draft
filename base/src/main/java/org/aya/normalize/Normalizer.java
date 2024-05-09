// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.normalize;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.immutable.ImmutableSet;
import kala.control.Either;
import kala.control.Option;
import org.aya.syntax.core.pat.PatToTerm;
import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.FnCall;
import org.aya.syntax.core.term.call.MetaCall;
import org.aya.syntax.core.term.call.PrimCall;
import org.aya.syntax.core.term.xtt.PAppTerm;
import org.aya.syntax.ref.AnyVar;
import org.aya.tyck.TyckState;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

/**
 * Unlike in pre-v0.30 Aya, we use only one normalizer, only doing head reduction,
 * and we merge conservative normalizer and the whnf normalizer.
 */
public record Normalizer(@NotNull TyckState state, @NotNull ImmutableSet<AnyVar> opaque)
  implements UnaryOperator<Term> {
  public Normalizer(@NotNull TyckState state) {
    this(state, ImmutableSet.empty());
  }

  @Override public Term apply(Term term) {
    return whnf(term);
  }

  public @NotNull Term whnf(@NotNull Term term) {
    if (term instanceof StableWHNF) return term;
    var postTerm = term.descent(this);

    return switch (postTerm) {
      case StableWHNF whnf -> Panic.unreachable();
      case AppTerm app -> {
        var result = app.make();
        yield result == app ? result : whnf(result);
      }
      case PAppTerm app -> {
        var result = app.make();
        yield result == app ? result : whnf(result);
      }
      case ProjTerm proj -> {
        var result = proj.make();
        yield result == proj ? result : whnf(result);
      }
      case FnCall(var ref, int ulift, var args) when ref.core != null -> switch (ref.core.body) {
        case Either.Left(var body) -> body.instantiateTele(args.view());
        case Either.Right(var clauses) -> throw new UnsupportedOperationException("TODO");
      };
      case PrimCall prim -> state.factory().unfold(prim, state);
      case MetaPatTerm(var meta) -> {
        var solution = meta.solution().get();
        if (solution == null) yield term;
        yield whnf(PatToTerm.visit(solution));
      }
      case MetaCall(var ref, var args) -> {
        var solution = state.solutions().getOrNull(ref);
        if (solution == null) yield term;
        yield whnf(MetaCall.app(ref, solution, args));
      }
      // TODO: handle other cases
      default -> term;
    };
  }

  public @NotNull Option<Term> tryUnfoldClauses(
    @NotNull ImmutableSeq<Term.Matching> clauses, @NotNull ImmutableSeq<Term> args,
    int ulift, boolean orderIndependent
  ) {
    for (var matchy : clauses) {
      var matcher = new PatternMatcher(false, this);
      var subst = matcher.matchMany(matchy.patterns(), args);
      if (subst.isOk()) {
        throw new UnsupportedOperationException("TODO");
        // return Option.some(matchy.body().rename().lift(ulift).subst(subst.get()));
      } else if (!orderIndependent && subst.getErr()) return Option.none();
    }
    return Option.none();
  }
}
