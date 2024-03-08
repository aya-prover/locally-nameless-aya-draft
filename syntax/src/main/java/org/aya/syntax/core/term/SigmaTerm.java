// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
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
   *
   * @return null if "too many items" error occur
   */
  public <T> @Nullable TupTerm check(@NotNull ImmutableSeq<? extends T> it, @NotNull BiFunction<T, Term, Term> inherit) {
    var items = MutableList.<Term>create();
    var againstTele = params.view();
    var spine = MutableList.<Term>create();
    for (var iter = it.iterator(); iter.hasNext(); ) {
      var item = iter.next();
      var first = againstTele.getFirst()
        // the closest term (0) is the last term in spine
        .instantiateAll(spine.view().reversed());
      var result = inherit.apply(item, first);
      items.append(result);
      againstTele = againstTele.drop(1);
      if (againstTele.isNotEmpty())
        // LGTM! The show must go on
        spine.append(result);
      else if (iter.hasNext())
        // Too many items
        return null;
    }
    return new TupTerm(items.toImmutableArray());
  }
}
