// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.resolve.visitor;

import kala.collection.SeqLike;
import org.aya.resolve.ResolveInfo;
import org.aya.resolve.ResolvingStmt;
import org.aya.resolve.ResolvingStmt.TopDecl;
import org.aya.resolve.context.Context;
import org.aya.resolve.error.NameProblem;
import org.aya.resolve.error.OperatorError;
import org.aya.resolve.salt.AyaBinOpSet;
import org.aya.syntax.concrete.stmt.BindBlock;
import org.aya.syntax.concrete.stmt.QualifiedID;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.ref.DefVar;
import org.aya.util.binop.OpDecl;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

import static org.aya.resolve.ResolvingStmt.*;

public interface StmtBinder {
  static void visitBind(@NotNull Context ctx, @NotNull DefVar<?, ?> selfDef, @NotNull BindBlock bind, @NotNull ResolveInfo info) {
    var opSet = info.opSet();
    var self = selfDef.opDecl;
    if (self == null && bind != BindBlock.EMPTY) {
      opSet.reporter.report(new OperatorError.BadBindBlock(selfDef.concrete.sourcePos(), selfDef.name()));
      throw new Context.ResolvingInterruptedException();
    }
    bind(ctx, bind, opSet, self);
  }

  /**
   * Bind {@param bindBlock} to {@param opSet} in {@param ctx}
   */
  static void bind(@NotNull Context ctx, @NotNull BindBlock bindBlock, AyaBinOpSet opSet, OpDecl self) {
    if (bindBlock == BindBlock.EMPTY) return;
    bindBlock.resolvedLoosers().set(bindBlock.loosers().map(looser -> bind(self, opSet, ctx, OpDecl.BindPred.Looser, looser)));
    bindBlock.resolvedTighters().set(bindBlock.tighters().map(tighter -> bind(self, opSet, ctx, OpDecl.BindPred.Tighter, tighter)));
  }

  static @NotNull DefVar<?, ?> bind(
    @NotNull OpDecl self, @NotNull AyaBinOpSet opSet, @NotNull Context ctx,
    @NotNull OpDecl.BindPred pred, @NotNull QualifiedID id
  ) throws Context.ResolvingInterruptedException {
    if (ctx.get(id) instanceof DefVar<?, ?> defVar) {
      var opDecl = defVar.resolveOpDecl(ctx.modulePath());
      if (opDecl != null) {
        opSet.bind(self, pred, opDecl, id.sourcePos());
        return defVar;
      }
    }

    // make compiler happy 😥
    throw StmtResolver.resolvingInterrupt(opSet.reporter, new NameProblem.OperatorNameNotFound(id.sourcePos(), id.join()));
  }

  static void resolveBind(@NotNull SeqLike<ResolvingStmt> contents, @NotNull ResolveInfo info) {
    contents.forEach(s -> resolveBind(info.thisModule(), s, info));
    info.opRename().forEach((_, v) -> {
      if (v.bind() == BindBlock.EMPTY) return;
      bind(info.thisModule(), v.bind(), info.opSet(), v.renamed());
    });
  }

  /**
   * @param ctx the context that {@param stmt} binds to
   */
  static void resolveBind(@NotNull Context ctx, @NotNull ResolvingStmt stmt, @NotNull ResolveInfo info) {
    switch (stmt) {
      case TopDecl(TeleDecl.DataDecl decl, var innerCtx) -> {
        decl.body.forEach(ctor -> resolveBind(innerCtx, new MiscDecl(ctor), info));
        visitBind(ctx, decl.ref, decl.bindBlock(), info);
      }
      case TopDecl(TeleDecl.FnDecl decl, _) -> visitBind(ctx, decl.ref, decl.bindBlock(), info);
      case MiscDecl(TeleDecl.DataCon ctor) -> visitBind(ctx, ctor.ref, ctor.bindBlock(), info);
      case TopDecl(TeleDecl.PrimDecl _, _), GenStmt _ -> { }
      case TopDecl _, MiscDecl _ -> Panic.unreachable();
      case ModStmt(_, var stmts) -> resolveBind(stmts, info);
      // case TeleDecl.ClassMember field -> visitBind(field.ref, field.bindBlock(), info);
      // case ClassDecl decl -> {
      //   decl.members.forEach(field -> resolveBind(field, info));
      //   visitBind(decl.ref, decl.bindBlock(), info);
      // }
    }
  }
}
