// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

import kala.function.IndexedFunction;
import org.aya.syntax.core.term.FreeTerm;
import org.aya.syntax.core.term.LamTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.marker.UnaryClosure;
import org.aya.syntax.ref.LocalVar;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

public record JitLamTerm(@NotNull UnaryOperator<Term> lam) implements UnaryClosure {
  @Override public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) { return toLam().descent(f); }

  public @NotNull LamTerm toLam() {
    var negativeMatter = new LocalVar("positiveMatter");
    var inner = lam.apply(new FreeTerm(negativeMatter)).bind(negativeMatter);
    return new LamTerm(inner);
  }
  @Override public Term apply(Term term) { return lam.apply(term); }
}
