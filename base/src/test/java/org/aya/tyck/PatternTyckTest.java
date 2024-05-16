// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import org.aya.TestUtil;
import org.aya.syntax.core.def.FnDef;
import org.aya.syntax.core.term.call.ConCall;
import org.junit.jupiter.api.Test;

import static org.aya.tyck.TyckTest.tyck;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PatternTyckTest {
  @Test public void test0() {
    var result = tyck("""
      open data Nat | O | S Nat

      def infix + (a b: Nat): Nat
      | O, b => b
      | S a', b => S (a' + b)
      
      def foo : Nat => (S O) + (S (S O))
      """);
    assert result.isNotEmpty();

    var foo = (FnDef) result.get(2);
    // It is correct that [nf] is [S (O + (S O))], since this is a WH-Normalizer!!
    var nf = TestUtil.sillyNormalizer().apply(TestUtil.emptyCall(foo));
    var conCall = assertInstanceOf(ConCall.class, nf);
  }

  @Test public void elim0() {
    var result = tyck("""
      open data Nat | O | S Nat
      def lind (a b : Nat) : Nat elim a
      | O => b
      | S a' => S (lind a' b)
      """);
    assertTrue(result.isNotEmpty());
  }
}
