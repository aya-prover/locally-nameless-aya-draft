// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.resolve;

import kala.collection.immutable.ImmutableSeq;
import org.aya.resolve.context.ModuleContext;
import org.aya.resolve.context.WithCtx;
import org.aya.resolve.visitor.StmtResolver;
import org.aya.resolve.visitor.StmtShallowResolver;
import org.aya.syntax.concrete.Salt;
import org.aya.syntax.concrete.stmt.Stmt;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.jetbrains.annotations.NotNull;

public class StmtResolvers {
  private StmtResolvers() {
  }

  private static @NotNull ImmutableSeq<WithCtx<Stmt>> fillContext(
    @NotNull ImmutableSeq<Stmt> stmts,
    @NotNull ModuleContext context
  ) {
    return new StmtShallowResolver().resolveStmt(stmts, context).zip(stmts, WithCtx::new);
  }

  private static void resolve(@NotNull ImmutableSeq<WithCtx<Stmt>> stmts) {
    StmtResolver.resolveStmt(stmts);
  }

  private static void desugar(@NotNull ImmutableSeq<Stmt> stmts) {
    var salt = new Salt();
    stmts.forEach(stmt -> {
      if (stmt instanceof Decl decl) decl.descentInPlace(salt);
    });
  }

  /**
   * Resolve {@link Stmt}s under {@param context}
   */
  public static void resolve(@NotNull ImmutableSeq<Stmt> stmts, @NotNull ModuleContext context) {
    resolve(fillContext(stmts, context));
    desugar(stmts);   // resolve mutates stmts
  }
}
