package org.aya.syntax.core.term;

import kala.function.IndexedFunction;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.Arg;
import org.jetbrains.annotations.NotNull;

public record AppTerm(@NotNull Term fun, @NotNull Term arg) implements Term {
  public @NotNull AppTerm update(@NotNull Term fun, @NotNull Term arg) {
    return fun == this.fun && arg == this.arg
      ? this
      : new AppTerm(fun, arg);
  }

  @Override
  public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(f.apply(0, fun), f.apply(0, arg));
  }
}
