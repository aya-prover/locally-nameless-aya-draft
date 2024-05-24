// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.repr;

import kala.collection.immutable.ImmutableSeq;
import kala.function.IndexedFunction;
import org.aya.generic.stmt.Shaped;
import org.aya.syntax.core.def.ConDefLike;
import org.aya.syntax.core.repr.CodeShape;
import org.aya.syntax.core.repr.ShapeRecognition;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.ConCall;
import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.core.term.marker.StableWHNF;
import org.jetbrains.annotations.NotNull;

public record ListTerm(
  @Override @NotNull ImmutableSeq<Term> repr,
  @NotNull ConDefLike nil,
  @NotNull ConDefLike cons,
  @Override @NotNull DataCall type
) implements StableWHNF, Shaped.List<Term> {
  public ListTerm(
    @NotNull ImmutableSeq<Term> repr,
    @NotNull ShapeRecognition recog,
    @NotNull DataCall type
  ) {
    this(repr, recog.getCon(CodeShape.GlobalId.NIL), recog.getCon(CodeShape.GlobalId.CONS), type);
  }

  @Override public @NotNull Term makeNil(@NotNull Term dataArg) {
    return new ConCall(nil, ImmutableSeq.of(dataArg), 0, ImmutableSeq.empty());
  }

  @Override public @NotNull Term
  makeCons(@NotNull Term dataArg, @NotNull Term x, @NotNull Term last) {
    return new ConCall(cons, ImmutableSeq.of(dataArg), 0, ImmutableSeq.of(x, last));
  }

  @Override public @NotNull Term destruct(@NotNull ImmutableSeq<Term> repr) {
    return new ListTerm(repr, nil, cons, type);
  }
  public ListTerm update(@NotNull ImmutableSeq<Term> repr) {
    return repr.sameElements(this.repr, true) ? this : new ListTerm(repr, nil, cons, type);
  }
  @Override public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(repr.map(term -> f.apply(0, term)));
  }
}
