// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.tycker;

import org.aya.syntax.core.term.FreeTerm;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.MetaCall;
import org.aya.syntax.ref.LocalCtx;
import org.aya.syntax.ref.MetaVar;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Indicating something is {@link LocalCtx}ful
 */
public interface ContextBased {
  @NotNull LocalCtx localCtx();

  /**
   * Update {@code localCtx} with the given one
   *
   * @param ctx new {@link LocalCtx}
   * @return old context
   */
  @ApiStatus.Internal
  @Contract(mutates = "this")
  @NotNull LocalCtx setLocalCtx(@NotNull LocalCtx ctx);

  @Contract(mutates = "this")
  default <R> R subscoped(@NotNull Supplier<R> action) {
    var parentCtx = setLocalCtx(localCtx().derive());
    var result = action.get();
    setLocalCtx(parentCtx);
    return result;
  }

  default @NotNull Term mockTerm(@NotNull Param param, @NotNull SourcePos pos) {
    return freshMeta(param.name(), pos, new MetaVar.OfType(param.type()));
  }

  default @NotNull MetaCall freshMeta(String name, @NotNull SourcePos pos, MetaVar.Requirement req) {
    var args = localCtx().extract().<Term>map(FreeTerm::new).toImmutableSeq();
    return new MetaCall(new MetaVar(name, pos, args.size(), req), args);
  }
}
