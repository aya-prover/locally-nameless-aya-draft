// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import org.aya.TestUtil;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.core.def.Def;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

public class SillyTycker {
  /**
   * Tyck {@param decls}
   *
   * @return well-typed decls
   */
  public static @NotNull ImmutableSeq<Def> tyck(@NotNull ImmutableSeq<Decl> decls, @NotNull Reporter reporter) {
    var pf = TestUtil.emptyState();
    var wellTyped = MutableList.<Def>create();

    for (var decl : decls) {
      var def = new StmtTycker(reporter)
        .check(decl, new ExprTycker(pf, TestUtil.makeLocalCtx(), TestUtil.makeLocalSubst(), reporter));

      wellTyped.append(def);
    }

    return wellTyped.toImmutableSeq();
  }
}
