// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import org.aya.generic.AyaDocile;
import org.aya.pretty.doc.Doc;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.Arg;
import org.aya.util.prettier.PrettierOptions;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

public record Param(@NotNull String name, @NotNull Term type, boolean explicit) implements AyaDocile {
  @Contract("_, _ -> new")
  public static @NotNull Param ofExplicit(@NotNull String name, @NotNull Term type) {
    return new Param(name, type, true);
  }

  public boolean nameEq(@Nullable String otherName) {
    return name.equals(otherName);
  }

  public @NotNull Arg<Term> toArg() {
    return new Arg<>(type, explicit);
  }

  public @NotNull Param implicitize() {
    return new Param(name, type, false);
  }

  public @NotNull Param bindAt(LocalVar ref, int i) {
    return this.map(t -> t.bindAt(ref, i));
  }

  public @NotNull Param map(@NotNull UnaryOperator<Term> mapper) {
    return new Param(name, mapper.apply(type), explicit);
  }

  @Override
  public @NotNull Doc toDoc(@NotNull PrettierOptions options) {
    // TODO
    throw new UnsupportedOperationException("TODO");
  }
}
