// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import kala.function.IndexedFunction;
import org.jetbrains.annotations.NotNull;

public record AppTerm(@NotNull Term fun, @NotNull Term arg) implements Term {
  public @NotNull AppTerm update(@NotNull Term fun, @NotNull Term arg) {
    return fun == this.fun && arg == this.arg ? this : new AppTerm(fun, arg);
  }

  @Override
  public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(f.apply(0, fun), f.apply(0, arg));
  }

  public static @NotNull Term make(@NotNull Term f, @NotNull Term a) {
    return make(new AppTerm(f, a));
  }

  public static @NotNull Term make(@NotNull AppTerm material) {
    return switch (material.fun) {
      case LamTerm(var body) -> body.instantiate(material.arg);
      default -> material;
    };
  }
}