// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.repr;

import kala.collection.immutable.ImmutableSeq;
import kala.function.IndexedFunction;
import org.aya.generic.stmt.Shaped;
import org.aya.syntax.core.def.ConDef;
import org.aya.syntax.core.repr.ShapeRecognition;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.ConCall;
import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.core.term.marker.StableWHNF;
import org.jetbrains.annotations.NotNull;

public record ListTerm(
  @Override @NotNull ImmutableSeq<Term> repr,
  @Override @NotNull ShapeRecognition recognition,
  @Override @NotNull DataCall type
) implements StableWHNF, Shaped.List<Term> {
  @Override public @NotNull Term makeNil(@NotNull ConDef nil, @NotNull Term dataArg) {
    return new ConCall(nil.dataRef, nil.ref(), ImmutableSeq.of(dataArg), 0, ImmutableSeq.empty());
  }

  @Override public @NotNull Term
  makeCons(@NotNull ConDef cons, @NotNull Term dataArg, @NotNull Term x, @NotNull Term last) {
    return new ConCall(cons.dataRef, cons.ref(), ImmutableSeq.of(dataArg), 0,
      ImmutableSeq.of(x, last));
  }

  @Override public @NotNull Term destruct(@NotNull ImmutableSeq<Term> repr) {
    return new ListTerm(repr, recognition, type);
  }
  public ListTerm update(@NotNull ImmutableSeq<Term> repr) {
    return repr.sameElements(this.repr, true) ? this : new ListTerm(repr, recognition, type);
  }
  @Override public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(repr.map(term -> f.apply(0, term)));
  }
}
