// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

import kala.collection.immutable.ImmutableSeq;
import kala.function.IndexedFunction;
import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;

public record JitConCall(
  @Override @NotNull JitCon instance,
  @Override int ulift,
  @NotNull ImmutableSeq<Term> ownerArgs,
  @NotNull ImmutableSeq<Term> conArgs
) implements JitCallable {
  public JitConCall update(@NotNull ImmutableSeq<Term> ownerArgs, @NotNull ImmutableSeq<Term> conArgs) {
    return this.ownerArgs.sameElements(ownerArgs, true)
      && this.conArgs.sameElements(conArgs, true)
      ? this : new JitConCall(instance, ulift, ownerArgs, conArgs);
  }
  @Override public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(
      ownerArgs.map(t -> f.apply(0, t)),
      conArgs.map(t -> f.apply(0, t))
    );
  }

  @Override
  public @NotNull ImmutableSeq<@NotNull Term> args() {
    return ownerArgs.appendedAll(conArgs);
  }
}
