// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

import kala.collection.immutable.ImmutableSeq;
import kala.function.IndexedFunction;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.marker.CallLike;
import org.jetbrains.annotations.NotNull;

public record JitFnCall(
  @Override @NotNull JitFn instance,
  @Override int ulift,
  @Override @NotNull ImmutableSeq<Term> args
) implements JitCallable, CallLike.FnCallLike {
  public @NotNull JitFnCall update(@NotNull ImmutableSeq<Term> args) {
    return this.args.sameElements(args, true) ? this : new JitFnCall(instance, ulift, args);
  }

  @Override public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(args.map(arg -> f.apply(0, arg)));
  }
}
