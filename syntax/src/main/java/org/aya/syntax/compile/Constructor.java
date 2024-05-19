// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Result;
import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;

public abstract class Constructor extends Telescopic {
  public final Datatype dataType;

  protected Constructor(int telescopeSize, boolean[] telescopeLicit, String[] telescopeName, Datatype dataType) {
    super(telescopeSize, telescopeLicit, telescopeName);
    this.dataType = dataType;
  }

  protected abstract @NotNull Result<ImmutableSeq<Term>, Boolean> isAvailable(@NotNull Term[] args);

  public @NotNull JitConCall of(Term[] ownerArgs, Term... args) {
    return new JitConCall(this, ownerArgs, args);
  }
}
