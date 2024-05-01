// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.xtt;

import kala.function.IndexedFunction;
import org.aya.syntax.core.term.Formation;
import org.aya.syntax.core.term.StableWHNF;
import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * <code>PathP (x. A) a b</code>
 */
public record EqTerm(Term A, Term a, Term b) implements Formation, StableWHNF {
  public @NotNull EqTerm update(Term A, Term a, Term b) {
    if (this.A == A && this.a == a && this.b == b) return this;
    return new EqTerm(A, a, b);
  }

  @Override public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(A.descent(f), a.descent(f), b.descent(f));
  }
}
