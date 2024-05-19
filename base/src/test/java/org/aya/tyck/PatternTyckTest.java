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
      | 0, b => b
      | S a', b => S (a' + b)
      
      def foo : Nat => (S O) + (S (S O))
      """);
    assert result.isNotEmpty();

    var foo = (FnDef) result.find(d -> d.ref().name().equals("foo")).get();
    // It is correct that [nf] is [S (O + (S O))], since this is a WH-Normalizer!!
    var nf = TestUtil.sillyNormalizer().apply(TestUtil.emptyCall(foo));
    var conCall = assertInstanceOf(ConCall.class, nf);
  }

  @Test public void elim0() {
    var result = tyck("""
      open data Nat | O | S Nat
      def lind (a b : Nat) : Nat elim a
      | 0 => b
      | S a' => S (lind a' b)
      """);
    assertTrue(result.isNotEmpty());
  }

  @Test public void test1() {
    var result = tyck("""
      open data Nat | O | S Nat
      open data Vec Nat Type
      | 0, A => vnil
      | S n, A => infixr vcons A (Vec n A)
      
      def length (A : Type) (n : Nat) (v : Vec n A) : Nat elim v
      | vnil => 0
      | _ vcons xs => S (length _ _ xs)
      
      def threeTimesAhThreeTimes (A : Type) (a : A) : Vec 3 A =>
        a vcons a vcons a vcons vnil
      
      def head (A : Type) (n : Nat) (v : Vec (S n) A) : A elim v
      | x vcons _ => x
      
      def unwrap (A : Type) (v : Vec 1 A) : A elim v
      | x vcons vnil => x
      """);

    assertTrue(result.isNotEmpty());
  }

  @Test public void test2() {
    var result = tyck("""
      open data Nat | O | S Nat

      prim I : ISet
      prim Path (A : I -> Type) (a : A 0) (b : A 1) : Type
      variable A B : Type
      def infix = (a b : A) : Type => Path (\\i => A) a b
      def refl {a : A} : a = a => \\i => a
      def pmap (f : A -> B) {a b : A} (p : a = b) : f a = f b => \\i => f (p i)

      overlap def infix + (a b: Nat): Nat
      | 0, b => b
      | a, 0 => a
      | S a, b => S (a + b)
      | a, S b => S (a + b)
      tighter =

      overlap def +-comm (a b: Nat): a + b = b + a
      | 0, _ => refl
      | _, 0 => refl
      | S a, b => pmap (\\n => S n) (+-comm a b)
      | a, S b => pmap (\\n => S n) (+-comm a b)
      """);
    assertTrue(result.isNotEmpty());
  }

  @Test public void test3() {
    var result = tyck("""
      open data Nat | O | S Nat

      prim I : ISet
      prim Path (A : I -> Type) (a : A 0) (b : A 1) : Type
      variable A B : Type
      def infix = (a b : A) => Path (\\i => A) a b
      def refl {a : A} : a = a => \\i => a

      overlap def infix +' (a b: Nat): Nat
      | 0, b => b
      | a, 0 => a
      | S a, b => S (a +' b)
      | a, S b => S (a +' b)
      tighter =

      open data Int
      | pos Nat | neg Nat
      | zro : pos 0 = neg 0

      def succ Int : Int
      | pos n => pos (S n)
      | neg 0 => pos 1
      | neg (S n) => neg n
      | zro i => pos 1

      def abs Int : Nat
      | pos n => n
      | neg n => n
      | zro _ => 0
      """);
  }
}
