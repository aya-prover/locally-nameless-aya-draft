// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.aya.tyck.TyckTest.tyck;

public class PatternTyckTest {
  @Test public void test0() {
    var result = tyck("""
      open data Nat | O | S Nat

      def infix + (a b: Nat): Nat
      | O, b => b
      | S a', b => S (a' + b)
      """);
    assert result.isNotEmpty();
  }

  @Test
  public void elim0() {
    var result = tyck("""
      open data Nat | O | S Nat
      def lind (a b : Nat) : Nat elim a
      | O => b
      | S a' => S (lind a' b)
      """);
    assert result.isNotEmpty();
  }
}
