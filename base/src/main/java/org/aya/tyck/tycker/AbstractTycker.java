// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.tycker;

import org.aya.syntax.ref.LocalCtx;
import org.aya.tyck.UnifyTycker;
import org.aya.tyck.TyckState;
import org.aya.tyck.unify.TermComparator;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

public sealed abstract class AbstractTycker implements StateBased, ContextBased, Problematic permits UnifyTycker, TermComparator {
  public @NotNull TyckState state;
  private @NotNull LocalCtx localCtx;
  public final @NotNull Reporter reporter;

  protected AbstractTycker(@NotNull TyckState state, @NotNull Reporter reporter) {
    this(state, new LocalCtx(), reporter);
  }

  protected AbstractTycker(@NotNull TyckState state, @NotNull LocalCtx ctx, @NotNull Reporter reporter) {
    this.state = state;
    this.localCtx = ctx;
    this.reporter = reporter;
  }

  @Override public @NotNull LocalCtx localCtx() {
    return localCtx;
  }

  @Override public @NotNull LocalCtx setLocalCtx(@NotNull LocalCtx ctx) {
    var old = this.localCtx;
    this.localCtx = ctx;
    return old;
  }

  @Override public @NotNull TyckState state() {
    return state;
  }

  @Override public @NotNull Reporter reporter() {
    return reporter;
  }
}
