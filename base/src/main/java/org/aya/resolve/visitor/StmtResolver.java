// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.resolve.visitor;

import kala.collection.immutable.ImmutableSeq;
import kala.value.MutableValue;
import org.aya.generic.TyckUnit;
import org.aya.resolve.context.Context;
import org.aya.resolve.context.WithCtx;
import org.aya.syntax.concrete.stmt.Stmt;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.term.Term;
import org.aya.util.reporter.Problem;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves expressions inside stmts, after {@link StmtShallowResolver}
 *
 * @author re-xyr, ice1000, kiva
 * @see StmtShallowResolver
 * @see ExprResolver
 */
public interface StmtResolver {
  static void resolveStmt(@NotNull ImmutableSeq<WithCtx<Stmt>> stmt/*, @NotNull ResolveInfo info*/) {
    stmt.forEach(s -> resolveStmt(s/*, info*/));
  }

  /** @apiNote Note that this function MUTATES the stmt if it's a Decl. */
  static void resolveStmt(@NotNull WithCtx<Stmt> stmt/*, @NotNull ResolveInfo info*/) {
    switch (stmt.data()) {
      case Decl decl -> resolveDecl(stmt.map(_ -> decl)/*, info*/);
      // case Command.Module mod -> resolveStmt(mod.contents(), info);
      // case Command cmd -> {}
      // case Generalize variables -> {
      //   assert variables.ctx != null;
      //   var resolver = new ExprResolver(variables.ctx, ExprResolver.RESTRICTIVE);
      //   resolver.enterBody();
      //   variables.type = resolver.apply(variables.type);
      //   addReferences(info, new TyckOrder.Body(variables), resolver);
      // }
    }
  }

  /**
   * Resolve {@param predecl}, where {@code predecl.ctx()} is the context of the body of {@param predecl}
   *
   * @apiNote Note that this function MUTATES the decl
   */
  private static void resolveDecl(@NotNull WithCtx<Decl> predecl/*, @NotNull ResolveInfo info*/) {
    switch (predecl.data()) {
      case TeleDecl.FnDecl decl -> {
        // Generalized works for simple bodies and signatures
        var resolver = resolveDeclSignature(predecl.map(_ -> decl), ExprResolver.LAX/*, info*/);
        decl.body = switch (decl.body) {
          case TeleDecl.BlockBody(var cls) -> {
            // introducing generalized variable is not allowed in clauses, hence we insert them before body resolving
            insertGeneralizedVars(decl, resolver);
            var clausesResolver = resolver.enterClauses();
            var body = new TeleDecl.BlockBody(cls.map(clausesResolver::apply));
            // TODO[hoshino]: How about sharing {resolver#reference} between resolver and clausesResolver?
            resolver.reference().appendAll(clausesResolver.reference());
            yield body;
          }
          case TeleDecl.ExprBody(var expr) -> {
            resolver.enterBody();
            var body = expr.descent(resolver);
            insertGeneralizedVars(decl, resolver);
            yield new TeleDecl.ExprBody(body);
          }
        };
        // addReferences(info, new TyckOrder.Body(decl), resolver);
      }
      case TeleDecl.DataDecl decl -> {
        var resolver = resolveDeclSignature(predecl.map(_ -> decl), ExprResolver.LAX/*, info*/);
        insertGeneralizedVars(decl, resolver);
        resolver.enterBody();
        decl.body.forEach(ctor -> {
          var bodyResolver = resolver.member(decl, ExprResolver.Where.Head);
          var mCtx = MutableValue.create(resolver.ctx());
          // ctor.patterns = ctor.patterns.map(pat -> pat.descent(pattern -> bodyResolver.bind(pattern, mCtx)));
          resolveMemberSignature(ctor, bodyResolver, mCtx);
          // ctor.clauses = bodyResolver.partial(mCtx.get(), ctor.clauses);
          // var head = new TyckOrder.Head(ctor);
          // addReferences(info, head, bodyResolver);
          // addReferences(info, new TyckOrder.Body(ctor), SeqView.of(head));
          // No body no body but you!
        });
        // addReferences(info, new TyckOrder.Body(decl), resolver.reference().view()
        //   .concat(decl.body.map(TyckOrder.Body::new)));
      }
      // case ClassDecl decl -> {
      //   assert decl.ctx != null;
      //   var resolver = new ExprResolver(decl.ctx, ExprResolver.RESTRICTIVE);
      //   resolver.enterHead();
      //   decl.members.forEach(field -> {
      //     var bodyResolver = resolver.member(decl, ExprResolver.Where.Head);
      //     var mCtx = MutableValue.create(resolver.ctx());
      //     resolveMemberSignature(field, bodyResolver, mCtx);
      //     addReferences(info, new TyckOrder.Head(field), bodyResolver.reference().view()
      //       .appended(new TyckOrder.Head(decl)));
      //     bodyResolver.enterBody();
      //     field.body = field.body.map(bodyResolver.enter(mCtx.get()));
      //     addReferences(info, new TyckOrder.Body(field), bodyResolver);
      //   });
      //   addReferences(info, new TyckOrder.Head(decl), resolver.reference().view()
      //     .concat(decl.members.map(TyckOrder.Head::new)));
      // }
      // case TeleDecl.PrimDecl decl -> {
      //   resolveDeclSignature(decl, ExprResolver.RESTRICTIVE, info);
      //   addReferences(info, new TyckOrder.Body(decl), SeqView.empty());
      // }
      // handled in DataDecl and StructDecl
      case TeleDecl.DataCtor ctor -> {}
      // case TeleDecl.ClassMember field -> {}
    }
  }
  private static <T extends TeleDecl<?> & TyckUnit>
  void resolveMemberSignature(T ctor, ExprResolver bodyResolver, MutableValue<@NotNull Context> mCtx) {
    ctor.modifyTelescope(t -> t.map(param -> bodyResolver.bind(param, mCtx)));
    // If changed to method reference, `bodyResolver.enter(mCtx.get())` will be evaluated eagerly
    //  so please don't
    ctor.modifyResult((pos, t) -> bodyResolver.enter(mCtx.get()).apply(pos, t));
  }

  // private static void addReferences(@NotNull ResolveInfo info, TyckOrder decl, SeqView<TyckOrder> refs) {
  //   info.depGraph().sucMut(decl).appendAll(refs
  //     .filter(unit -> unit.unit().needTyck(info.thisModule().modulePath().path())));
  //   if (decl instanceof TyckOrder.Body) info.depGraph().sucMut(decl)
  //     .append(new TyckOrder.Head(decl.unit()));
  // }
  //
  // /** @param decl is unmodified */
  // private static void addReferences(@NotNull ResolveInfo info, TyckOrder decl, ExprResolver resolver) {
  //   addReferences(info, decl, resolver.reference().view());
  // }

  private static @NotNull ExprResolver resolveDeclSignature(
    @NotNull WithCtx<TeleDecl.TopLevel<?>> decl,
    ExprResolver.@NotNull Options options
    // , @NotNull ResolveInfo info
  ) {
    var resolver = new ExprResolver(decl.ctx(), options);
    resolver.enterHead();
    var mCtx = MutableValue.create(decl.ctx());
    var telescope = decl.data().telescope.map(param -> resolver.bind(param, mCtx));
    var newResolver = resolver.enter(mCtx.get());
    decl.data().modifyResult(newResolver);
    decl.data().telescope = telescope;
    // addReferences(info, new TyckOrder.Head(decl), resolver);
    return newResolver;
  }

  private static <RetTy extends Term> void insertGeneralizedVars(
    @NotNull TeleDecl<RetTy> decl,
    @NotNull ExprResolver resolver
  ) {
    // decl.telescope = decl.telescope.prependedAll(resolver.allowedGeneralizes().valuesView());
  }
/*

  static void visitBind(@NotNull DefVar<?, ?> selfDef, @NotNull BindBlock bind, @NotNull ResolveInfo info) {
    var opSet = info.opSet();
    var self = selfDef.opDecl;
    if (self == null && bind != BindBlock.EMPTY) {
      opSet.reporter.report(new OperatorError.BadBindBlock(selfDef.concrete.sourcePos(), selfDef.name()));
      throw new Context.ResolvingInterruptedException();
    }
    bind(bind, opSet, self);
  }

  private static void bind(@NotNull BindBlock bindBlock, AyaBinOpSet opSet, OpDecl self) {
    if (bindBlock == BindBlock.EMPTY) return;
    var ctx = bindBlock.context().get();
    assert ctx != null : "no shallow resolver?";
    bindBlock.resolvedLoosers().set(bindBlock.loosers().map(looser -> bind(self, opSet, ctx, OpDecl.BindPred.Looser, looser)));
    bindBlock.resolvedTighters().set(bindBlock.tighters().map(tighter -> bind(self, opSet, ctx, OpDecl.BindPred.Tighter, tighter)));
  }

  private static @NotNull DefVar<?, ?> bind(
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
    throw resolvingInterrupt(opSet.reporter, new NameProblem.OperatorNameNotFound(id.sourcePos(), id.join()));
  }

  static void resolveBind(@NotNull SeqLike<@NotNull Stmt> contents, @NotNull ResolveInfo info) {
    contents.forEach(s -> resolveBind(s, info));
    info.opRename().forEach((k, v) -> {
      if (v.component2() == BindBlock.EMPTY) return;
      bind(v.component2(), info.opSet(), v.component1());
    });
  }

  static void resolveBind(@NotNull Stmt stmt, @NotNull ResolveInfo info) {
    switch (stmt) {
      case Command.Module mod -> resolveBind(mod.contents(), info);
      case ClassDecl decl -> {
        decl.members.forEach(field -> resolveBind(field, info));
        visitBind(decl.ref, decl.bindBlock(), info);
      }
      case TeleDecl.DataDecl decl -> {
        decl.body.forEach(ctor -> resolveBind(ctor, info));
        visitBind(decl.ref, decl.bindBlock(), info);
      }
      case TeleDecl.DataCtor ctor -> visitBind(ctor.ref, ctor.bindBlock(), info);
      case TeleDecl.ClassMember field -> visitBind(field.ref, field.bindBlock(), info);
      case TeleDecl.FnDecl decl -> visitBind(decl.ref, decl.bindBlock(), info);
      case TeleDecl.PrimDecl decl -> {}
      case Command cmd -> {}
      case Generalize generalize -> {}
    }
  }
*/

  @Contract("_, _ -> fail")
  static Context.ResolvingInterruptedException resolvingInterrupt(Reporter reporter, Problem problem) {
    reporter.report(problem);
    throw new Context.ResolvingInterruptedException();
  }
}
