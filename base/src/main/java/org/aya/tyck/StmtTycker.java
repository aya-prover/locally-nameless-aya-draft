// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.Def;
import org.aya.syntax.core.def.Signature;
import org.aya.syntax.core.term.SortTerm;
import org.aya.tyck.tycker.Problematic;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

import static org.aya.tyck.tycker.TeleTycker.checkTele;
import static org.aya.tyck.tycker.TeleTycker.loadTele;

public record StmtTycker(@NotNull Reporter reporter) implements Problematic {
  private @NotNull Def check(Decl predecl, ExprTycker tycker) {
    if (predecl instanceof TeleDecl<?> decl) {
      if (decl.signature != null) loadTele(decl.signature, tycker);
      else checkHeader(decl, tycker);
    }
    throw new UnsupportedOperationException("TODO");
  }

  private void checkHeader(TeleDecl<?> decl, ExprTycker tycker) {
    switch (decl) {
      case TeleDecl.DataCtor con -> {}
      case TeleDecl.DataDecl data -> {
        var signature = checkTele(data.telescope, data.result, tycker);
        SortTerm sort = SortTerm.Type0;
        if (signature.result() instanceof SortTerm userSort) {
          sort = userSort;
        } else {
          // TODO: report
        }
        data.signature = new Signature<>(signature.param(), sort);
      }
      case TeleDecl.FnDecl fn -> fn.signature = checkTele(fn.telescope, fn.result, tycker);
    }
  }
}
