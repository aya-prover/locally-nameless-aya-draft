// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.resolve;

import kala.collection.immutable.ImmutableSeq;
import org.aya.resolve.context.ModuleContext;
import org.aya.resolve.context.WithCtx;
import org.aya.resolve.salt.Desalt;
import org.aya.resolve.visitor.StmtResolver;
import org.aya.resolve.visitor.StmtShallowResolver;
import org.aya.syntax.concrete.stmt.Stmt;
import org.jetbrains.annotations.NotNull;

public record StmtResolvers(@NotNull ResolveInfo info) {
  private @NotNull ImmutableSeq<WithCtx<Stmt>> fillContext(
    @NotNull ImmutableSeq<Stmt> stmts,
    @NotNull ModuleContext context
  ) {
    return new StmtShallowResolver(info).resolveStmt(stmts, context).zip(stmts, WithCtx::new);
  }

  private void resolve(@NotNull ImmutableSeq<WithCtx<Stmt>> stmts) {
    StmtResolver.resolveStmt(stmts, info);
  }

  private void desugar(@NotNull ImmutableSeq<Stmt> stmts) {
    var salt = new Desalt(info);
    stmts.forEach(stmt -> stmt.descentInPlace(salt, salt.pattern()));
  }

  /**
   * Resolve {@link Stmt}s under {@param context}
   */
  public void resolve(@NotNull ImmutableSeq<Stmt> stmts, @NotNull ModuleContext context) {
    resolve(fillContext(stmts, context));
    desugar(stmts); // resolve mutates stmts
  }
}
