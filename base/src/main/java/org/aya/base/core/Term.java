package org.aya.base.core;

import org.aya.base.generic.LocalVar;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public sealed interface Term extends Serializable
    permits AppTerm, FreeTerm, LamTerm, LocalTerm {
  @ApiStatus.Internal
  @NotNull Term bindAt(@NotNull LocalVar var, int depth);

  default @NotNull Term bind(@NotNull LocalVar var) {
    return bindAt(var, 0);
  }
}
