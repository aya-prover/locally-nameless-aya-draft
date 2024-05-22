// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.concrete.stmt.decl;

import org.aya.generic.stmt.TyckUnit;
import org.aya.syntax.concrete.stmt.BindBlock;
import org.aya.syntax.concrete.stmt.Stmt;
import org.aya.syntax.ref.DefVar;
import org.aya.util.binop.OpDecl;
import org.aya.util.error.SourceNode;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Generic concrete definitions, corresponding to {@link org.aya.syntax.core.def.Def}.
 *
 * @author kiva, zaoqi
 * @see TeleDecl
 * @see ClassDecl
 */
public sealed interface Decl extends SourceNode, Stmt, TyckUnit, OpDecl permits TeleDecl {
  @Contract(pure = true) @NotNull DefVar<?, ?> ref();
  @Contract(pure = true) @NotNull DeclInfo info();
  default @NotNull BindBlock bindBlock() { return info().bindBlock(); }
  default @NotNull SourcePos entireSourcePos() { return info().entireSourcePos(); }
  @Override default @NotNull SourcePos sourcePos() { return info().sourcePos(); }
  @Override default @NotNull Stmt.Accessibility accessibility() { return info().accessibility(); }
  @Override default @Nullable OpDecl.OpInfo opInfo() { return info().opInfo(); }
}
