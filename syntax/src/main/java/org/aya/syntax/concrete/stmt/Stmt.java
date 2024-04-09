// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.concrete.stmt;

import org.aya.generic.TyckUnit;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.concrete.Pattern;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.util.PosedUnaryOperator;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * @author kiva
 */
public sealed interface Stmt extends TyckUnit permits Decl {
  /** @apiNote the \import stmts do not have a meaningful accessibility, do not refer to this in those cases */
  @Contract(pure = true) @NotNull Accessibility accessibility();

  /**
   * @author re-xyr
   */
  enum Accessibility {
    Private("private"),
    Public("public");
    public final @NotNull String keyword;

    Accessibility(@NotNull String keyword) {
      this.keyword = keyword;
    }
  }

  @Contract(mutates = "this")
  void descentInPlace(@NotNull PosedUnaryOperator<Expr> f);
}
