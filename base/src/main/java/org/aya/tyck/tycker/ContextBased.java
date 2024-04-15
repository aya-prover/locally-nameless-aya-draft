// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.tycker;

import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.DeBruijnCtx;
import org.aya.syntax.ref.LocalCtx;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Indicating something is {@link LocalCtx}ful and {@link DeBruijnCtx}ful
 */
public interface ContextBased {
  @NotNull LocalCtx localCtx();
  @NotNull DeBruijnCtx deBruijnCtx();

  /**
   * Update {@code localCtx} with the given one
   *
   * @param ctx new {@link LocalCtx}
   * @return old context
   */
  @ApiStatus.Internal
  @Contract(mutates = "this")
  @NotNull LocalCtx setLocalCtx(@NotNull LocalCtx ctx);

  @ApiStatus.Internal
  @Contract(mutates = "this")
  @NotNull DeBruijnCtx setDeBruijnCtx(@NotNull DeBruijnCtx ctx);

  @Contract(mutates = "this")
  default <R> R subscoped(@NotNull Supplier<R> action) {
    var parentCtx = setLocalCtx(localCtx().derive());
    var parentDCtx = setDeBruijnCtx(deBruijnCtx().derive());
    var result = action.get();
    setLocalCtx(parentCtx);
    setDeBruijnCtx(parentDCtx);
    return result;
  }

  default @NotNull Term mockTerm(@NotNull Param param, @NotNull SourcePos pos) {
    throw new UnsupportedOperationException("TODO");
  }
}
