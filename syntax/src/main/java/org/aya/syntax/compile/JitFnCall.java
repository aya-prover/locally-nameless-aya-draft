// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

import kala.function.IndexedFunction;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.marker.GenericCall;
import org.aya.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

public record JitFnCall(
  @Override @NotNull JitFn instance,
  @Override int ulift,
  @Override @NotNull Term... arguments
) implements JitCallable, GenericCall.GenericFnCall {
  public @NotNull JitFnCall update(@NotNull Term[] args) {
    return ArrayUtil.identical(this.arguments, args) ? this : new JitFnCall(instance, ulift, args);
  }

  @Override public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(ArrayUtil.map(arguments, arg -> f.apply(0, arg)));
  }
}
