// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;

public abstract class JitData extends JitTele {
  protected final JitCon @NotNull [] constructors;

  protected JitData(int telescopeSize, boolean[] telescopeLicit, String[] telescopeName, int conAmount) {
    super(telescopeSize, telescopeLicit, telescopeName);
    this.constructors = new JitCon[conAmount];
  }

  /**
   * The constructor of this data type, should initialize {@link #constructors} at the first call.
   */
  public abstract @NotNull JitCon @NotNull [] constructors();

  public @NotNull JitDataCall of(Term... args) {
    return new JitDataCall(this, 0, args);
  }
}
