package org.aya.base.core;

import org.aya.base.generic.LocalVar;
import org.jetbrains.annotations.NotNull;

public record AppTerm(@NotNull Term fun, @NotNull Term arg) implements Term {
  @Override public @NotNull Term bindAt(@NotNull LocalVar var, int depth) {
    return new AppTerm(fun.bindAt(var, depth), arg.bindAt(var, depth));
  }
}
