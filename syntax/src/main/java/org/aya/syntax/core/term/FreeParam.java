// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import org.aya.generic.ParamLike;
import org.aya.syntax.ref.LocalVar;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

/**
 * {@link Param} during tyck, we may need those {@link LocalVar}
 */
public record FreeParam(
  @Override @NotNull LocalVar ref,
  @Override @NotNull Term type,
  @Override boolean explicit
) implements ParamLike<Term> {
  @Override public @NotNull FreeParam map(@NotNull UnaryOperator<Term> mapper) {
    return new FreeParam(ref, mapper.apply(type), explicit);
  }

  // Who am I?
  public @NotNull Param forget() {
    return new Param(ref.name(), type, explicit);
  }
}
