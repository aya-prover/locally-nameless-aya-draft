package org.aya.base.core;

import org.aya.base.generic.LocalVar;
import org.jetbrains.annotations.NotNull;

public record LamTerm(Term body) implements Term {
  @Override public @NotNull Term bindAt(@NotNull LocalVar var, int depth) {
    var newBody = body.bindAt(var, depth + 1);
    if (newBody == body) return this;
    return new LamTerm(newBody);
  }
}
