// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.call;

import kala.collection.immutable.ImmutableSeq;
import kala.function.IndexedFunction;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.FnDef;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.DefVar;
import org.aya.util.Arg;
import org.jetbrains.annotations.NotNull;

public record FnCall(
  @Override @NotNull DefVar<FnDef, TeleDecl.FnDecl> ref,
  @Override int ulift,
  @Override @NotNull ImmutableSeq<@NotNull Term> args
) implements Callable.Tele {
  public @NotNull FnCall update(@NotNull ImmutableSeq<Term> args) {
    return args.sameElements(args(), true) ? this : new FnCall(ref, ulift, args);
  }

  @Override
  public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(args.map(x -> f.apply(0, x)));
  }

  @Override
  public @NotNull Tele applyTo(@NotNull Term arg) {
    return new FnCall(ref, ulift, args);
  }

  @Override public Tele elevate(int level) {
    return new FnCall(ref, ulift + level, args);
  }
}