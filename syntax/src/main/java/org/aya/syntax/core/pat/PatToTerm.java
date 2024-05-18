// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.pat;

import kala.collection.immutable.ImmutableSeq;
import kala.tuple.Tuple2;
import org.aya.syntax.core.term.FreeTerm;
import org.aya.syntax.core.term.MetaPatTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.TupTerm;
import org.aya.syntax.core.term.call.ConCall;
import org.aya.syntax.core.term.call.ConCallLike;
import org.aya.syntax.core.term.repr.IntegerTerm;
import org.aya.syntax.ref.LocalCtx;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

public interface PatToTerm {
  static @NotNull Term visit(@NotNull Pat pat) {
    return switch (pat) {
      case Pat.Absurd _ -> Panic.unreachable();
      case Pat.Bind bind -> new FreeTerm(bind.bind());
      case Pat.Con con -> new ConCall(conHead(con), con.args().map(PatToTerm::visit));
      case Pat.Tuple tuple -> new TupTerm(tuple.elements().map(PatToTerm::visit));
      case Pat.Meta meta -> new MetaPatTerm(meta);
      case Pat.ShapedInt(var i, var recog, var data) -> new IntegerTerm(i, recog, data);
    };
  }
  private static ConCallLike.@NotNull Head conHead(Pat.Con con) {
    return new ConCallLike.Head(con.data().ref(), con.ref(), 0, con.data().args());
  }

  record Binary(@NotNull LocalCtx ctx) {
    public @NotNull ImmutableSeq<Term> visit(@NotNull ImmutableSeq<Pat> lhs, @NotNull ImmutableSeq<Pat> rhs) {
      return lhs.zipView(rhs).map(this::visit).toImmutableSeq();
    }

    public @NotNull Term visit(@NotNull Tuple2<Pat, Pat> pat) {
      return switch (pat) {
        case Tuple2(Pat.Bind _, var rhs) -> PatToTerm.visit(rhs);
        case Tuple2(var lhs, Pat.Bind _) -> PatToTerm.visit(lhs);
        // It must be the case that lhs.ref == rhs.ref
        case Tuple2(Pat.Con lhs, Pat.Con rhs) -> new ConCall(conHead(lhs), visit(lhs.args(), rhs.args()));
        case Tuple2(Pat.Tuple lhs, Pat.Tuple rhs) -> new TupTerm(visit(lhs.elements(), rhs.elements()));
        default -> Panic.unreachable();
      };
    }
  }
}
