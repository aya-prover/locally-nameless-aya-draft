// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import org.aya.generic.AyaDocile;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.LocalVar;
import org.jetbrains.annotations.NotNull;

public sealed interface Result {
  @NotNull Term wellTyped();
  @NotNull Term type();
  @NotNull Result freezeHoles(@NotNull TyckState state);

  default @NotNull Result.Default bind(@NotNull LocalVar var) {
    return new Default(wellTyped().bind(var), type().bind(var));
  }

  /**
   * {@link Default#type} is the type of {@link Default#wellTyped}.
   *
   * @author ice1000
   */
  record Default(@Override @NotNull Term wellTyped, @Override @NotNull Term type) implements Result {
    public static @NotNull Default error(@NotNull AyaDocile description) {
      throw new UnsupportedOperationException("TODO");
    }

    @Override public @NotNull Default freezeHoles(@NotNull TyckState state) {
      throw new UnsupportedOperationException("TODO");
    }
  }
}
