// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import kala.function.IndexedFunction;
import kala.tuple.Tuple;
import kala.tuple.Tuple2;
import org.jetbrains.annotations.NotNull;

public record AppTerm(@NotNull Term fun, @NotNull Term arg) implements Term {
  public @NotNull AppTerm update(@NotNull Term fun, @NotNull Term arg) {
    return fun == this.fun && arg == this.arg ? this : new AppTerm(fun, arg);
  }

  @Override
  public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(f.apply(0, fun), f.apply(0, arg));
  }

  public static @NotNull Term make(@NotNull Term f, @NotNull Term a) {
    return new AppTerm(f, a).make();
  }

  public @NotNull Term make() {
    return switch (fun) {
      case LamTerm(var body) -> body.instantiate(arg);
      default -> this;
    };
  }

  public static @NotNull Tuple2<ImmutableSeq<Term>, Term> unapp(@NotNull Term maybeApp) {
    var args = MutableList.<Term>create();
    while (maybeApp instanceof AppTerm(var f, var a)) {
      maybeApp = f;
      args.append(a);
    }

    return Tuple.of(args.reversed(), maybeApp);
  }
}
