// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.normalize;

import kala.collection.Map;
import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.PrimDef;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.PrimCall;
import org.aya.syntax.ref.DefVar;
import org.aya.tyck.TyckState;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Function;

import static org.aya.syntax.core.def.PrimDef.*;

public record PrimFactory(@NotNull Map<@NotNull ID, @NotNull PrimSeed> seeds) {
  @FunctionalInterface
  interface Unfolder extends BiFunction<@NotNull PrimCall, @NotNull TyckState, @NotNull Term> {
  }

  record PrimSeed(
    @NotNull ID name,
    @NotNull Unfolder unfold,
    @NotNull Function<@NotNull DefVar<PrimDef, TeleDecl.PrimDecl>, @NotNull PrimDef> supplier,
    @NotNull ImmutableSeq<@NotNull ID> dependency
  ) {
    public @NotNull PrimDef supply(@NotNull DefVar<PrimDef, TeleDecl.PrimDecl> ref) {
      return supplier.apply(ref);
    }
  }
}
