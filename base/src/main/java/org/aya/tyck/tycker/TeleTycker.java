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
    @NotNull ImmutableSeq<Expr.Param> cTele,
    @NotNull WithPos<Expr> result, @NotNull ExprTycker tycker
  ) {
    var locals = cTele.view().map(Expr.Param::ref).toImmutableSeq();
    var finalParam = checkTele(cTele, tycker);
    var finalResult = bindResult(tycker.ty(result), locals);
    tycker.solveMetas();
    // TODO: zonk these data
    return new Signature<>(finalParam, finalResult);
  }

  static @NotNull ImmutableSeq<WithPos<Param>> checkTele(
    @NotNull ImmutableSeq<Expr.Param> cTele,
    @NotNull ExprTycker tycker
  ) {
    var tele = checkTeleFree(cTele, tycker);
    var locals = cTele.view().map(Expr.Param::ref).toImmutableSeq();
    bindTele(locals, tele);
    return tele.zipView(cTele)
      .map(p -> new WithPos<>(p.component2().sourcePos(), p.component1()))
      .toImmutableSeq();
  }

  /**
   * Check the tele with free variables remaining in the localCtx.
   * Does not zonk!
   */
  @Contract(pure = true)
  static @NotNull MutableSeq<Param> checkTeleFree(ImmutableSeq<Expr.Param> cTele, ExprTycker tycker) {
    return MutableSeq.from(cTele.view().map(p -> {
      var pTy = tycker.ty(p.typeExpr());
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

  /**
   * Replace {@link org.aya.syntax.core.term.FreeTerm} in {@param tele} with appropriate index
   */
  static void bindTele(ImmutableSeq<LocalVar> binds, MutableSeq<Param> tele) {
    final var lastIndex = tele.size() - 1;
    // fix some param, say [p]
    for (int i = lastIndex - 1; i >= 0; i--) {
      var p = binds.get(i);
      // for any other param that is able to refer to [p]
      for (int j = i + 1; j < tele.size(); j++) {
        var og = tele.get(j);
        // j - i is the human distance between [p] and [og]. However, we count from 0
        int ii = i, jj = j;
        tele.set(j, og.map(x -> x.bindAt(p, jj - ii - 1)));
      }
    }
  }

  /**
   * Load a well-typed {@link Signature} into {@link ExprTycker#localCtx()}
   */
  @Contract(mutates = "param3")
  static void loadTele(
    @NotNull ImmutableSeq<LocalVar> binds,
    @NotNull Signature<?> signature,
    @NotNull ExprTycker tycker) {
    assert binds.sizeEquals(signature.param());
    var tele = MutableList.<LocalVar>create();

    binds.view().zip(signature.param()).forEach(pair -> {
      var ref = pair.component1();
      var param = pair.component2();
      tycker.localCtx().put(ref, param.data().type().instantiateTeleVar(tele.view()));
      tele.append(ref);
    });
  }
}
