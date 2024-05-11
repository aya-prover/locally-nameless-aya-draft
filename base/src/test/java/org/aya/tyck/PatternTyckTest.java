// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.aya.tyck.TyckTest.tyck;

public class PatternTyckTest {
  @Test public void test0() {
    @Language("Aya") String code = """
      data Nat | O | S Nat

      def infix + (a b: Nat): Nat
      | Nat::O, b => b
      | Nat::S a', b => Nat::S (a' + b)
      """;
    var result = tyck(code);
  }
}
