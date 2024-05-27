// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.repr;

import kala.collection.immutable.ImmutableSeq;
import org.aya.generic.stmt.Shaped;
import org.aya.syntax.core.def.AnyDef;
import org.aya.syntax.core.def.ConDefLike;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.DataCall;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ListOps<Def extends AnyDef> extends Shaped.Applicable<Term, Def> {
  record ConRule(
    @Override @NotNull ConDefLike ref,
    @NotNull ListTerm empty,
    @Override @NotNull DataCall paramType
  ) implements ListOps<ConDefLike> {
    @Override public @Nullable Term apply(@NotNull ImmutableSeq<Term> args) {
      // empty
      if (args.isEmpty()) return empty;

      // cons
      assert args.sizeEquals(2);
      var x = args.get(0);
      var xs = args.get(0);
      if (xs instanceof ListTerm list)
        return list.update(list.repr().prepended(x));
      return null;
    }
  }
}
