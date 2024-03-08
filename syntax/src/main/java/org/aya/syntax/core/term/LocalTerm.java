// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import kala.collection.immutable.ImmutableSeq;
import kala.function.IndexedFunction;
import org.aya.syntax.ref.LocalVar;
import org.jetbrains.annotations.NotNull;

public record LocalTerm(int index) implements Term {
  @Override
  public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return this;
  }

  @Override public @NotNull Term bindAt(@NotNull LocalVar var, int depth) {
    return this;
  }

  @Override public @NotNull Term replaceAllFrom(int from, @NotNull ImmutableSeq<Term> list) {
    if (index - from < list.size()) return list.get(index - from);
    return this;
  }
}
