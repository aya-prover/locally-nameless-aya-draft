// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.tycker;

import org.aya.tyck.ctx.LocalSubstitution;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public interface LocalDeful {
  @NotNull LocalSubstitution localDefinition();

  @ApiStatus.Internal
  @Contract(mutates = "this")
  @NotNull LocalSubstitution setLocalDefinition(@NotNull LocalSubstitution newOne);

  @Contract(mutates = "this")
  default <R> R subscoped(@NotNull Supplier<R> action) {
    var parentCtx = setLocalDefinition(localDefinition().derive());
    var result = action.get();
    setLocalDefinition(parentCtx);
    return result;
  }
}
