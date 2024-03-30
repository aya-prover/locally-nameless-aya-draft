// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.concrete;

import org.aya.syntax.SyntaxTestUtil;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

public class SyntaxTest {
  @Test
  public void test0() {
    @Language("Aya")
    var code = """
      def foo (f : Type -> Type 0) (a : Type 0) : Type 0 => f a
      """;

    var decl = (Decl) SyntaxTestUtil.parse(code).getFirst();
    SyntaxTestUtil.resolve(decl);
    return;
  }
}
