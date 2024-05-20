// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

import org.aya.syntax.core.term.Term;

public sealed interface JitCallable extends Compiled
  permits JitConCall, JitDataCall, JitFnCall {
  JitTele instance();
  int ulift();
  Term[] args();
}
