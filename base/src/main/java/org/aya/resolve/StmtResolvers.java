// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.resolve;

import kala.collection.immutable.ImmutableSeq;
import org.aya.resolve.context.ModuleContext;
import org.aya.resolve.context.WithCtx;
import org.aya.resolve.visitor.StmtResolver;
import org.aya.resolve.visitor.StmtShallowResolver;
import org.aya.resolve.salt.Desalt;
import org.aya.syntax.concrete.stmt.Stmt;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.jetbrains.annotations.NotNull;

public class StmtResolvers {
  private StmtResolvers() {
  }

  private static @NotNull ImmutableSeq<WithCtx<Stmt>> fillContext(
    @NotNull ResolveInfo info,
    @NotNull ImmutableSeq<Stmt> stmts,
    @NotNull ModuleContext context
  ) {
    return new StmtShallowResolver(info).resolveStmt(stmts, context).zip(stmts, WithCtx::new);
  }

  private static void resolve(@NotNull ImmutableSeq<WithCtx<Stmt>> stmts) {
    StmtResolver.resolveStmt(stmts);
  }

  private static void desugar(@NotNull ResolveInfo info, @NotNull ImmutableSeq<Stmt> stmts) {
    var salt = new Desalt(info);
    stmts.forEach(stmt -> {
      if (stmt instanceof Decl decl) decl.descentInPlace(salt);
    });
  }

  /**
   * Resolve {@link Stmt}s under {@param context}
   */
  public static void resolve(@NotNull ResolveInfo info, @NotNull ImmutableSeq<Stmt> stmts, @NotNull ModuleContext context) {
    resolve(fillContext(info, stmts, context));
    desugar(info, stmts); // resolve mutates stmts
  }
}
