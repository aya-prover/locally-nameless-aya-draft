// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.marker;

import org.aya.syntax.compile.JitLamTerm;
import org.aya.syntax.core.term.FreeTerm;
import org.aya.syntax.core.term.LamTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.LocalVar;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

/**
 * An "abstract" Lambda Term.<br/>
 * We also use it to represent a term that contains a "free" {@link org.aya.syntax.core.term.LocalTerm},
 * so we can handle them in a scope-safe manner.
 */
public sealed interface UnaryClosure extends StableWHNF, UnaryOperator<Term> permits LamTerm, JitLamTerm {
  static @NotNull UnaryClosure mkConst(@NotNull Term term) { return new JitLamTerm(_ -> term); }

  /**
   * Corresponds to <emph>instantiate</emph> operator in [MM 2004],
   * <emph>instantiate</emph> the body with given {@param term}.
   * Called <code>apply</code> due to Mini-TT.
   */
  @Override Term apply(Term term);
  default @NotNull Term apply(LocalVar var) { return apply(new FreeTerm(var)); }
}
