// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.generic;

import org.jetbrains.annotations.NotNull;

public sealed interface TyckOrder {
  @NotNull TyckUnit unit();

  /** header order */
  record Head(@NotNull TyckUnit unit) implements TyckOrder {}

  /** body order */
  record Body(@NotNull TyckUnit unit) implements TyckOrder {}
}
