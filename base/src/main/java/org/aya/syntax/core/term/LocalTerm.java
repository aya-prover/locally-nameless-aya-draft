package org.aya.syntax.core.term;

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

  @Override public @NotNull Term replace(int incoming, @NotNull Term arg) {
    if (index == incoming) return arg;
    return this;
  }
}
