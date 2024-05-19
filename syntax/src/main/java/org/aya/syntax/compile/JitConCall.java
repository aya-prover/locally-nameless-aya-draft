// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;

public record JitConCall(
  @NotNull JitCon instance,
  @NotNull Term[] ownerArgs,
  @NotNull Term[] conArgs
) implements Compiled {
}
