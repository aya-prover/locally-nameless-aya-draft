package org.aya.syntax.core;

import kala.collection.mutable.MutableMap;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.core.term.AppTerm;
import org.aya.syntax.core.term.FreeTerm;
import org.aya.syntax.core.term.LamTerm;
import org.aya.syntax.core.term.LocalTerm;
import org.aya.syntax.ref.LocalCtx;
import org.aya.syntax.ref.LocalVar;
import org.aya.tyck.ExprTycker;
import org.aya.util.Arg;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BindTest {
  @Test public void simpleBind() {
    // λx. λy. x y
    var x = new LocalVar("x", SourcePos.NONE);
    var y = new LocalVar("y", SourcePos.NONE);
    var body = new AppTerm(new FreeTerm(x), Arg.ofExplicitly(new FreeTerm(y)));
    // λy. x y => λ. x 0
    var lamYXY = new LamTerm(true, body.bind(y));
    // λx. λ. x 0 => λ. λ. 1 0
    var lamXYXY = new LamTerm(true, lamYXY.bind(x));
    var expect = new LamTerm(true, new LamTerm(true, new AppTerm(new LocalTerm(1), Arg.ofExplicitly(new LocalTerm(0)))));
    assertEquals(expect, lamXYXY);
  }


  @Test
  public void tyckLam() {
    var x = new LocalVar("x", SourcePos.NONE);
    var y = new LocalVar("y", SourcePos.NONE);
    // x y
    var body = new Expr.App(new WithPos<>(SourcePos.NONE, new Expr.Ref(x)), new Expr.NamedArg(SourcePos.NONE, true, null, new WithPos<>(SourcePos.NONE, new Expr.Ref(y))));
    // λ y. x y
    var lamYXY = new Expr.Lambda(new Expr.Param(SourcePos.NONE, y, true), new WithPos<>(SourcePos.NONE, body));
    var lamXYXY = new Expr.Lambda(new Expr.Param(SourcePos.NONE, x, true), new WithPos<>(SourcePos.NONE, lamYXY));
    var tycker = new ExprTycker(null, new LocalCtx(MutableMap.create(), null), null);
    var result = tycker.synthesize(lamXYXY);
    return;
  }
}
