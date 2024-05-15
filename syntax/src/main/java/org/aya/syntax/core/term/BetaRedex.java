// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import org.aya.syntax.core.term.xtt.PAppTerm;
import org.jetbrains.annotations.NotNull;

public sealed interface BetaRedex extends Term permits AppTerm, PAppTerm, ProjTerm {
  @NotNull Term make();
}
