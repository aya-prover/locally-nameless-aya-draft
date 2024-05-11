// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.ctx;

import kala.collection.mutable.MutableLinkedHashMap;
import kala.control.Option;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.LocalVar;
import org.aya.tyck.Result;
import org.aya.util.Scoped;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A locally, lazy substitution<br/>
 * Every substitution should be well-scoped, i.e.,
 * {@link Term} can only refer to some free variable or some lazy substitution in this object.
 */
public record LocalSubstitution(
  @Override @Nullable LocalSubstitution parent,
  @NotNull MutableLinkedHashMap<LocalVar, Result> subst
) implements Scoped<LocalVar, Result, LocalSubstitution> {
  public LocalSubstitution() {
    this(null, MutableLinkedHashMap.of());
  }

  @Override
  public @NotNull LocalSubstitution self() {
    return this;
  }

  @Override
  public @NotNull LocalSubstitution derive() {
    return new LocalSubstitution(this, MutableLinkedHashMap.of());
  }

  @Override
  public @NotNull Option<Result> getLocal(@NotNull LocalVar key) {
    return subst.getOption(key);
  }

  @Override
  public void putLocal(@NotNull LocalVar key, @NotNull Result value) {
    subst.put(key, value);
  }
}
