package org.aya.syntax.core.term;

import kala.function.IndexedFunction;
import org.aya.syntax.ref.LocalVar;
import org.jetbrains.annotations.NotNull;

public record LamTerm(Term body) implements StableWHNF {
  public @NotNull LamTerm update(@NotNull Term body) {
    return body == this.body
      ? this
      : new LamTerm(body);
  }

  @Override
  public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(f.apply(1, body));
  }
}
