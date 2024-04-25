// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core;

import kala.collection.immutable.ImmutableSeq;
import org.aya.TestUtil;
import org.aya.normalize.Normalizer;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.core.term.AppTerm;
import org.aya.syntax.core.term.FreeTerm;
import org.aya.syntax.core.term.LamTerm;
import org.aya.syntax.core.term.LocalTerm;
import org.aya.syntax.ref.LocalVar;
import org.aya.tyck.ExprTycker;
import org.aya.tyck.TyckState;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BindTest {
  @Test public void simpleBind() {
    // λx. λy. x y
    var x = new LocalVar("x", SourcePos.NONE);
    var y = new LocalVar("y", SourcePos.NONE);
    var body = new AppTerm(new FreeTerm(x), new FreeTerm(y));
    // λy. x y => λ. x 0
    var lamYXY = new LamTerm(body.bind(y));
    // λx. λ. x 0 => λ. λ. 1 0
    var lamXYXY = new LamTerm(lamYXY.bind(x));
    var expect = new LamTerm(new LamTerm(new AppTerm(new LocalTerm(1), new LocalTerm(0))));
    assertEquals(expect, lamXYXY);
  }

  @Test public void testTyckLam() {
    var x = new LocalVar("x", SourcePos.NONE);
    var y = new LocalVar("y", SourcePos.NONE);
    var ty = new Expr.Type(0);
    var pi = new Expr.Pi(new Expr.Param(SourcePos.NONE, LocalVar.IGNORED, of(ty), true), of(ty));   // Type -> Type
    var refX = new Expr.Ref(x);
    var refY = new Expr.Ref(y);
    var XY = new Expr.App(of(refX), ImmutableSeq.of(
      new Expr.NamedArg(true, null, of(refY))));
    var YXY = new Expr.Lambda(new Expr.Param(SourcePos.NONE, y, of(ty), true), of(XY));
    var XYXY = new Expr.Lambda(new Expr.Param(SourcePos.NONE, x, of(pi), true), of(YXY));

    var tycker = new ExprTycker(new TyckState(), TestUtil.makeLocalCtx(), TestUtil.makeDBLocalCtx(), TestUtil.THROWING);
    var result = tycker.synthesize(of(XYXY));
  }

  @Test
  public void testIce1000() {
    // (\ 0 1) (\ \ 1)
    var f = new LamTerm(new AppTerm(new LocalTerm(0), new LocalTerm(1)));
    var a = new LamTerm(new LamTerm(new LocalTerm(1)));
    var app = new AppTerm(f, a);
    //     \a. (\b. b a) (\c d. c)
    // --> \a. (\c d. c) a
    // --> \a. (\d. a)
    //     (\ 0 1) (\ \ 1)
    // --> (\ \ 1) 0
    // --> (\ 1)    (replace 1 with 0, but the context where 1 lives has an binding, so increase 0)
    var result = new Normalizer(new TyckState()).whnf(app);
  }

  public static <T> @NotNull WithPos<T> of(T data) {
    return new WithPos<>(SourcePos.NONE, data);
  }
}
