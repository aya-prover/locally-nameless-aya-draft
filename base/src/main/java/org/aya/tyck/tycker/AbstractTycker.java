// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.tycker;

import org.aya.syntax.ref.DeBruijnCtx;
import org.aya.syntax.ref.LocalCtx;
import org.aya.tyck.ExprTycker;
import org.aya.tyck.TyckState;
import org.aya.tyck.unify.TermComparator;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

public sealed abstract class AbstractTycker implements StateBased, ContextBased, Problematic permits ExprTycker, TermComparator {
  public @NotNull TyckState state;
  private @NotNull LocalCtx localCtx;
  private @NotNull DeBruijnCtx deBruijnCtx;
  public final @NotNull Reporter reporter;

  protected AbstractTycker(@NotNull TyckState state, @NotNull LocalCtx ctx, @NotNull DeBruijnCtx dCtx, @NotNull Reporter reporter) {
    this.state = state;
    this.localCtx = ctx;
    this.deBruijnCtx = dCtx;
    this.reporter = reporter;
  }

  @Override public @NotNull LocalCtx localCtx() {
    return this.localCtx;
  }

  @Override public @NotNull DeBruijnCtx deBruijnCtx() {
    return deBruijnCtx;
  }

  @Override public @NotNull LocalCtx setLocalCtx(@NotNull LocalCtx ctx) {
    var old = this.localCtx;
    this.localCtx = ctx;
    return old;
  }

  @Override public @NotNull DeBruijnCtx setDeBruijnCtx(@NotNull DeBruijnCtx ctx) {
    var old = this.deBruijnCtx;
    this.deBruijnCtx = ctx;
    return old;
  }

  @Override public @NotNull TyckState state() {
    return state;
  }

  @Override public @NotNull Reporter reporter() {
    return reporter;
  }
}
