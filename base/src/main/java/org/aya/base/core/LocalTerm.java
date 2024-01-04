package org.aya.base.core;

import org.aya.base.generic.LocalVar;
import org.jetbrains.annotations.NotNull;

public record LocalTerm(int index) implements Term {
  @Override public @NotNull Term bindAt(@NotNull LocalVar var, int depth) {
    return this;
  }

  @Override public @NotNull Term replace(int incoming, @NotNull Term arg) {
    if (index == incoming) return arg;
    return this;
  }
}
