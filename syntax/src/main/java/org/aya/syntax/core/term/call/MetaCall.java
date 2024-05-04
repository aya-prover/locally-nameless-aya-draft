// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.call;

import kala.collection.immutable.ImmutableSeq;
import kala.function.IndexedFunction;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.MetaVar;
import org.jetbrains.annotations.NotNull;

public record MetaCall(
  @Override @NotNull MetaVar ref,
  @Override @NotNull ImmutableSeq<Term> args
) implements Callable {
  public @NotNull Term update(@NotNull ImmutableSeq<Term> args) {
    return new MetaCall(ref, args);
  }

  @Override public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(Callable.descent(args, f));
  }
}
