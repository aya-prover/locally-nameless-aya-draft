// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.tycker;

import org.aya.normalize.Finalizer;
import org.aya.normalize.Normalizer;
import org.aya.tyck.Result;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.MetaVar;
import org.aya.tyck.TyckState;
import org.jetbrains.annotations.NotNull;

/**
 * Indicating something is {@link TyckState}ful,
 * therefore we can perform weak-head normalizing.
 *
 * @author ice1000
 * @see #whnf(Term)
 * @see Contextful
 */
public interface Stateful {
  @NotNull TyckState state();
  default @NotNull Term whnf(@NotNull Term term) {
    return new Normalizer(state()).apply(term);
  }
  /**
   * Does not validate solution.
   */
  default void solve(MetaVar meta, Term solution) {
    state().solve(meta, solution);
  }
  default @NotNull Term freezeHoles(@NotNull Term term) {
    return new Finalizer.Freeze(this).zonk(term);
  }
  default @NotNull Result freezeHoles(@NotNull Result r) {
    return switch (r) {
      case Result.Default(var term, var type) -> new Result.Default(freezeHoles(term), freezeHoles(type));
      case Result.Sort sort -> sort;
    };
  }
}
