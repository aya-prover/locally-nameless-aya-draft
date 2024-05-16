// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.resolve.visitor;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import org.aya.resolve.ResolveInfo;
import org.aya.resolve.ResolvingStmt;
import org.aya.resolve.context.ModuleContext;
import org.aya.resolve.context.NoExportContext;
import org.aya.resolve.context.PhysicalModuleContext;
import org.aya.resolve.error.PrimResolveError;
import org.aya.syntax.concrete.stmt.*;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.PrimDef;
import org.aya.util.binop.Assoc;
import org.aya.util.binop.OpDecl;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * TODO: fix commented code
 * simply adds all top-level names to the context
 *
 * @author re-xyr
 */
public record StmtPreResolver(/*@NotNull ModuleLoader loader, */ @NotNull ResolveInfo resolveInfo) {
  /**
   * Resolve {@link Stmt}s under {@param context}.
   *
   * @return the context of the body of each {@link Stmt}, where imports and opens are stripped.
   */
  public ImmutableSeq<ResolvingStmt> resolveStmt(@NotNull ImmutableSeq<Stmt> stmts, ModuleContext context) {
    return stmts.mapNotNull(stmt -> resolveStmt(stmt, context));
  }

  public @Nullable ResolvingStmt resolveStmt(@NotNull Stmt stmt, @NotNull ModuleContext context) {
    return switch (stmt) {
      case Decl decl -> resolveDecl(decl, context);
      case Command.Module mod -> {
        var wholeModeName = context.modulePath().derive(mod.name());
        // Is there a file level module with path {context.moduleName}::{mod.name} ?
        // if (loader.existsFileLevelModule(wholeModeName.path())) {
        //   context.reportAndThrow(new NameProblem.ClashModNameError(wholeModeName.path(), mod.sourcePos()));
        // }
        var newCtx = context.derive(mod.name());
        var children = resolveStmt(mod.contents(), newCtx);
        context.importModule(ModuleName.This.resolve(mod.name()), newCtx, mod.accessibility(), mod.sourcePos());
        yield new ResolvingStmt.ModStmt(mod, children);
      }
      case Command.Import cmd -> {
        var modulePath = cmd.path();
        throw new UnsupportedOperationException("TODO");
        // var success = loader.load(modulePath.path());
        // if (success == null)
        //   context.reportAndThrow(new NameProblem.ModNotFoundError(modulePath, cmd.sourcePos()));
        // var mod = success.thisModule();
        // var as = cmd.asName();
        // var importedName = as != null ? ModuleName.This.resolve(as) : modulePath.asName();
        // context.importModule(importedName, mod, cmd.accessibility(), cmd.sourcePos());
        // resolveInfo.imports().put(importedName, Tuple.of(success, cmd.accessibility() == Stmt.Accessibility.Public));
      }
      case Command.Open cmd -> {
        var mod = cmd.path();
        var acc = cmd.accessibility();
        var useHide = cmd.useHide();
        var ctx = cmd.openExample() ? exampleContext(context) : context;
        ctx.openModule(mod, acc, cmd.sourcePos(), useHide);
        // open necessities from imported modules (not submodules)
        // because the module itself and its submodules share the same ResolveInfo
        resolveInfo.imports().getOption(mod).ifDefined(modResolveInfo -> {
          if (acc == Stmt.Accessibility.Public) resolveInfo.reExports().put(mod, useHide);
          resolveInfo.open(modResolveInfo.resolveInfo(), cmd.sourcePos(), acc);
        });
        // renaming as infix
        if (useHide.strategy() == UseHide.Strategy.Using) useHide.list().forEach(use -> {
          if (use.asAssoc() == Assoc.Invalid) return;
          var symbol = ctx.modules().get(mod).symbols().getMaybe(use.id().component(), use.id().name());
          assert symbol.isOk(); // checked in openModule
          var asName = use.asName().getOrDefault(use.id().name());
          var renamedOpDecl = new ResolveInfo.RenamedOpDecl(new OpDecl.OpInfo(asName, use.asAssoc()));
          var bind = use.asBind();
          if (bind != BindBlock.EMPTY) {
            // bind.context().set(ctx);
            // TODO[ice]: is this a no-op?
            throw new UnsupportedOperationException("TODO");
          }
          resolveInfo.renameOp(symbol.get(), renamedOpDecl, bind, true);
        });
        yield null;
      }
      case Generalize variables -> {
        for (var variable : variables.variables)
          context.defineSymbol(variable, Stmt.Accessibility.Private, variable.sourcePos);
        yield new ResolvingStmt.GenStmt(variables);
      }
    };
  }

  private @NotNull ResolvingStmt resolveDecl(@NotNull Decl predecl, @NotNull ModuleContext context) {
    return switch (predecl) {
      case TeleDecl.DataDecl decl -> {
        var ctx = resolveTopLevelDecl(decl, context);
        var innerCtx = resolveChildren(decl, ctx, d -> d.body.view(), (ctor, mCtx) -> {
          ctor.ref().module = mCtx.modulePath();
          ctor.ref().fileModule = resolveInfo.thisModule().modulePath();
          mCtx.defineSymbol(ctor.ref(), Stmt.Accessibility.Public, ctor.sourcePos());
          resolveOpInfo(ctor);
        });
        resolveOpInfo(decl);
        yield new ResolvingStmt.TopDecl(decl, innerCtx);
      }
      // case ClassDecl decl -> {
      //   var ctx = resolveTopLevelDecl(decl, context);
      //   var innerCtx = resolveChildren(decl, decl, ctx, s -> s.members.view(), (field, mockCtx) -> {
      //     field.ref().module = mockCtx.modulePath().path();
      //     field.ref().fileModule = resolveInfo.thisModule().modulePath().path();
      //     mockCtx.defineSymbol(field.ref, Stmt.Accessibility.Public, field.sourcePos());
      //     resolveOpInfo(field, mockCtx);
      //   });
      //   resolveOpInfo(decl, innerCtx);
      // }
      case TeleDecl.FnDecl decl -> {
        var innerCtx = resolveTopLevelDecl(decl, context);
        resolveOpInfo(decl);
        yield new ResolvingStmt.TopDecl(decl, innerCtx);
      }
      case TeleDecl.PrimDecl decl -> {
        var factory = resolveInfo.primFactory();
        var name = decl.ref.name();
        var sourcePos = decl.sourcePos();
        var primID = PrimDef.ID.find(name);
        if (primID == null) context.reportAndThrow(new PrimResolveError.UnknownPrim(sourcePos, name));
        var lack = factory.checkDependency(primID);
        if (lack.isNotEmpty() && lack.get().isNotEmpty())
          context.reportAndThrow(new PrimResolveError.Dependency(name, lack.get(), sourcePos));
        else if (factory.have(primID) && !factory.suppressRedefinition())
          context.reportAndThrow(new PrimResolveError.Redefinition(name, sourcePos));
        factory.factory(primID, decl.ref);
        var innerCtx = resolveTopLevelDecl(decl, context);
        resolveOpInfo(decl);
        yield new ResolvingStmt.TopDecl(decl, innerCtx);
      }
      default -> Panic.unreachable();
    };
  }

  private <D extends Decl, Child extends Decl> PhysicalModuleContext resolveChildren(
    @NotNull D decl,
    @NotNull ModuleContext context,
    @NotNull Function<D, SeqView<Child>> childrenGet,
    @NotNull BiConsumer<Child, ModuleContext> childResolver
  ) {
    var innerCtx = context.derive(decl.ref().name());
    childrenGet.apply(decl).forEach(child -> childResolver.accept(child, innerCtx));
    var module = decl.ref().name();
    context.importModule(
      ModuleName.This.resolve(module),
      innerCtx.exports,
      decl.accessibility(),
      decl.sourcePos()
    );
    return innerCtx;
  }

  private void resolveOpInfo(@NotNull Decl decl) {
    if (decl.opInfo() != null) {
      var ref = decl.ref();
      ref.opDecl = decl;
    }
  }

  private @NotNull NoExportContext exampleContext(@NotNull ModuleContext context) {
    return context instanceof PhysicalModuleContext physical ? physical.exampleContext() : Panic.unreachable();
  }

  private <D extends Decl> @NotNull ModuleContext
  resolveTopLevelDecl(@NotNull D decl, @NotNull ModuleContext context) {
    decl.ref().module = context.modulePath();
    decl.ref().fileModule = resolveInfo.thisModule().modulePath();
    context.defineSymbol(decl.ref(), decl.accessibility(), decl.sourcePos());
    return context;
  }
}
