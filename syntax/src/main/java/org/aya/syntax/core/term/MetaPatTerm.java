// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import kala.function.IndexedFunction;
import org.aya.syntax.core.pat.Pat;
import org.jetbrains.annotations.NotNull;

/**
 * A meta-like term, but it will be solved while pattern tyck
 */
public record MetaPatTerm(@NotNull Pat.Meta meta) implements Term {
  @Override
  public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return this;
  }
}
