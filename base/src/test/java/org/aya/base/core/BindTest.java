package org.aya.base.core;

import org.aya.base.generic.LocalVar;
import org.aya.util.error.SourcePos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BindTest {
  @Test public void simpleBind() {
    // λx. λy. x y
    var x = new LocalVar("x", SourcePos.NONE);
    var y = new LocalVar("y", SourcePos.NONE);
    var body = new AppTerm(new FreeTerm(x), new FreeTerm(y));
    var lamYXY = new LamTerm(body.bind(y));
    var lamXYXY = new LamTerm(lamYXY.bind(x));
    // λ. λ. 1 0
    var expect = new LamTerm(new LamTerm(new AppTerm(new LocalTerm(1), new LocalTerm(0))));
    assertEquals(expect, lamXYXY);
  }
}
