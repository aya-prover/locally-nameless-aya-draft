// Copyright (c) 2020-2023 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.call;

import kala.collection.immutable.ImmutableSeq;
import kala.function.IndexedFunction;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.CtorDef;
import org.aya.syntax.core.def.DataDef;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.DefVar;
import org.aya.util.Arg;
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
    return update(head.descent(x -> f.apply(0, x)), conArgs.map(x -> f.apply(0, x)));
  }

  public ConCall(
    @NotNull DefVar<DataDef, TeleDecl.DataDecl> dataRef,
    @NotNull DefVar<CtorDef, TeleDecl.DataCtor> ref,
    @NotNull ImmutableSeq<@NotNull Term> dataArgs,
    int ulift,
    @NotNull ImmutableSeq<@NotNull Term> conArgs
  ) {
    this(new Head(dataRef, ref, ulift, dataArgs), conArgs);
  }
}
