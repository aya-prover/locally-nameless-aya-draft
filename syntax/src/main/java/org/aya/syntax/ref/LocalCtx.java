// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.ref;

import kala.collection.SeqView;
import kala.collection.mutable.MutableLinkedHashMap;
import kala.collection.mutable.MutableList;
import org.aya.syntax.core.term.Term;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

public record LocalCtx(
  @NotNull MutableLinkedHashMap<LocalVar, Term> binds,
  @NotNull MutableList<LocalVar> vars,
  @Nullable LocalCtx parent
) {
  public LocalCtx() {
    this(MutableLinkedHashMap.of(), MutableList.create(), null);
  }

  public boolean isEmpty() {
    return binds.isEmpty() && (parent == null || parent.isEmpty());
  }

  @Contract("-> new")
  public @NotNull LocalCtx derive() {
    return new LocalCtx(MutableLinkedHashMap.of(), MutableList.create(), this);
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
    throw new Panic("¿");
  }

  public @Nullable Term getLocal(@NotNull LocalVar name) {
    return binds.getOrNull(name);
  }

  public void put(@NotNull LocalVar name, @NotNull Term type) {
    binds.put(name, type);
    vars.append(name);
  }

  @Contract(value = "_ -> new", pure = true)
  public @NotNull LocalCtx map(UnaryOperator<Term> mapper) {
    var newBinds = this.binds.view()
      .mapValues((_, t) -> mapper.apply(t));

    return new LocalCtx(MutableLinkedHashMap.from(newBinds), vars, parent == null ? null : parent.map(mapper));
  }

  public SeqView<LocalVar> extract() {
    SeqView<LocalVar> parentView = parent == null ? SeqView.empty() : parent.extract();
    return parentView.concat(vars);
  }
}
