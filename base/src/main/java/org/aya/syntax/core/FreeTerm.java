package org.aya.syntax.core;

import org.aya.syntax.ref.LocalVar;
import org.jetbrains.annotations.NotNull;

public record FreeTerm(@NotNull LocalVar name) implements Term {
  @Override public @NotNull Term bindAt(@NotNull LocalVar var, int depth) {
    if (name == var) return new LocalTerm(depth);
    return this;
  }

  @Override public @NotNull Term replace(int index, @NotNull Term arg) {
    return this;
  }
}
