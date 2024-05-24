// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.core.term.marker.CallLike;
import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;

public sealed interface JitCallable extends Compiled, CallLike
  permits JitConCall, JitDataCall, JitFnCall {
  JitTele instance();
  @Override int ulift();
  @NotNull ImmutableSeq<@NotNull Term> args();
}
