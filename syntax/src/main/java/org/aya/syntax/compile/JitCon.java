// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Result;
import org.aya.syntax.core.def.ConDefLike;
import org.aya.syntax.core.def.DataDefLike;
import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;

public abstract non-sealed class JitCon extends JitTeleDef implements ConDefLike {
  public final JitData dataType;

  protected JitCon(int telescopeSize, boolean[] telescopeLicit, String[] telescopeName, JitData dataType) {
    super(telescopeSize, telescopeLicit, telescopeName);
    this.dataType = dataType;
  }

  /**
   * Whether this constructor is available of data type
   * @param args the argument to the data type
   * @return a match result, a sequence of substitution if success
   */
  public abstract @NotNull Result<ImmutableSeq<Term>, Boolean> isAvailable(@NotNull Term[] args);
  @Override public @NotNull DataDefLike dataRef() { return dataType; }
}
