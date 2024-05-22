// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

import kala.function.IndexedFunction;
import org.aya.syntax.core.term.Term;
import org.aya.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

public record JitConCall(
  @Override @NotNull JitCon instance,
  @Override int ulift,
  @NotNull Term[] ownerArgs,
  @NotNull Term[] conArgs
) implements JitCallable {
  public JitConCall update(@NotNull Term[] ownerArgs, @NotNull Term[] conArgs) {
    return ArrayUtil.identical(ownerArgs, ownerArgs()) && ArrayUtil.identical(conArgs, conArgs())
      ? this : new JitConCall(instance, ulift, ownerArgs, conArgs);
  }
  @Override public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(
      ArrayUtil.map(ownerArgs, t -> f.apply(0, t)),
      ArrayUtil.map(conArgs, t -> f.apply(0, t))
    );
  }
  @Override public Term[] arguments() {
    var ret = new Term[ownerArgs.length + conArgs.length];
    System.arraycopy(ownerArgs, 0, ret, 0, ownerArgs.length);
    System.arraycopy(conArgs, 0, ret, ownerArgs.length, conArgs.length);
    return ret;
  }
}
