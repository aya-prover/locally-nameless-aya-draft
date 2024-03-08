// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import kala.function.IndexedFunction;
import org.jetbrains.annotations.NotNull;

public record ProjTerm(@NotNull Term of, int index) implements Term {
  public @NotNull ProjTerm update(@NotNull Term of, int index) {
    return this.of == of && this.index == index ? this : new ProjTerm(of, index);
  }

  @Override public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(f.apply(0, of), index);
  }

  public static @NotNull Term make(@NotNull Term of, int index) {
    return make(new ProjTerm(of, index));
  }

  public static @NotNull Term make(@NotNull ProjTerm material) {
    return switch (material.of) {
      case TupTerm(var elems) -> elems.get(material.index);
      default -> material;
    };
  }
}
