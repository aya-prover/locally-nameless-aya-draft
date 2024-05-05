// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.prettier;

import kala.function.IndexedFunction;
import kala.value.primitive.MutableIntValue;
import org.aya.prettier.BasePrettier.Usage.Ref;
import org.aya.syntax.core.term.FreeTerm;
import org.aya.syntax.core.term.LocalTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.function.ToIntFunction;

public record FindUsage(@NotNull Ref ref) implements IndexedFunction<Term, Integer> {
  @Override
  public Integer apply(int index, Term term) {
    return switch (new Pair<>(term, ref)) {
      case Pair(FreeTerm(_), Ref.AnyFree _) -> 1;
      case Pair(FreeTerm(var var), Ref.Free(var fvar)) when var == fvar -> 1;
      case Pair(LocalTerm(var idx), Ref.Bound(var idy)) when idx == (idy + index) -> 1;
      default -> {
        var accMut = MutableIntValue.create();
        term.descent((l, t) -> {
          accMut.add(apply(index + l, t));
          return t;
        });

        yield accMut.get();
      }
    };
  }

  public static final @NotNull BasePrettier.Usage<Term, LocalVar> Free = (t, l) ->
    new FindUsage(new Ref.Free(l)).apply(0, t);
  public static final @NotNull ToIntFunction<Term> AnyFree = t ->
    new FindUsage(Ref.AnyFree.INSTANCE).apply(0, t);
  public static final @NotNull BasePrettier.Usage<Term, Integer> Bound = (t, i) ->
    new FindUsage(new Ref.Bound(i)).apply(0, t);
}
