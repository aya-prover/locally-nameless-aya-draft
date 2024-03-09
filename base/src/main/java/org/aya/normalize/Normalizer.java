// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.normalize;

import kala.collection.immutable.ImmutableSet;
import org.aya.syntax.core.term.AppTerm;
import org.aya.syntax.core.term.ProjTerm;
import org.aya.syntax.core.term.StableWHNF;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.AnyVar;
import org.aya.tyck.TyckState;
import org.jetbrains.annotations.NotNull;

/**
 * Unlike in pre-v0.30 Aya, we use only one normalizer, only doing head reduction,
 * and we merge conservative normalizer and the whnf normalizer.
 */
public record Normalizer(@NotNull TyckState state, @NotNull ImmutableSet<AnyVar> opaque) {
  public Normalizer(@NotNull TyckState state) {
    this(state, ImmutableSet.empty());
  }

  public @NotNull Term whnf(@NotNull Term term) {
    return switch (term) {
      case StableWHNF whnf -> whnf;
      case AppTerm app -> AppTerm.make(app);
      case ProjTerm proj -> ProjTerm.make(proj);
      // TODO: handle other cases
      case Term _ -> term;
    };
  }
}
