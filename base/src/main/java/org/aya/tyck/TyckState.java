// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import org.aya.syntax.ref.DeBruijnCtx;
import org.aya.syntax.ref.LocalCtx;
import org.jetbrains.annotations.NotNull;

public record TyckState(
  @NotNull LocalCtx ctx,
  @NotNull DeBruijnCtx dCtx
  ) {
}
