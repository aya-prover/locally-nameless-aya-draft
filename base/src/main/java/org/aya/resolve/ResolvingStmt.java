// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.resolve;

import org.aya.resolve.context.Context;
import org.aya.syntax.concrete.stmt.Stmt;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.jetbrains.annotations.NotNull;

/**
 * {@link Stmt} that is resolving, stores some extra information (i.e. the context 'inside' of it).
 * This is a functional construction, it is similar to the following agda code:
 *
 * <pre>
 *   postulate
 *     Context : Set
 *
 *   data Stmt : Set where
 *     FnDecl : Stmt
 *     DataDecl : Stmt
 *     DataCtor : Stmt
 *
 *   data ExtInfo : Stmt -> Set where
 *     ExtData : Context -> ExtInfo DataDecl
 *     ExtFn : Context -> ExtInfo FnDecl
 *     -- trivial extra info
 *     ExtCtor : ExtInfo DataCtor
 *
 *   ResolvingStmt : Set _
 *   ResolvingStmt = Σ[ s ∈ Stmt ] ExtInfo s
 * </pre>
 *
 * FIXME: I agree, we can use {@code Option<Context>} for now.
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
