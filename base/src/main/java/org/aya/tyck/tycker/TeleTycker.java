// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.tycker;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableSeq;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.core.def.Signature;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.LocalVar;
import org.aya.tyck.ExprTycker;
import org.aya.util.error.WithPos;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Type-checking code for telescopes.
 */
public interface TeleTycker {
  /**
   * @param tycker the localCtx inside will have types where the bindings are free.
   * @return a locally nameless signature computed from what's in the localCtx.
   */
  @Contract(pure = true)
  static @NotNull Signature<Term> checkTele(
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
  static @NotNull MutableSeq<Param> checkTeleFree(ImmutableSeq<Expr.Param> cTele, ExprTycker tycker) {
    return MutableSeq.from(cTele.view().map(p -> {
      var pTy = tycker.ty(p.type());
      tycker.localCtx().put(p.ref(), pTy);
      return new Param(p.ref().name(), pTy, p.explicit());
    }));
  }

  static @NotNull Term bindResult(@NotNull Term result, ImmutableSeq<LocalVar> locals) {
    final var lastIndex = locals.size() - 1;
    for (int i = lastIndex; i >= 0; i--) {
      result = result.bindAt(locals.get(i), lastIndex - i);
    }
    return result;
  }

  static void bindTele(ImmutableSeq<LocalVar> locals, MutableSeq<Param> tele) {
    final var lastIndex = tele.size() - 1;
    for (int i = lastIndex; i >= 0; i--) {
      for (int j = i + 1; j < tele.size(); j++) {
        var og = tele.get(j);
        tele.set(j, og.bindAt(locals.get(i), j - i - 1));
      }
    }
  }

  @Contract(mutates = "param2")
  static void loadTele(Signature<?> signature, ExprTycker tycker) {
    var names = MutableList.<LocalVar>create();
    signature.param().forEach(param -> {
      var name = LocalVar.make(param.data().name(), param.sourcePos());
      tycker.localCtx().put(name, param.data().type().instantiateAllVars(names.view().reversed()));
      names.append(name);
    });
  }
}
