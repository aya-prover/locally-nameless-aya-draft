// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import kala.function.IndexedFunction;
import kala.tuple.primitive.IntObjTuple2;
import org.jetbrains.annotations.NotNull;

public record LamTerm(Term body) implements StableWHNF {
  public @NotNull LamTerm update(@NotNull Term body) {
    return body == this.body ? this : new LamTerm(body);
  }

  @Override
  public @NotNull LamTerm descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(f.apply(1, body));
  }

  public static @NotNull Term make(int paramSize, @NotNull Term body) {
    var result = body;

    for (var i = 0; i < paramSize; ++i) {
      result = new LamTerm(result);
    }

    return result;
  }

  /**
   * Unwrap a {@link LamTerm} as much as possible
   *
   * @return a integer indicates how many bindings are introduced
   * and a most inner term that is not a {@link LamTerm}.
   */
  public static @NotNull IntObjTuple2<Term> unwrap(@NotNull LamTerm term) {
    int params = 0;
    Term it = term;

    while (it instanceof LamTerm lamTerm) {
      params = params + 1;
      it = lamTerm.body;
    }

    return IntObjTuple2.of(params, it);
  }
}
