// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.call;

import kala.collection.immutable.ImmutableSeq;
import kala.function.IndexedFunction;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.core.def.Def;
import org.aya.syntax.core.term.AppTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.marker.CallLike;
import org.aya.syntax.ref.AnyVar;
import org.aya.syntax.ref.DefVar;
import org.jetbrains.annotations.NotNull;

/**
 * @see AppTerm#make()
 */
public sealed interface Callable extends Term permits Callable.Tele, MetaCall {
  @NotNull AnyVar ref();
  @NotNull ImmutableSeq<@NotNull Term> args();

  static @NotNull ImmutableSeq<Term> descent(ImmutableSeq<Term> args, IndexedFunction<Term, Term> f) {
    return args.map(arg -> f.apply(0, arg));
  }

  /**
   * Call to a {@link Decl}.
   */
  sealed interface Tele extends Callable, CallLike permits ConCallLike, DataCall, FnCall, PrimCall, RuleReducer {
    @Override @NotNull DefVar<? extends Def, ? extends Decl> ref();
    int ulift();
  }
}
