// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import kala.function.IndexedFunction;
import org.aya.generic.AyaDocile;
import org.jetbrains.annotations.NotNull;

/**
 * @param isReallyError true if this is indeed an error,
 *                      false if this is just for pretty printing placeholder
 * @author ice1000
 * @see CorePrettier#term(BasePrettier.Outer, Term) (ErrorTerm case)
 */
public record ErrorTerm(AyaDocile description) implements StableWHNF {
  public static @NotNull ErrorTerm typeOf(@NotNull Term origin) {
    throw new UnsupportedOperationException("TODO");
    // return typeOf((AyaDocile) origin.freezeHoles(null)); TODO
  }

  public static @NotNull ErrorTerm typeOf(@NotNull AyaDocile doc) {
    throw new UnsupportedOperationException("TODO");  // TODO
  }

  @Override
  public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return this;
  }
}
