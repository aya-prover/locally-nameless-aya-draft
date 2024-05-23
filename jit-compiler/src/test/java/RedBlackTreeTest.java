import org.aya.compiler.AyaSerializer;
import org.aya.compiler.ModuleSerializer;
import org.aya.generic.NameGenerator;
import org.aya.syntax.core.def.DataDef;
import org.aya.syntax.core.def.FnDef;
import org.junit.jupiter.api.Test;

// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
public class RedBlackTreeTest {
  @Test public void test1() {
    var result = CompileTest.tyck("""
      open data Nat | O | S Nat
      open data Bool | true | false
      open data List Type
      | nil
      | A => infixr cons A (List A)
      
      open data Color | red | black
      def Decider (A : Type) => Fn (x y : A) -> Bool
      
      variable A : Type
      
      open data RBTree (A : Type) : Type
      | rbLeaf
      | rbNode Color (RBTree A) A (RBTree A)
      
      def rbTreeToList (rb : RBTree A) (r : List A) : List A elim rb
      | rbLeaf => r
      | rbNode x t1 a t2 => rbTreeToList t1 (a cons rbTreeToList t2 r)
      
      def repaint (RBTree A) : RBTree A
      | rbNode c l a r => rbNode black l a r
      | rbLeaf => rbLeaf
      
      def balanceLeft Color (RBTree A) A (RBTree A) : RBTree A
      | black, rbNode red (rbNode red a x b) y c, v, r =>
          rbNode red (rbNode black a x b) y (rbNode black c v r)
      | black, rbNode red a x (rbNode red b y c), v, r =>
          rbNode red (rbNode black a x b) y (rbNode black c v r)
      | c, a, v, r => rbNode c a v r
      
      def balanceRight Color (RBTree A) A (RBTree A) : RBTree A
      | black, l, v, rbNode red (rbNode red b y c) z d =>
          rbNode red (rbNode black l v b) y (rbNode black c z d)
      | black, l, v, rbNode red b y (rbNode red c z d) =>
          rbNode red (rbNode black l v b) y (rbNode black c z d)
      | c, l, v, b => rbNode c l v b

      def insert-lemma (dec_le : Decider A) (a a1 : A) (c : Color) (l1 l2 : RBTree A) (b : Bool) : RBTree A elim b
      | true => balanceRight c l1 a1 (insert a l2 dec_le)
      | false => balanceLeft c (insert a l1 dec_le) a1 l2

      def insert (a : A) (node : RBTree A) (dec_le : Decider A) : RBTree A elim node
      | rbLeaf => rbNode red rbLeaf a rbLeaf
      | rbNode c l1 a1 l2 => insert-lemma dec_le a a1 c l1 l2 (dec_le a1 a)

      private def aux (l : List A) (r : RBTree A) (dec_le : Decider A) : RBTree A elim l
      | nil => r
      | a cons l => aux l (repaint (insert a r dec_le)) dec_le
      def tree-sort (dec_le : Decider A) (l : List A) => rbTreeToList (aux l rbLeaf dec_le) nil
      """).filter(x -> x instanceof FnDef || x instanceof DataDef);

    var out = new ModuleSerializer(new StringBuilder(), 1, new NameGenerator())
        .serialize(result)
        .result();

    var code = STR."""
    package AYA;

    \{AyaSerializer.IMPORT_BLOCK}

    @SuppressWarnings({"NullableProblems", "SwitchStatementWithTooFewBranches", "ConstantValue"})
    public interface baka {
    \{out}
    }
    """;

    System.out.println(code);
  }
}
