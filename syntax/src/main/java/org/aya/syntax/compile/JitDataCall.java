// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

import kala.function.IndexedFunction;
import org.aya.syntax.core.term.Term;
import org.aya.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

public record JitDataCall(
  @Override @NotNull JitData instance,
  @Override int ulift,
  @Override @NotNull Term... arguments
) implements JitCallable {
  public @NotNull JitDataCall update(Term[] dataArgs) {
    return ArrayUtil.identical(this.arguments, dataArgs) ? this : new JitDataCall(instance, ulift, dataArgs);
  }

  @Override public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(ArrayUtil.map(arguments, x -> f.apply(0, x)));
  }
}
