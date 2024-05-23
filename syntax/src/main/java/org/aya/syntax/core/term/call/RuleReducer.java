// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.call;

import kala.collection.immutable.ImmutableSeq;
import kala.function.IndexedFunction;
import org.aya.generic.stmt.Shaped;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.ConDef;
import org.aya.syntax.core.def.FnDef;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.DefVar;
import org.jetbrains.annotations.NotNull;

public sealed interface RuleReducer extends Callable.Tele {
  @NotNull Shaped.Applicable<Term, ?, ?> rule();

  /**
   * A {@link Callable} for {@link Shaped.Applicable}.
   *
   * @param ulift
   * @param args
   */
  record Fn(
    @Override @NotNull Shaped.Applicable<Term, FnDef, TeleDecl.FnDecl> rule,
    @Override int ulift,
    @Override @NotNull ImmutableSeq<Term> args
  ) implements RuleReducer {
    @Override public @NotNull DefVar<FnDef, TeleDecl.FnDecl> ref() { return rule.ref(); }
    private @NotNull RuleReducer.Fn update(@NotNull ImmutableSeq<Term> args) {
      return args.sameElements(this.args, true)
        ? this : new Fn(rule, ulift, args);
    }

    @Override public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
      return update(Callable.descent(args, f));
    }
    public @NotNull FnCall toFnCall() { return new FnCall(rule.ref(), ulift, args); }
  }

  /**
   * A special {@link ConCall} which can be reduced to something interesting.
   */
  record Con(
    @NotNull Shaped.Applicable<Term, ConDef, TeleDecl.DataCon> rule,
    int ulift,
    @NotNull ImmutableSeq<Term> dataArgs,
    @Override @NotNull ImmutableSeq<Term> conArgs
  ) implements RuleReducer, ConCallLike {
    @Override public @NotNull ConCallLike.Head head() {
      return new Head(rule.ref().core.dataRef, rule.ref(), this.ulift, dataArgs);
    }

    public @NotNull RuleReducer.Con update(
      @NotNull ImmutableSeq<Term> dataArgs,
      @NotNull ImmutableSeq<Term> conArgs
    ) {
      return dataArgs.sameElements(this.dataArgs, true) && conArgs.sameElements(this.conArgs, true)
        ? this : new Con(rule, ulift, dataArgs, conArgs);
    }

    @Override public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
      return update(Callable.descent(dataArgs, f), Callable.descent(conArgs, f));
    }
  }
}
