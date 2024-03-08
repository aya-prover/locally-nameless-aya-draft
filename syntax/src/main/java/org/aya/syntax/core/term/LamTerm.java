// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import kala.collection.SeqLike;
import kala.function.BooleanConsumer;
import kala.function.IndexedFunction;
import org.jetbrains.annotations.NotNull;

public record LamTerm(Term body) implements StableWHNF {
  public @NotNull LamTerm update(@NotNull Term body) {
    return body == this.body
      ? this
      : new LamTerm(body);
  }

  @Override
  public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(f.apply(1, body));
  }

  public static @NotNull Term make(int paramSize, @NotNull Term body) {
    var result = body;

    for (var i = 0; i < paramSize; ++i) {
      result = new LamTerm(result);
    }

    return result;
  }
}
