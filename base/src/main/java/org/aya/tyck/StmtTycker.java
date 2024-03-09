// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.Def;
import org.aya.syntax.core.def.Signature;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

public record StmtTycker(@NotNull Reporter reporter) {
  private @NotNull Def check(Decl predecl, ExprTycker tycker) {
    if (predecl instanceof TeleDecl<?> decl) {
      if (decl.signature != null) loadTele(decl.signature, tycker);
      else if (decl.ref().core == null) checkHeader(decl, tycker);
    }
    throw new UnsupportedOperationException("TODO");
  }

  private void checkHeader(TeleDecl<?> decl, ExprTycker tycker) {
    switch (decl) {
      case TeleDecl.DataCtor con -> {}
      case TeleDecl.DataDecl data -> {}
      case TeleDecl.FnDecl fn -> {}
    }
  }

  private void loadTele(Signature<?> signature, ExprTycker tycker) {
    signature.param().forEach(p -> tycker.localCtx().put(
        LocalVar.make(p.data().name(), p.sourcePos()), p.data().type()));
  }
}
