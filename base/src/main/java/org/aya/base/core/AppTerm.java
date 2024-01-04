package org.aya.base.core;

import org.aya.base.generic.LocalVar;
import org.jetbrains.annotations.NotNull;

public record AppTerm(@NotNull Term fun, @NotNull Term arg) implements Term {
  @Override public @NotNull Term bindAt(@NotNull LocalVar var, int depth) {
    var newFun = fun.bindAt(var, depth);
    var newArg = arg.bindAt(var, depth);
    if (newFun == fun && newArg == arg) return this;
    return new AppTerm(newFun, newArg);
  }

  @Override public @NotNull Term replace(int index, @NotNull Term incoming) {
    var newArg = arg.replace(index, incoming);
    var newFun = fun.replace(index, incoming);
    if (newFun instanceof LamTerm(var body)) return body.instantiate(incoming);
    if (newFun == fun && newArg == arg) return this;
    return new AppTerm(newFun, newArg);
  }
}
