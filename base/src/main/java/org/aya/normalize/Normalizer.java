// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.normalize;

import kala.collection.immutable.ImmutableSet;
import org.aya.syntax.core.term.AppTerm;
import org.aya.syntax.core.term.ProjTerm;
import org.aya.syntax.core.term.StableWHNF;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.FnCall;
import org.aya.syntax.ref.AnyVar;
import org.aya.tyck.TyckState;
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

  @Override
  public Term apply(Term term) {
    return whnf(term);
  }

  public @NotNull Term whnf(@NotNull Term term) {
    var postTerm = term.descent(this);

    return switch (postTerm) {
      case StableWHNF whnf -> whnf;
      case AppTerm app -> AppTerm.make(app);
      case ProjTerm proj -> ProjTerm.make(proj);
      case FnCall(var ref, int ulift, var args) when ref.core != null -> {
        throw new UnsupportedOperationException("TODO: implement");
      }
      // TODO: handle other cases
      default -> term;
    };
  }
}
