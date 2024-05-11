// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import kala.function.IndexedFunction;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

public record LetTerm(@NotNull Bind bind, @NotNull Term body) implements Term {
  public record Bind(@NotNull Param bind, @NotNull Term definedAs) {
    public @NotNull Bind update(@NotNull Param bind, @NotNull Term definedAs) {
      return bind == this.bind && definedAs == this.definedAs ? this : new Bind(bind, definedAs);
    }

    public @NotNull Bind descent(@NotNull UnaryOperator<Term> f) {
      return update(bind.descent(f), f.apply(definedAs));
    }
  }

  public @NotNull LetTerm update(@NotNull Bind bind, @NotNull Term body) {
    return bind == this.bind && body == this.body ? this : new LetTerm(bind, body);
  }

  @Override
  public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(bind.descent(t -> f.apply(0, t)), f.apply(1, body));
  }
}
