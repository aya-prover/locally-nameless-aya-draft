// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.marker;

import org.aya.syntax.compile.JitLamTerm;
import org.aya.syntax.core.term.LamTerm;
import org.aya.syntax.core.term.Term;

import java.util.function.UnaryOperator;

public sealed interface UnaryClosure extends StableWHNF, UnaryOperator<Term> permits LamTerm, JitLamTerm {
}
