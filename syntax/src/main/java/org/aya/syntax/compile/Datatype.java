// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;

public abstract class Datatype extends Telescopic {
  protected Datatype(int telescopeSize, boolean[] telescopeLicit, String[] telescopeName) {
    super(telescopeSize, telescopeLicit, telescopeName);
  }

  public abstract @NotNull Constructor[] constructors();

  public @NotNull JitDataCall of(Term... args) {
    return new JitDataCall(this, args);
  }
}
