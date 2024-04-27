// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.prettier;

import kala.function.IndexedFunction;
import kala.value.primitive.MutableIntValue;
import org.aya.syntax.core.term.FreeTerm;
import org.aya.syntax.core.term.LocalTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.LocalVar;
import org.jetbrains.annotations.NotNull;

public record FindUsage(@NotNull BasePrettier.Usage.Ref ref) implements IndexedFunction<Term, Integer> {
  @Override
  public Integer apply(int index, Term term) {
    return switch (term) {
      case FreeTerm(var var) when ref instanceof BasePrettier.Usage.Ref.Free(var fvar)
        && var == fvar -> 1;
      case LocalTerm(var idx) when ref instanceof BasePrettier.Usage.Ref.Bound(var idy)
        && idx == (idy + index) -> 1;
      default -> {
        MutableIntValue accMut = MutableIntValue.create();

        term.descent((l, t) -> {
          accMut.add(apply(index + l, t));
          return t;
        });

        yield accMut.get();
      }
    };
  }

  public static final @NotNull BasePrettier.Usage<Term, LocalVar> Free = (t, l) ->
    new FindUsage(new BasePrettier.Usage.Ref.Free(l)).apply(0, t);

  public static final @NotNull BasePrettier.Usage<Term, Integer> Bound = (t, i) ->
    new FindUsage(new BasePrettier.Usage.Ref.Bound(i)).apply(0, t);
}
