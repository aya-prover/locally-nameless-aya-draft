// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.core.term.xtt.DimTyTerm;
import org.aya.syntax.core.term.xtt.EqTerm;
import org.aya.syntax.core.term.xtt.PartialTyTerm;

/**
 * Term formers, definitely.
 * Note that {@link PrimCall} may also be term formers, but not necessarily.
 */
public sealed interface Formation extends Term
  permits PiTerm, SigmaTerm, SortTerm, DataCall, DimTyTerm, EqTerm, PartialTyTerm {
}
