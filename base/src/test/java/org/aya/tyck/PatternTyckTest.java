// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.collection.immutable.ImmutableSeq;
import org.aya.TestUtil;
import org.aya.normalize.Normalizer;
import org.aya.syntax.core.def.DataDef;
import org.aya.syntax.core.def.FnDef;
import org.aya.syntax.core.term.call.ConCall;
import org.aya.syntax.core.term.call.FnCall;
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
            
      def foo : Nat => (S O) + (S (S O))
      """);
    assert result.isNotEmpty();

    var foo = (FnDef) result.get(2);
    var nf = new Normalizer(TestUtil.emptyState()).apply(new FnCall(foo.ref, 0, ImmutableSeq.empty()));
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
