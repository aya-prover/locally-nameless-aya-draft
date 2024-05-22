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
      open data List (A : Type)
      | nil
      | infixr cons A (List A)
      
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

      def insert-lemma (dec< : Decider A) (a a1 : A) (c : Color) (l1 l2 : RBTree A) (b : Bool) : RBTree A elim b
      | true => balanceRight c l1 a1 (insert a l2 dec<)
      | false => balanceLeft c (insert a l1 dec<) a1 l2

      def insert (a : A) (node : RBTree A) (dec< : Decider A) : RBTree A elim node
      | rbLeaf => rbNode red rbLeaf a rbLeaf
      | rbNode c l1 a1 l2 => insert-lemma dec< a a1 c l1 l2 (dec< a1 a)

      private def aux (List A) (RBTree A) (Decider A) : RBTree A
      | nil, r, _ => r
      | a cons l, r, dec< => aux l (repaint (insert a r dec<)) dec<
      def tree-sort (dec< : Decider A) (l : List A) => rbTreeToList (aux l rbLeaf dec<) nil
      """).filter(x -> x instanceof FnDef || x instanceof DataDef);

  }
}
