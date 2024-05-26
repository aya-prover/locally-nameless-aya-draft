// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.ref;

import org.aya.syntax.compile.JitTele;
import org.jetbrains.annotations.NotNull;

public record CompiledVar(
  @NotNull JitTele core,
  @Override @NotNull String name
) implements AnyVar {
}
