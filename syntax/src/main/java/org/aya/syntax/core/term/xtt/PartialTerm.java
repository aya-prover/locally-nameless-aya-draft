// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.xtt;

import kala.collection.immutable.ImmutableMap;
import kala.function.IndexedFunction;
import kala.tuple.Tuple;
import org.aya.syntax.core.term.StableWHNF;
import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * <pre>
 *   { i = i0 -> xx | i = i1 -> yy }
 * </pre>
 *
 * @param term    the <code>i</code>
 * @param clauses the map from i0 to xx, i1 to yy
 * @implNote We haven't implemented smart update for this one.
 */
public record PartialTerm(
  @NotNull Term term,
  @NotNull ImmutableMap<DimTerm, Term> clauses
) implements StableWHNF {
  public @NotNull PartialTerm update(@NotNull Term term, @NotNull ImmutableMap<DimTerm, Term> clauses) {
    if (this.term == term && this.clauses == clauses) return this;
    if (term instanceof DimTerm dim && clauses.containsKey(dim)) {
      // Trim the map if we already know which side we are on.
      return new PartialTerm(term, ImmutableMap.of(dim, clauses.get(dim)));
    }
    return new PartialTerm(term, clauses);
  }

  @Override public @NotNull PartialTerm descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(term.descent(f),
      ImmutableMap.from(clauses.view().map((k, c) -> Tuple.of(k, c.descent(f)))));
  }
}
