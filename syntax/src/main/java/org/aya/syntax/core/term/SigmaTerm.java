// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import kala.control.Result;
import kala.function.IndexedFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * @author re-xyr
 */
public record SigmaTerm(@NotNull ImmutableSeq<Term> params) implements StableWHNF, Formation {
  public @NotNull SigmaTerm update(@NotNull ImmutableSeq<Term> params) {
    return params.sameElements(params(), true) ? this : new SigmaTerm(params);
  }

  @Override
  public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(params.mapIndexed(f));
  }

  // public static @NotNull SortTerm lub(@NotNull SortTerm x, @NotNull SortTerm y) {
  //   int lift = Math.max(x.lift(), y.lift());
  //   if (x.kind() == SortKind.Set || y.kind() == SortKind.Set) {
  //     return new SortTerm(SortKind.Set, lift);
  //   } else if (x.kind() == SortKind.Type || y.kind() == SortKind.Type) {
  //     return new SortTerm(SortKind.Type, lift);
  //   } else if (x.kind() == SortKind.ISet || y.kind() == SortKind.ISet) {
  //     // ice: this is controversial, but I think it's fine.
  //     // See https://github.com/agda/cubical/pull/910#issuecomment-1233113020
  //     return SortTerm.ISet;
  //   }
  //   throw new AssertionError("unreachable");
  // }

  // public @NotNull LamTerm coe(@NotNull CoeTerm coe, @NotNull LamTerm.Param i) {
  //   var t = new RefTerm(new LocalVar("t"));
  //   assert params.sizeGreaterThanOrEquals(2);
  //   var items = MutableArrayList.<Arg<Term>>create(params.size());
  //   var subst = new Subst();
  //
  //   var ix = 1;
  //   for (var param : params) {
  //     // Item: t.ix
  //     var tn = new ProjTerm(t, ix++);
  //     // Because i : I |- params, so is i : I |- param, now bound An := \i. param
  //     var An = new LamTerm(i, param.type().subst(subst)).rename();
  //     // coe r s' (\i => A_n) t.ix
  //     UnaryOperator<Term> fill = s -> AppTerm.make(new CoeTerm(An, coe.r(), s),
  //       new Arg<>(tn, true));
  //
  //     subst.add(param.ref(), fill.apply(i.toTerm()));
  //     items.append(new Arg<>(fill.apply(coe.s()), param.explicit()));
  //   }
  //   return new LamTerm(new LamTerm.Param(t.var(), true),
  //     new TupTerm(items.toImmutableArray()));
  // }

  /**
   * A simple "generalized type checking" for tuples.
   */
  public <T> @NotNull Result<ImmutableSeq<Term>, ErrorKind> check(
    @NotNull ImmutableSeq<? extends T> elem,
    @NotNull BiFunction<@NotNull T, @NotNull Term, @Nullable Term> checker
  ) {
    var params = this.params.view();
    var args = MutableList.<Term>create();
    var iter = elem.iterator();

    while (iter.hasNext() && params.isNotEmpty()) {
      var item = iter.next();
      var first = params.getFirst().instantiateTele(args.view());
      var result = checker.apply(item, first);
      if (result == null) return Result.err(ErrorKind.CheckFailed);
      args.append(result);
      params = params.drop(1);
    }

    if (iter.hasNext()) return Result.err(ErrorKind.TooManyElement);
    if (params.isNotEmpty()) return Result.err(ErrorKind.TooManyParameter);
    return Result.ok(args.toImmutableSeq());
  }

  // ruast!
  public enum ErrorKind {
    TooManyElement,
    TooManyParameter,
    CheckFailed
  }
}
