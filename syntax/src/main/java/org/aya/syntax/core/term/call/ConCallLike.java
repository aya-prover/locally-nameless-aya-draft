// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.call;

import kala.collection.immutable.ImmutableSeq;
import kala.function.IndexedFunction;
import org.aya.syntax.concrete.stmt.decl.DataCon;
import org.aya.syntax.concrete.stmt.decl.DataDecl;
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
public sealed interface ConCallLike extends Callable.Tele permits ConCall, RuleReducer.Con, IntegerTerm {
  /**
   * @param ownerArgs the arguments to the owner/patterns, NOT the data type parameters!!
   */
  record Head(
    @NotNull DefVar<DataDef, DataDecl> dataRef,
    @NotNull DefVar<ConDef, DataCon> ref,
    int ulift,
    @NotNull ImmutableSeq<@NotNull Term> ownerArgs
  ) {
    public @NotNull Head descent(@NotNull IndexedFunction<Term, Term> f) {
      var args = Callable.descent(ownerArgs, f);
      if (args.sameElements(ownerArgs, true)) return this;
      return new Head(dataRef, ref, ulift, args);
    }
  }

  @NotNull ConCallLike.Head head();
  @NotNull ImmutableSeq<Term> conArgs();

  @Override default @NotNull DefVar<ConDef, DataCon> ref() { return head().ref; }

  @Override default @NotNull ImmutableSeq<@NotNull Term> args() {
    return head().ownerArgs().concat(conArgs());
  }

  @Override default int ulift() { return head().ulift; }
}
