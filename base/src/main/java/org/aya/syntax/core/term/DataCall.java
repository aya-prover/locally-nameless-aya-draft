// Copyright (c) 2020-2023 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term;

import kala.collection.immutable.ImmutableSeq;
import kala.function.IndexedFunction;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.DataDef;
import org.aya.syntax.ref.DefVar;
import org.aya.util.Arg;
import org.jetbrains.annotations.NotNull;

public record DataCall(
  @Override @NotNull DefVar<DataDef, TeleDecl.DataDecl> ref,
  @Override int ulift,
  @Override @NotNull ImmutableSeq<Arg<@NotNull Term>> args
) implements Callable.Tele, StableWHNF, Formation {
  public @NotNull DataCall update(@NotNull ImmutableSeq<Arg<Term>> args) {
    return args.sameElements(args(), true) ? this : new DataCall(ref, ulift, args);
  }

  @Override
  public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(args.map(arg -> arg.descent(x -> f.apply(0, x))));
  }

  // public @NotNull ConCall.Head conHead(@NotNull DefVar<CtorDef, TeleDecl.DataCtor> ctorRef) {
  //   return new ConCall.Head(ref, ctorRef, ulift, args);
  // }
}
