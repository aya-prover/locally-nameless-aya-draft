// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.xtt;

import kala.collection.immutable.ImmutableSeq;
import kala.function.IndexedFunction;
import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;

public record PartialTerm(
  @NotNull Term term,
  @NotNull ImmutableSeq<ParClause> clauses
) implements Term {
  public record ParClause(@NotNull DimTerm dim, @NotNull Term term) {
    public @NotNull ParClause descent(@NotNull IndexedFunction<Term, Term> f) {
      return new ParClause(dim, term.descent(f));
    }
  }

  @Override public @NotNull PartialTerm descent(@NotNull IndexedFunction<Term, Term> f) {
    return new PartialTerm(term.descent(f), clauses.map(c -> c.descent(f)));
  }
}
