// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.ctx;

import kala.collection.mutable.MutableLinkedHashMap;
import kala.control.Option;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.LocalVar;
import org.aya.tyck.Jdg;
import org.aya.util.Scoped;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A locally, lazy substitution<br/>
 * Every substitution should be well-scoped, i.e.,
 * {@link Jdg} can only refer to some free variable or elder lazy substitution.
 */
public record LocalSubstitution(
  @Override @Nullable LocalSubstitution parent,
  @NotNull MutableLinkedHashMap<LocalVar, Jdg> subst
) implements Scoped<LocalVar, Jdg, LocalSubstitution> {
  public LocalSubstitution() { this(null, MutableLinkedHashMap.of()); }
  @Override public @NotNull LocalSubstitution self() { return this; }

  @Override public @NotNull LocalSubstitution derive() {
    return new LocalSubstitution(this, MutableLinkedHashMap.of());
  }

  @Override public @NotNull Option<Jdg> getLocal(@NotNull LocalVar key) {
    return subst.getOption(key);
  }

  @Override public void putLocal(@NotNull LocalVar key, @NotNull Jdg value) { subst.put(key, value); }

  public void put(@NotNull LocalVar var, @NotNull Term term, @NotNull Term type) {
    put(var, new Jdg.Default(term, type));
  }
}
