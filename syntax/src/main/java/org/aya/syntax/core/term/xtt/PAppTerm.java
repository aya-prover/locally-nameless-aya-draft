// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.xtt;

import kala.function.IndexedFunction;
import org.aya.syntax.core.term.LamTerm;
import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;

public record PAppTerm(@NotNull Term fun, @NotNull Term arg, @NotNull Term a, @NotNull Term b) implements Term {
  public @NotNull PAppTerm update(@NotNull Term fun, @NotNull Term arg, @NotNull Term a, @NotNull Term b) {
    if (this.fun == fun && this.arg == arg && this.a == a && this.b == b) return this;
    return new PAppTerm(fun, arg, a, b);
  }

  @Override public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(fun.descent(f), arg.descent(f), a.descent(f), b.descent(f));
  }

  public @NotNull Term make() {
    if (fun instanceof LamTerm(var body)) return body.instantiate(arg);
    if (arg instanceof DimTerm dim) return switch (dim) {
      case I0 -> a;
      case I1 -> b;
    };
    return this;
  }
}
