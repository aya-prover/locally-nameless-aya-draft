// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.collection.immutable.ImmutableSeq;
import org.aya.TestUtil;
import org.aya.syntax.SyntaxTestUtil;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.core.def.Def;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TyckTest {
  @Test public void test0() {
    var result = tyck("""
      data Nat | O | S Nat
      data FreeMonoid (A : Type) | e | cons A (FreeMonoid A)
      
      def id {A : Type} (a : A) : A => a
      def lam (A : Type) : Fn (a : A) -> Type => fn a => A
      def tup (A : Type) (B : A -> Type) (a : A) (b : Fn (a : A) -> B a)
        : Sig (a : A) ** B a => (id a, id (b a))
      def letExample (A : Type) (B : A -> Type) (f : Fn (a : A) -> B a) (a : A) : B a => let b : B a := f a in b
      """);
    assertTrue(result.isNotEmpty());
  }

  @Test public void test1() {
    var result = tyck("""
      open data Unit | unit
      variable A : Type
      def foo {value : A} : A => value
      def what : Unit => foo {value := unit}
      """);
    assertTrue(result.isNotEmpty());
  }

  @Test public void path0() {
    var result = tyck("""
      data Nat
      | O : Nat
      | S (x : Nat) : Nat
      prim I : ISet
      prim Path (A : I -> Type) (a : A 0) (b : A 1) : Type
      prim coe (r s : I) (A : I -> Type) : A r -> A s
      
      def transp (A : I -> Type) (a : A 0) : A 1 => coe 0 1 A a
      def transpInv (A : I -> Type) (a : A 1) : A 0 => coe 1 0 A a
      def coeFill0 (A : I -> Type) (u : A 0) : Path A u (transp A u) => \\i => coe 0 i A u
      """);
    assertTrue(result.isNotEmpty());
  }

  @Test public void path1() {
    var result = tyck("""
      data Nat | O | S Nat
      prim I : ISet
      prim Path (A : I -> Type) (a : A 0) (b : A 1) : Type
      variable A : Type
      def infix = (a b : A) : Type => Path (\\i => A) a b
      def refl {a : A} : a = a => \\i => a
      open data Int
      | pos Nat | neg Nat
      | zro : pos 0 = neg 0
      example def testZro0 : zro 0 = pos 0 => refl
      example def testZro1 : zro 1 = neg 0 => refl
      """);
    assertTrue(result.isNotEmpty());
  }

  @Test public void issue768() {
    var result = tyck("""
      open data Unit | unit
      data Nat | O | S Nat
      open data SomeDT Nat
      | m => someDT
      def how' {m : Nat} (a : Nat) (b : SomeDT m) : Nat => 0
      def what {A : Nat -> Type} (B : Fn (n : Nat) -> A n -> Nat) : Unit => unit
      def boom : Unit => what (fn n => fn m => how' 0 m)
      """);
    assertTrue(result.isNotEmpty());
  }

  public static @NotNull ImmutableSeq<Def> tyck(@Language("Aya") @NotNull String code) {
    var stmts = SyntaxTestUtil.parse(code);
    var resolveInfo = SyntaxTestUtil.resolve(ImmutableSeq.narrow(stmts));
    var decls = stmts.filterIsInstance(Decl.class);
    decls.forEach(decl -> System.out.println(STR."Scanned \{decl.ref().name()}"));
    return SillyTycker.tyck(resolveInfo, decls, TestUtil.THROWING);
  }
}
