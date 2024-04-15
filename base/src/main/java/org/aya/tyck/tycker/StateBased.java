// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.tycker;

import org.aya.normalize.Normalizer;
import org.aya.syntax.core.term.Term;
import org.aya.tyck.TyckState;
import org.jetbrains.annotations.NotNull;

/**
 * Indicating something is {@link TyckState}ful,
 * therefore we can perform weak-head normalizing.
 *
 * @author ice1000
 * @see #whnf(Term)
 * @see ContextBased
 */
public interface StateBased {
  @NotNull TyckState state();
  default @NotNull Term whnf(@NotNull Term term) {
    return new Normalizer(state()).whnf(term);
  }
}
