package org.aya.syntax.core.term;

import kala.collection.SeqLike;
import kala.function.BooleanConsumer;
import kala.function.IndexedFunction;
import org.jetbrains.annotations.NotNull;

public record LamTerm(boolean explicit, Term body) implements StableWHNF {
  public @NotNull LamTerm update(@NotNull Term body) {
    return body == this.body
      ? this
      : new LamTerm(explicit, body);
  }

  @Override
  public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(f.apply(1, body));
  }

  public static @NotNull Term unwrap(@NotNull Term term, @NotNull BooleanConsumer params) {
    while (term instanceof LamTerm lambda) {
      params.accept(lambda.explicit);
      term = lambda.body;
    }
    return term;
  }

  public static @NotNull Term make(@NotNull SeqLike<Boolean> telescope, @NotNull Term body) {
    return telescope.view().foldRight(body, LamTerm::new);
  }
}
