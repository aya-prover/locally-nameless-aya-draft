// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import org.aya.syntax.core.term.call.DataCall;

/**
 * Term formers, definitely.
 * Note that {@link PrimCall} may also be term formers, but not necessarily.
 */
public sealed interface Formation extends Term
  permits DataCall, PiTerm, SigmaTerm, SortTerm {
}
