// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Option;
import kala.function.IndexedFunction;
import kala.tuple.Tuple2;
import org.aya.syntax.core.def.Def;
import org.aya.syntax.core.repr.AyaShape;
import org.aya.syntax.core.repr.ShapeRecognition;
import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.core.term.repr.IntegerTerm;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public record MetaLitTerm(
  @NotNull SourcePos sourcePos,
  @NotNull Object repr,
  @NotNull ImmutableSeq<Tuple2<Def, ShapeRecognition>> candidates,
  @NotNull Term type
) implements Term {
  public @NotNull MetaLitTerm update(@NotNull Term type) {
    return type == type() ? this : new MetaLitTerm(sourcePos, repr, candidates, type);
  }
  @Override public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(f.apply(0, type));
  }

  public @NotNull Term inline() {
    if (!(type instanceof DataCall dataCall)) return this;
    return candidates.find(t -> t.component1().ref() == dataCall.ref()).flatMap(t -> {
      var shape = t.component2().shape();
      if (shape == AyaShape.NAT_SHAPE) return Option.some(new IntegerTerm((int) repr, t.component2(), dataCall));
      // if (shape == AyaShape.LIST_SHAPE) return Option.some(new ListTerm((ImmutableSeq<Term>) repr, t.component2(), dataCall));
      return Option.<Term>none();
    }).getOrDefault(this);
  }
}
