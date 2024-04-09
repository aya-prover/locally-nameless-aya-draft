// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.resolve;

import org.aya.resolve.context.Context;
import org.aya.syntax.concrete.stmt.Stmt;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.jetbrains.annotations.NotNull;

/**
 * Stmt's that are resolving
 */
public sealed interface ResolvingStmt {
  record DataDecl(@NotNull TeleDecl.DataDecl decl, @NotNull Context innerCtx) implements ResolvingStmt {}
  record Default(@NotNull Stmt stmt) implements ResolvingStmt {}
}
