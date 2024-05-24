// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.marker;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.compile.JitCallable;
import org.aya.syntax.compile.JitFnCall;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.Callable;
import org.aya.syntax.core.term.call.FnCall;
import org.jetbrains.annotations.NotNull;

public sealed interface CallLike permits JitCallable, Callable.Common, CallLike.FnCallLike {
  @NotNull ImmutableSeq<@NotNull Term> args();
  int ulift();

  sealed interface FnCallLike extends CallLike permits JitFnCall, FnCall {

  }
}
