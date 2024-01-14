// Copyright (c) 2020-2023 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

/**
 * Cubical-stable WHNF: those who will not change to other term formers
 * after a substitution (this usually happens under face restrictions (aka cofibrations)).
 */
public sealed interface StableWHNF extends Term
  permits DataCall, ErrorTerm, LamTerm, PiTerm, SigmaTerm, SortTerm, TupTerm {
}
