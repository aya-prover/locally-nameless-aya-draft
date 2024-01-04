package org.aya.base.core;

import org.aya.base.generic.LocalVar;
import org.jetbrains.annotations.NotNull;

public record FreeTerm(@NotNull LocalVar name) implements Term {
  @Override public @NotNull Term bindAt(@NotNull LocalVar var, int depth) {
    if (name == var) return new LocalTerm(depth);
    return this;
  }
}
