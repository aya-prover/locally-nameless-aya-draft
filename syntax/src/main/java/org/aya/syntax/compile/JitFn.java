// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

import org.aya.syntax.core.def.FnDefLike;
import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;

public abstract non-sealed class JitFn extends JitTeleDef implements FnDefLike {
  protected JitFn(int telescopeSize, boolean[] telescopeLicit, String[] telescopeName) {
    super(telescopeSize, telescopeLicit, telescopeName);
  }

  /**
   * Unfold this function
   */
  public abstract @NotNull Term invoke(Term stuck, Term... args);
}
