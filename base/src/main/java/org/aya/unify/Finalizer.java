// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.unify;

import kala.collection.mutable.MutableStack;
import org.aya.syntax.core.term.MetaPatTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.MetaCall;
import org.aya.tyck.TyckState;
import org.aya.tyck.tycker.Stateful;
import org.jetbrains.annotations.NotNull;

public record Finalizer(
  @NotNull Stateful delegate,
  @NotNull MutableStack<Term> stack
) implements Stateful {
  @Override public @NotNull TyckState state() {
    return delegate.state();
  }

  public @NotNull Term doZonk(@NotNull Term preterm) {
    return switch (preterm) {
      case MetaCall meta -> whnf(meta);
      case MetaPatTerm meta -> whnf(meta);
      default -> preterm.descent(this::zonk);
    };
  }

  public @NotNull Term zonk(@NotNull Term term) {
    stack.push(term);
    var result = doZonk(term);
    // TODO: check whether result is a Meta(LitTerm|Call|PatTerm)
    stack.pop();
    return result;
  }
}
