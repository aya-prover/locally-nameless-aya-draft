// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.ref;

import kala.collection.mutable.MutableMap;
import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record LocalCtx(
  @NotNull MutableMap<LocalVar, Term> binds,
  @Nullable LocalCtx parent
) {
  @Contract("-> new")
  public @NotNull LocalCtx derive() {
    return new LocalCtx(MutableMap.create(), this);
  }

  public @NotNull Term get(@NotNull LocalVar name) {
    var ctx = this;
    Term result;

    while (ctx != null) {
      result = ctx.getLocal(name);
      if (result != null) return result;
      ctx = ctx.parent;
    }

    throw new UnsupportedOperationException("?");
  }

  public @Nullable Term getLocal(@NotNull LocalVar name) {
    return binds.getOrNull(name);
  }

  public void put(@NotNull LocalVar name, @NotNull Term type) {
    binds.put(name, type);
  }
}
