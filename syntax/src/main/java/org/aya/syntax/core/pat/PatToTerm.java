// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.pat;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.core.term.FreeTerm;
import org.aya.syntax.core.term.MetaPatTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.TupTerm;
import org.aya.syntax.core.term.call.ConCall;
import org.aya.syntax.core.term.call.ConCallLike;
import org.aya.syntax.core.term.repr.IntegerTerm;
import org.aya.syntax.ref.LocalCtx;
import org.aya.util.Pair;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface PatToTerm {
  static @NotNull Term visit(@NotNull Pat pat) {
    return new Unary(_ -> { }).apply(pat);
  }
  record Unary(@NotNull Consumer<Pat.Bind> freshCallback) implements Function<Pat, Term> {
    @Override public Term apply(Pat pat) {
      return switch (pat) {
        case Pat.Absurd _ -> Panic.unreachable();
        case Pat.Bind bind -> {
          freshCallback.accept(bind);
          yield new FreeTerm(bind.bind());
        }
        case Pat.Con con -> new ConCall(conHead(con), con.args().map(this));
        case Pat.Tuple tuple -> new TupTerm(tuple.elements().map(this));
        case Pat.Meta meta -> new MetaPatTerm(meta);
        case Pat.ShapedInt(var i, var recog, var data) -> new IntegerTerm(i, recog, data);
      };
    }
  }
  private static ConCallLike.@NotNull Head conHead(Pat.Con con) {
    return new ConCallLike.Head(con.data().ref(), con.ref(), 0, con.data().args());
  }

  record Binary(@NotNull LocalCtx ctx, @NotNull Unary unary) implements BiFunction<Pat, Pat, Term> {
    public Binary(@NotNull LocalCtx ctx) {
      this(ctx, new Unary(bind -> ctx.put(bind.bind(), bind.type())));
    }
    public @NotNull ImmutableSeq<Term> list(@NotNull ImmutableSeq<Pat> lhs, @NotNull ImmutableSeq<Pat> rhs) {
      return lhs.zip(rhs, this);
    }

    public @NotNull Term apply(@NotNull Pat l, @NotNull Pat r) {
      return switch (new Pair<>(l, r)) {
        case Pair(Pat.Bind _, var rhs) -> unary.apply(rhs);
        case Pair(var lhs, Pat.Bind _) -> unary.apply(lhs);
        // It must be the case that lhs.ref == rhs.ref
        case Pair(Pat.Con lhs, Pat.Con rhs) -> new ConCall(conHead(lhs), list(lhs.args(), rhs.args()));
        case Pair(Pat.Tuple lhs, Pat.Tuple rhs) -> new TupTerm(list(lhs.elements(), rhs.elements()));
        default -> Panic.unreachable();
      };
    }
  }
}
