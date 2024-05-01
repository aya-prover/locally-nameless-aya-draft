// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.core.term.xtt.DimTerm;
import org.aya.syntax.core.term.xtt.EqTerm;
import org.aya.syntax.core.term.xtt.PartialTerm;
import org.aya.syntax.core.term.xtt.PartialTyTerm;

/**
 * Cubical-stable WHNF: those who will not change to other term formers
 * after a substitution (this usually happens under face restrictions (aka cofibrations)).
 */
public sealed interface StableWHNF extends Term
  permits ErrorTerm, LamTerm, PiTerm, SigmaTerm, SortTerm, TupTerm, DataCall, DimTerm, EqTerm, PartialTerm, PartialTyTerm {
}
