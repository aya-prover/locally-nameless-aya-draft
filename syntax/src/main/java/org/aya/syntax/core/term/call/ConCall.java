// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.call;

import kala.collection.immutable.ImmutableSeq;
import kala.function.IndexedFunction;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.ConDef;
import org.aya.syntax.core.def.DataDef;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.DefVar;
import org.jetbrains.annotations.NotNull;

public record ConCall(
  @Override @NotNull ConCall.Head head,
  @Override @NotNull ImmutableSeq<Term> conArgs
) implements ConCallLike {
  public @NotNull ConCall update(@NotNull Head head, @NotNull ImmutableSeq<Term> conArgs) {
    return head == head() && conArgs.sameElements(conArgs(), true) ? this : new ConCall(head, conArgs);
  }

  @Override
  public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(head.descent(f), Callable.descent(conArgs, f));
  }

  public ConCall(
    @NotNull DefVar<DataDef, TeleDecl.DataDecl> dataRef,
    @NotNull DefVar<ConDef, TeleDecl.DataCon> ref,
    @NotNull ImmutableSeq<@NotNull Term> dataArgs,
    int ulift,
    @NotNull ImmutableSeq<@NotNull Term> conArgs
  ) {
    this(new Head(dataRef, ref, ulift, dataArgs), conArgs);
  }

  @Override public @NotNull Tele doElevate(int level) {
    return new ConCall(new Head(head.dataRef(), head.ref(), head.ulift() + level, head.dataArgs()), conArgs);
  }
}
