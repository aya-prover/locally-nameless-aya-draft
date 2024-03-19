// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.xtt;

import kala.function.IndexedFunction;
import org.aya.syntax.core.term.Formation;
import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;

public enum DimTyTerm implements Formation {
  INSTANCE;

  @Override public @NotNull DimTyTerm descent(@NotNull IndexedFunction<Term, Term> f) {
    return this;
  }
}
