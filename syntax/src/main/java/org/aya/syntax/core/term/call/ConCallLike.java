// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.call;

import kala.collection.immutable.ImmutableSeq;
import kala.function.IndexedFunction;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.ConDef;
import org.aya.syntax.core.def.DataDef;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.repr.IntegerTerm;
import org.aya.syntax.ref.DefVar;
import org.jetbrains.annotations.NotNull;

/**
 * Terms that behave like a {@link ConCall}, for example:
 * <ul>
 *   <li>{@link IntegerTerm} behaves like a {@link ConCall}, in a efficient way</li>
 *   <li>{@link RuleReducer.Con} behaves like a {@link ConCall}, but it produce a special term</li>
 *   <li>Of course, {@link ConCall} behaves like a {@link ConCall}</li>
 * </ul>
 */
public sealed interface ConCallLike extends Callable.Tele permits ConCall, IntegerTerm {
  /**
   * @param dataArgs the arguments to the data type, NOT the constructor patterns!!
   *                 They need to be turned implicit when used as arguments.
   */
  record Head(
    @NotNull DefVar<DataDef, TeleDecl.DataDecl> dataRef,
    @NotNull DefVar<ConDef, TeleDecl.DataCon> ref,
    int ulift,
    @NotNull ImmutableSeq<@NotNull Term> dataArgs
  ) {
    public @NotNull DataCall underlyingDataCall() {
      return new DataCall(dataRef, ulift, dataArgs);
    }

    public @NotNull Head descent(@NotNull IndexedFunction<Term, Term> f) {
      var args = Callable.descent(dataArgs, f);
      if (args.sameElements(dataArgs, true)) return this;
      return new Head(dataRef, ref, ulift, args);
    }
  }

  @NotNull ConCallLike.Head head();
  @NotNull ImmutableSeq<Term> conArgs();

  @Override default @NotNull DefVar<ConDef, TeleDecl.DataCon> ref() {
    return head().ref();
  }

  @Override default @NotNull ImmutableSeq<@NotNull Term> args() {
    return head().dataArgs().view()
      .concat(conArgs())
      .toImmutableSeq();
  }

  @Override default int ulift() {
    return head().ulift();
  }
}
