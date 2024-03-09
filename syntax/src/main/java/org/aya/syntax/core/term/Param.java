// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import org.aya.syntax.ref.LocalVar;
import org.aya.util.Arg;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Param(@Nullable String name, @NotNull Term type, boolean explicit) {
  @Contract("_, _ -> new")
  public static @NotNull Param ofExplicit(@Nullable String name, @NotNull Term type) {
    return new Param(name, type, true);
  }

  public Param(@NotNull Term type, boolean explicit) {
    this(null, type, explicit);
  }

  public boolean nameEq(@Nullable String otherName) {
    return name != null && name.equals(otherName);
  }

  public @NotNull Arg<Term> toArg() {
    return new Arg<>(type, explicit);
  }

  public @NotNull Param implicitize() {
    return new Param(name, type, false);
  }

  public @NotNull Param bindAt(LocalVar ref, int i) {
    return new Param(name, type.bindAt(ref, i), explicit);
  }
}
