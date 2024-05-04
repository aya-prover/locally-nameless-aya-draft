// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.ref;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableMap;
import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record LocalCtx(
  @NotNull MutableMap<LocalVar, Term> binds,
  @NotNull MutableList<LocalVar> vars,
  @Nullable LocalCtx parent
) {
  public LocalCtx() {
    this(MutableMap.create(), MutableList.create(), null);
  }

  public boolean isEmpty() {
    return binds.isEmpty() && (parent == null || parent.isEmpty());
  }

  @Contract("-> new")
  public @NotNull LocalCtx derive() {
    return new LocalCtx(MutableMap.create(), MutableList.create(), this);
  }

  public int size() {
    return binds.size() + (parent == null ? 0 : parent.size());
  }

  public @NotNull Term get(@NotNull LocalVar name) {
    var ctx = this;
    while (ctx != null) {
      var result = ctx.getLocal(name);
      if (result != null) return result;
      ctx = ctx.parent;
    }
    throw new UnsupportedOperationException("¿");
  }

  public @Nullable Term getLocal(@NotNull LocalVar name) {
    return binds.getOrNull(name);
  }

  public void put(@NotNull LocalVar name, @NotNull Term type) {
    binds.put(name, type);
    vars.append(name);
  }

  public SeqView<LocalVar> extract() {
    SeqView<LocalVar> parentView = parent == null ? SeqView.empty() : parent.extract();
    return parentView.concat(vars);
  }

  // @Contract(mutates = "this")
  // public <R> R with(@NotNull LocalVar ref, Supplier<R> block) {
  //
  // }
}
