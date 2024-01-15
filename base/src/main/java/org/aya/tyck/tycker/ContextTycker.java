// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.tycker;

import org.aya.syntax.ref.LocalCtx;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public sealed interface ContextTycker permits AbstractExprTycker {
  @NotNull LocalCtx localCtx();

  /**
   * Update {@code localCtx} with the given one
   *
   * @param ctx new {@link LocalCtx}
   * @return old context
   */
  @NotNull LocalCtx setLocalCtx(@NotNull LocalCtx ctx);

  default <R> R subscoped(@NotNull Supplier<R> action) {
    var parentCtx = setLocalCtx(localCtx().derive());
    var result = action.get();
    setLocalCtx(parentCtx);
    return result;
  }
}
