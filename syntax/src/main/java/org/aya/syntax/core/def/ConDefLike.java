// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.def;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.compile.JitCon;
import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;

public sealed interface ConDefLike extends AnyDef permits JitCon, ConDef {
  @NotNull DataDefLike dataRef();

  /** @return true if this is a path constructor */
  boolean isEq();
  @NotNull Term equality(Term[] args, boolean is0);
}
