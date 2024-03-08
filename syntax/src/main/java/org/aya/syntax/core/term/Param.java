// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import org.aya.util.Arg;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Param(
  @Nullable String name,
  @NotNull Term type,
  boolean explicit
) {
  public Param(@NotNull Term type, boolean explicit) {
    this(null, type, explicit);
  }

  public @NotNull Arg<Term> toArg() {
    return new Arg<>(type, explicit);
  }
}
