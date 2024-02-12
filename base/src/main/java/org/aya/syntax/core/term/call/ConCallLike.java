// Copyright (c) 2020-2023 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.call;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.CtorDef;
import org.aya.syntax.core.def.DataDef;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.DefVar;
import org.aya.util.Arg;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

/**
 * Terms that behave like a {@link ConCall}, for example:
 * <ul>
 *   <li>{@link IntegerTerm} behaves like a {@link ConCall}, in a efficient way</li>
 *   <li>{@link RuleReducer.Con} behaves like a {@link ConCall}, but it produce a special term</li>
 *   <li>Of course, {@link ConCall} haves like a {@link ConCall}</li>
 * </ul>
 */
public sealed interface ConCallLike extends Callable.Tele permits ConCall {
  /**
   * @param dataArgs the arguments to the data type, NOT the constructor patterns!!
   *                 They need to be turned implicit when used as arguments.
   * @see org.aya.tyck.pat.PatternTycker#mischa
   */
  record Head(
    @NotNull DefVar<DataDef, TeleDecl.DataDecl> dataRef,
    @NotNull DefVar<CtorDef, TeleDecl.DataCtor> ref,
    int ulift,
    @NotNull ImmutableSeq<@NotNull Term> dataArgs
  ) {
    public @NotNull DataCall underlyingDataCall() {
      return new DataCall(dataRef, ulift, dataArgs);
    }

    public @NotNull Head descent(@NotNull UnaryOperator<@NotNull Term> f) {
      var args = dataArgs.map(f::apply);
      if (args.sameElements(dataArgs, true)) return this;
      return new Head(dataRef, ref, ulift, args);
    }
  }

  @NotNull ConCallLike.Head head();
  @NotNull ImmutableSeq<Term> conArgs();

  @Override
  default @NotNull DefVar<CtorDef, TeleDecl.DataCtor> ref() {
    return head().ref();
  }

  @Override
  default @NotNull ImmutableSeq<@NotNull Term> args() {
    return head().dataArgs().view()
      .concat(conArgs())
      .toImmutableSeq();
  }

  @Override
  default int ulift() {
    return head().ulift();
  }
}
