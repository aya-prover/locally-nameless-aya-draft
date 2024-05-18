// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core;

import org.aya.syntax.core.term.AppTerm;
import org.aya.syntax.core.term.FreeTerm;
import org.aya.syntax.core.term.LamTerm;
import org.aya.syntax.core.term.LocalTerm;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BindTest {
  public static final @NotNull LocalTerm ZERO = new LocalTerm(0);
  public static final @NotNull LocalTerm ONE = new LocalTerm(1);
  public static final @NotNull LocalTerm TWO = new LocalTerm(2);

  @Test public void simpleBind() {
    // λx. λy. x y
    var x = new LocalVar("x", SourcePos.NONE);
    var y = new LocalVar("y", SourcePos.NONE);
    var body = new AppTerm(new FreeTerm(x), new FreeTerm(y));
    // λy. x y => λ. x 0
    var lamYXY = new LamTerm(body.bind(y));
    // λx. λ. x 0 => λ. λ. 1 0
    var lamXYXY = new LamTerm(lamYXY.bind(x));
    var expect = new LamTerm(new LamTerm(new AppTerm(ONE, ZERO)));
    assertEquals(expect, lamXYXY);
  }

  public static <T> @NotNull WithPos<T> of(T data) {
    return new WithPos<>(SourcePos.NONE, data);
  }
}
