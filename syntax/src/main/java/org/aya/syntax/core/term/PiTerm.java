// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import kala.collection.SeqLike;
import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import kala.function.IndexedFunction;
import org.aya.generic.SortKind;
import org.aya.syntax.core.term.marker.Formation;
import org.aya.syntax.core.term.marker.StableWHNF;
import org.aya.syntax.core.term.marker.UnaryClosure;
import org.aya.util.ForLSP;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

/**
 * @author re-xyr, kiva, ice1000
 */
public record PiTerm(@NotNull Term param, @NotNull UnaryClosure body) implements StableWHNF, Formation {
  public @NotNull PiTerm update(@NotNull Term param, @NotNull UnaryClosure body) {
    return param == this.param && body == this.body ? this : new PiTerm(param, body);
  }

  @Override public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(f.apply(0, param), (UnaryClosure) f.apply(1, body));
  }

  public record Unpi(@NotNull ImmutableSeq<Term> params, @NotNull Term body) { }
  public static @NotNull Unpi unpi(@NotNull Term term, @NotNull UnaryOperator<Term> pre) {
    var params = MutableList.<Term>create();
    while (pre.apply(term) instanceof PiTerm(var param, var body)) {
      params.append(param);
      term = body;
    }

    return new Unpi(params.toImmutableSeq(), term);
  }

  public static @NotNull SortTerm lub(@NotNull SortTerm domain, @NotNull SortTerm codomain) {
    var alift = domain.lift();
    var blift = codomain.lift();
    return switch (domain.kind()) {
      case Type -> switch (codomain.kind()) {
        case Type -> new SortTerm(SortKind.Type, Math.max(alift, blift));
        case ISet, Set -> new SortTerm(SortKind.Set, alift);
      };
      case ISet -> switch (codomain.kind()) {
        case ISet -> SortTerm.Set0;
        case Set, Type -> codomain;
      };
      case Set -> new SortTerm(SortKind.Set, Math.max(alift, blift));
    };
  }

  // public @NotNull LamTerm coe(@NotNull CoeTerm coe, @NotNull LamTerm.Param varI) {
  //   var M = new LamTerm.Param(new LocalVar("f"), true);
  //   var a = new LamTerm.Param(new LocalVar("a"), param.explicit());
  //   var arg = AppTerm.make(coe.inverse(new LamTerm(varI, param.type()).rename()), new Arg<>(a.toTerm(), true));
  //   var cover = CoeTerm.cover(varI, param, body, a.toTerm(), coe.s()).rename();
  //   return new LamTerm(M, new LamTerm(a,
  //     AppTerm.make(coe.recoe(cover),
  //       new Arg<>(AppTerm.make(M.toTerm(), new Arg<>(arg, param.explicit())), true))));
  // }

  public static @NotNull Term substBody(@NotNull Term pi, @NotNull SeqView<Term> args) {
    for (var arg : args) {
      if (pi instanceof PiTerm realPi) pi = realPi.body.apply(arg);
      else Panic.unreachable();
    }
    return pi;
  }

  @ForLSP
  public static @NotNull Term make(@NotNull SeqLike<@NotNull Term> telescope, @NotNull Term body) {
    return telescope.view().foldRight(body, (param, cod) -> new PiTerm(param, new LamTerm(cod)));
  }
}
