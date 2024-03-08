// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.normalize;

import org.aya.syntax.core.term.Term;
import org.aya.tyck.TyckState;
import org.jetbrains.annotations.NotNull;

public record Normalizer(@NotNull TyckState state) {
  public @NotNull Term whnf(@NotNull Term term) {
    // TODO
    return term;
  }
}
