// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.repr;

import kala.collection.immutable.ImmutableSeq;
import kala.function.IndexedFunction;
import org.aya.generic.Shaped;
import org.aya.syntax.core.def.ConDef;
import org.aya.syntax.core.repr.CodeShape;
import org.aya.syntax.core.repr.ShapeRecognition;
import org.aya.syntax.core.term.StableWHNF;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.ConCall;
import org.aya.syntax.core.term.call.ConCallLike;
import org.aya.syntax.core.term.call.DataCall;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntUnaryOperator;

/**
 * An efficient represent for Nat
 */
public record IntegerTerm(
  @Override int repr,
  @Override @NotNull ShapeRecognition recognition,
  @Override @NotNull DataCall type
) implements StableWHNF, Shaped.Nat<Term>, ConCallLike {
  public IntegerTerm {
    assert repr >= 0;
  }

  @Override
  public @NotNull ConCall.Head head() {
    var ref = repr == 0
      ? ctorRef(CodeShape.GlobalId.ZERO)
      : ctorRef(CodeShape.GlobalId.SUC);

    return new ConCallLike.Head(type.ref(), ref.core.ref, 0, ImmutableSeq.empty());
  }

  @Override
  public @NotNull ImmutableSeq<Term> conArgs() {
    if (repr == 0) return ImmutableSeq.empty();
    var conTele = head().ref().core.selfTele;
    assert conTele.sizeEquals(1);

    return ImmutableSeq.of(new IntegerTerm(repr - 1, recognition, type));
  }

  @Override public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) { return this; }
  @Override public @NotNull Term constructorForm() {
    // I AM the constructor form.
    return this;
  }

  @Override public @NotNull Term makeZero(@NotNull ConDef zero) {
    return map(x -> 0);
  }

  @Override public @NotNull Term makeSuc(@NotNull ConDef suc, @NotNull Term term) {
    return new ConCall(suc.dataRef, suc.ref, ImmutableSeq.empty(), 0,
      ImmutableSeq.of(term));
    // return new RuleReducer.Con(new IntegerOps.ConRule(suc.ref, recognition, type),
    //   0, ImmutableSeq.empty(), ImmutableSeq.of(term));
  }

  @Override public @NotNull Term destruct(int repr) {
    return new IntegerTerm(repr, this.recognition, this.type);
  }

  @Override public @NotNull IntegerTerm map(@NotNull IntUnaryOperator f) {
    return new IntegerTerm(f.applyAsInt(repr), recognition, type);
  }
}
