// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableSeq;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.Def;
import org.aya.syntax.core.def.Signature;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.SortTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.LocalVar;
import org.aya.tyck.tycker.Problematic;
import org.aya.util.error.WithPos;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.Contract;
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

  /**
   * @param tycker the localCtx inside will have types where the bindings are free.
   * @return a locally nameless signature computed from what's in the localCtx.
   */
  @Contract(pure = true)
  private @NotNull Signature<Term> checkTele(
    ImmutableSeq<Expr.Param> cTele,
    WithPos<Expr> cResult, ExprTycker tycker
  ) {
    var tele = checkTeleFree(cTele, tycker);
    var locals = cTele.view().map(Expr.Param::ref).toImmutableSeq();
    bindTele(locals, tele);
    var result = bindResult(tycker.ty(cResult), locals);
    var finalParam = tele.zipView(cTele)
      .map(p -> new WithPos<>(p.component2().sourcePos(), p.component1()))
      .toImmutableSeq();
    return new Signature<>(finalParam, result);
  }

  /**
   * Check the tele with free variables remaining in the localCtx.
   */
  @Contract(pure = true)
  private static @NotNull MutableSeq<Param> checkTeleFree(ImmutableSeq<Expr.Param> cTele, ExprTycker tycker) {
    return MutableSeq.from(cTele.view().map(p -> {
      var pTy = tycker.ty(p.type());
      tycker.localCtx().put(p.ref(), pTy);
      return new Param(p.ref().name(), pTy, p.explicit());
    }));
  }

  private static @NotNull Term bindResult(@NotNull Term result, ImmutableSeq<LocalVar> locals) {
    final var lastIndex = locals.size() - 1;
    for (int i = lastIndex; i >= 0; i--) {
      result = result.bindAt(locals.get(i), lastIndex - i);
    }
    return result;
  }

  private static void bindTele(ImmutableSeq<LocalVar> locals, MutableSeq<Param> tele) {
    final var lastIndex = tele.size() - 1;
    for (int i = lastIndex; i >= 0; i--) {
      for (int j = i + 1; j < tele.size(); j++) {
        var og = tele.get(j);
        tele.set(j, og.bindAt(locals.get(i), j - i - 1));
      }
    }
  }

  @Contract(mutates = "param2")
  private void loadTele(Signature<?> signature, ExprTycker tycker) {
    var names = MutableList.<LocalVar>create();
    signature.param().forEach(param -> {
      var name = LocalVar.make(param.data().name(), param.sourcePos());
      tycker.localCtx().put(name, param.data().type().instantiateAllVars(names.view().reversed()));
      names.append(name);
    });
  }
}
