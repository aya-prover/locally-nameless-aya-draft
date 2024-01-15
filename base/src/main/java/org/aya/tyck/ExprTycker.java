// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.LocalCtx;
import org.aya.tyck.tycker.AbstractExprTycker;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

public final class ExprTycker extends AbstractExprTycker {
  public ExprTycker(@NotNull TyckState state, @NotNull LocalCtx ctx, @NotNull Reporter reporter) {
    super(state, ctx, reporter);
  }

  @Override
  public @NotNull Term whnf(@NotNull Term term) {
    throw new UnsupportedOperationException("TODO");
  }
}
