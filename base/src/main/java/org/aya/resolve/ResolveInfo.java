// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.resolve;

import kala.collection.mutable.MutableMap;
import org.aya.generic.TyckOrder;
import org.aya.normalize.PrimFactory;
import org.aya.resolve.context.Context;
import org.aya.resolve.context.ModuleContext;
import org.aya.resolve.salt.AyaBinOpSet;
import org.aya.syntax.concrete.stmt.BindBlock;
import org.aya.syntax.concrete.stmt.ModuleName;
import org.aya.syntax.concrete.stmt.Stmt;
import org.aya.syntax.concrete.stmt.UseHide;
import org.aya.syntax.core.repr.AyaShape;
import org.aya.syntax.ref.DefVar;
import org.aya.util.binop.OpDecl;
import org.aya.util.error.SourcePos;
import org.aya.util.terck.MutableGraph;
import org.jetbrains.annotations.Debug;
import org.jetbrains.annotations.NotNull;

@Debug.Renderer(text = "thisModule.moduleName().joinToString(\"::\")")
public record ResolveInfo(
  @NotNull ModuleContext thisModule,
  @NotNull PrimFactory primFactory,
  @NotNull AyaShape.Factory shapeFactory,
  @NotNull AyaBinOpSet opSet,
  @NotNull MutableMap<DefVar<?, ?>, OpRenameInfo> opRename,
  @NotNull MutableMap<ModuleName.Qualified, ImportInfo> imports,
  @NotNull MutableMap<ModuleName.Qualified, UseHide> reExports,
  @NotNull MutableGraph<TyckOrder> depGraph
) {
  public ResolveInfo(
    @NotNull ModuleContext thisModule,
    @NotNull PrimFactory primFactory,
    @NotNull AyaShape.Factory shapeFactory,
    @NotNull AyaBinOpSet opSet
  ) {
    this(thisModule, primFactory, shapeFactory, opSet,
      MutableMap.create(), MutableMap.create(), MutableMap.create(), MutableGraph.create());
  }

  public record ImportInfo(@NotNull ResolveInfo resolveInfo, boolean reExport) { }
  public record OpRenameInfo(
    @NotNull Context bindCtx, @NotNull RenamedOpDecl renamed,
    @NotNull BindBlock bind, boolean reExport
  ) { }

  /**
   * @param definedHere Is this operator renamed in this module, or publicly renamed by upstream?
   * @see #open(ResolveInfo, SourcePos, Stmt.Accessibility)
   */
  public void renameOp(
    @NotNull Context bindCtx, @NotNull DefVar<?, ?> defVar,
    @NotNull RenamedOpDecl renamed, @NotNull BindBlock bind, boolean definedHere
  ) {
    defVar.addOpDeclRename(thisModule.modulePath(), renamed);
    opRename.put(defVar, new OpRenameInfo(bindCtx, renamed, bind, definedHere));
  }

  public void open(@NotNull ResolveInfo other, @NotNull SourcePos sourcePos, @NotNull Stmt.Accessibility acc) {
    // open defined operator and their bindings
    opSet().importBind(other.opSet(), sourcePos);
    // open discovered shapes as well
    shapeFactory().importAll(other.shapeFactory());
    // open renamed operators and their bindings
    other.opRename().forEach((defVar, tuple) -> {
      if (acc == Stmt.Accessibility.Public) {
        // if it is `public open`, make renamed operators transitively accessible by storing
        // them in my `opRename` bc "my importers" cannot access `other.opRename`.
        // see: https://github.com/aya-prover/aya-dev/issues/519
        renameOp(thisModule(), defVar, tuple.renamed, tuple.bind, false);
      } else defVar.addOpDeclRename(thisModule().modulePath(), tuple.renamed);
    });
  }

  @Debug.Renderer(text = "opInfo.name()")
  public record RenamedOpDecl(@NotNull OpInfo opInfo) implements OpDecl { }
}
