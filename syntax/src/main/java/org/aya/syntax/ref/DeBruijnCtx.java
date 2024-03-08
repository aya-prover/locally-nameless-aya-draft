// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.ref;

import kala.collection.mutable.MutableList;
import org.aya.syntax.core.term.Term;
import org.aya.util.error.InternalException;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * It is NOT DeBrujin
 *
 * @param ctx reversed ctx, the last one is the type of 0.
 */
public record DeBruijnCtx(@NotNull MutableList<Term> ctx) {
  /**
   * Maps {@code 0} to {@code type}
   */
  public void push(@NotNull Term type) {
    ctx.append(type);
  }

  public @NotNull Term get(int index) {
    if (index < 0) throw new InternalException("index < 0");
    if (index >= ctx.size()) throw new InternalException("index >= ctx.size()");
    var realIndex = ctx.size() - 1 - index;
    return ctx.get(realIndex);
  }

  public <R> @NotNull R with(@NotNull Term type, @NotNull Supplier<R> subscope) {
    push(type);
    var ret = subscope.get();
    pop();
    return ret;
  }

  public @NotNull Term pop() {
    if (ctx.isEmpty()) throw new InternalException("empty ctx");
    return ctx.removeLast();
  }
}
