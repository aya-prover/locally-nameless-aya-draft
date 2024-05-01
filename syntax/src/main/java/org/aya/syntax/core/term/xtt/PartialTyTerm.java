// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.xtt;

import kala.function.IndexedFunction;
import org.aya.syntax.core.term.Formation;
import org.aya.syntax.core.term.StableWHNF;
import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * <pre>
 *   Partial φ A
 * </pre>
 */
public record PartialTyTerm(@NotNull Term A, @NotNull Term phi) implements Formation, StableWHNF {
  public @NotNull PartialTyTerm update(Term A, Term phi) {
    if (this.A == A && this.phi == phi) return this;
    return new PartialTyTerm(A, phi);
  }

  @Override public @NotNull PartialTyTerm descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(A.descent(f), phi.descent(f));
  }
}
