// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.tycker;

import org.aya.syntax.core.term.*;
import org.aya.tyck.TyckState;
import org.jetbrains.annotations.NotNull;

/**
 * TODO: update document
 * This is the second base-base class of a tycker.
 * It has the zonking stuffs and basic def-call related functions.
 * Apart from that, it also deals with core term references in concrete terms.
 *
 * @author ice1000
 * @see #whnf(Term)
 */
public interface StateBased {
  @NotNull TyckState state();

  @NotNull Term whnf(@NotNull Term term);
}
