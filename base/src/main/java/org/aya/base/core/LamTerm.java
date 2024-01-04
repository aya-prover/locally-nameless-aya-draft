package org.aya.base.core;

import org.aya.base.generic.LocalVar;
import org.jetbrains.annotations.NotNull;

public record LamTerm(Term body) implements Term {
  @Override public @NotNull Term bindAt(@NotNull LocalVar var, int depth) {
    return new LamTerm(body.bindAt(var, depth + 1));
  }
}
