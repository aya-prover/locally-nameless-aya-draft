// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class JitFn extends JitTele {
  protected JitFn(int telescopeSize, boolean[] telescopeLicit, String[] telescopeName) {
    super(telescopeSize, telescopeLicit, telescopeName);
  }

  /**
   * Unfold this function
   * @return null if stuck
   */
  public abstract @Nullable Term invoke(Term... args);

  public @NotNull JitFnCall of(Term... args) {
    return new JitFnCall(this, 0, args);
  }
}
