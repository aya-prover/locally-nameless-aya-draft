// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.call;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.TeleDef;
import org.aya.syntax.core.term.AppTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.AnyVar;
import org.aya.syntax.ref.DefVar;
import org.aya.util.Arg;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * @author ice1000
 * @see AppTerm#make(AppTerm)
 */
public sealed interface Callable extends Term permits Callable.Common {
  @NotNull AnyVar ref();
  @NotNull ImmutableSeq<@NotNull Arg<Term>> args();
  /**
   * Call to a {@link TeleDecl}.
   */
  sealed interface Tele extends Common permits DataCall, FnCall {
    @Override @NotNull DefVar<? extends TeleDef, ? extends TeleDecl<?>> ref();
    int ulift();
  }

  sealed interface Common extends Callable permits Tele {
    @Override @NotNull DefVar<? extends TeleDef, ? extends Decl> ref();
    int ulift();
  }

  /** This exists solely for simplifying code in the tycker. */
  @FunctionalInterface
  interface Factory<D extends TeleDef, S extends Decl> {
    @Contract(pure = true, value = "_,_,_->new") @NotNull Callable make(
      DefVar<D, S> defVar,
      int ulift,
      ImmutableSeq<@NotNull Arg<Term>> args
    );
  }
}
