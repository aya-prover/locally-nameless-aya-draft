// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.resolve;

import org.aya.resolve.context.Context;
import org.aya.syntax.concrete.stmt.Stmt;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.jetbrains.annotations.NotNull;

/**
 * Stmt's that are resolving.
 */
public sealed interface ResolvingStmt {
  @NotNull Stmt stmt();

  sealed interface ResolvingDecl extends ResolvingStmt {
    @Override @NotNull Decl stmt();
  }

  sealed interface ResolvingTeleDecl extends ResolvingDecl {
    @Override @NotNull TeleDecl<?> stmt();
  }

  record TopDecl(@Override @NotNull TeleDecl<?> stmt, @NotNull Context innerCtx) implements ResolvingTeleDecl {}
  record MiscDecl(@Override @NotNull Decl stmt) implements ResolvingDecl {}
}
