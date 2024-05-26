// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.

import kala.collection.immutable.ImmutableSeq;
import kala.collection.immutable.primitive.ImmutableIntSeq;
import org.aya.normalize.Normalizer;
import org.aya.normalize.PrimFactory;
import org.aya.syntax.compile.JitCon;
import org.aya.syntax.compile.JitData;
import org.aya.syntax.compile.JitFn;
import org.aya.syntax.core.Closure;
import org.aya.syntax.core.term.LamTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.ConCall;
import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.core.term.call.FnCall;
import org.aya.syntax.core.term.repr.IntegerTerm;
import org.aya.syntax.core.term.repr.ListTerm;
import org.aya.syntax.literate.CodeOptions;
import org.aya.tyck.TyckState;
import org.aya.util.error.Panic;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.function.Function;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.*;

public class RedBlackTreeTest {
  @Test public void test1() {
    var result = CompileTest.tyck("""
      open data Nat | O | S Nat
      open data Bool | True | False
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

      def le (x y : Nat) : Bool
      | O, _ => True
      | S _, O => False
      | S a, S b => le a b
      
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

      def insert_lemma (dec_le : Decider A) (a a1 : A) (c : Color) (l1 l2 : RBTree A) (b : Bool) : RBTree A elim b
      | True => balanceRight c l1 a1 (insert a l2 dec_le)
      | False => balanceLeft c (insert a l1 dec_le) a1 l2

      def insert (a : A) (node : RBTree A) (dec_le : Decider A) : RBTree A elim node
      | rbLeaf => rbNode red rbLeaf a rbLeaf
      | rbNode c l1 a1 l2 => insert_lemma dec_le a a1 c l1 l2 (dec_le a1 a)

      private def aux (l : List A) (r : RBTree A) (dec_le : Decider A) : RBTree A elim l
      | nil => r
      | a cons l => aux l (repaint (insert a r dec_le)) dec_le
      def tree_sort (dec_le : Decider A) (l : List A) => rbTreeToList (aux l rbLeaf dec_le) nil
      """);

    var tester = CompileTester.make(result.defs(), result.info().shapeFactory());
    tester.compile();

    System.out.println(tester.code);

    var List = tester.<JitData>loadInstance("baka", "List");
    var nil = tester.<JitCon>loadInstance("baka", "List", "nil");
    var cons = tester.<JitCon>loadInstance("baka", "List", "cons");
    var Nat = tester.<JitData>loadInstance("baka", "Nat");
    var O = tester.<JitCon>loadInstance("baka", "Nat", "O");
    var S = tester.<JitCon>loadInstance("baka", "Nat", "S");
    var Bool = tester.<JitData>loadInstance("baka", "Bool");
    var True = tester.<JitCon>loadInstance("baka", "Bool", "True");
    var False = tester.<JitCon>loadInstance("baka", "Bool", "False");
    var tree_sort = tester.<JitFn>loadInstance("baka", "tree_sort");
    var le = tester.<JitFn>loadInstance("baka", "le");

    var NatCall = new DataCall(Nat, 0, ImmutableSeq.empty());
    var ListNatCall = new DataCall(List, 0, ImmutableSeq.of(NatCall));

    // note that we didn't supply the correct shape factory, so IntegerTerm is unavailable.
    IntFunction<Term> mkInt = i -> new IntegerTerm(i, O, S, NatCall);

    Function<ImmutableIntSeq, Term> mkList = xs -> new ListTerm(xs.mapToObj(mkInt::apply), nil, cons, ListNatCall);

    // var decider = new LamTerm(new Closure.Jit(l -> new LamTerm(new Closure.Jit(r -> {
    //   if (l instanceof IntegerTerm lint && r instanceof IntegerTerm rint) {
    //      var def = lint.repr() <= rint.repr() ? True : False;
    //      return new ConCall(def, ImmutableSeq.empty(), 0, ImmutableSeq.empty());
    //   } else {
    //     return Panic.unreachable();
    //   }
    // }))));

    var leCall = new LamTerm(new Closure.Jit(x ->
      new LamTerm(new Closure.Jit(y ->
        new FnCall(le, 0, ImmutableSeq.of(x, y))))));

    var seed = 114514L;
    var random = new Random(seed);
    var largeList = mkList.apply(ImmutableIntSeq.fill(10, () -> Math.abs(random.nextInt()) % 100));
    var args = ImmutableSeq.of(NatCall, leCall, largeList);

    var beginTime = System.currentTimeMillis();
    var sortResult = new Normalizer(new TyckState(result.info().shapeFactory(), new PrimFactory()))
      .normalize(tree_sort.invoke(null, args), CodeOptions.NormalizeMode.FULL);
    var endTime = System.currentTimeMillis();
    assertNotNull(sortResult);

    System.out.println(STR."Done in \{(endTime - beginTime)}");
    System.out.println(sortResult.debuggerOnlyToDoc().debugRender());
  }
}
