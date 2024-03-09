// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.Def;
import org.aya.syntax.core.def.Signature;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.LocalVar;
import org.aya.tyck.tycker.Problematic;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

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
      case TeleDecl.DataDecl data -> {}
      case TeleDecl.FnDecl fn -> {
        fn.signature = checkTele(fn.telescope, fn.result, tycker);
      }
    }
  }

  /**
   * @return the variables will remain free in the result,
   * because later they will be used in the body of the type checking.
   */
  private Signature<Term> checkTele(ImmutableSeq<Expr.Param> telescope, Expr result, ExprTycker tycker) {
    var names = MutableList.<LocalVar>create();
    var tele = telescope.map(p -> {
      var pTy = tycker.ty(p.type());
      tycker.localCtx().put(p.ref(), pTy);
      names.append(p.ref());
      return new Param(p.ref().name(), pTy, p.explicit());
    });
    // TODO: check result, turn into indices
    throw new UnsupportedOperationException("TODO");
  }

  private void loadTele(Signature<?> signature, ExprTycker tycker) {
    var names = MutableList.<LocalVar>create();
    signature.param().forEach(param -> {
      var name = LocalVar.make(param.data().name(), param.sourcePos());
      tycker.localCtx().put(name, param.data().type().instantiateAllVars(names.view().reversed()));
      names.append(name);
    });
  }
}
