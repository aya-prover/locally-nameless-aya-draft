// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.

import kala.collection.immutable.ImmutableSeq;
import kala.collection.immutable.primitive.ImmutableIntSeq;
import org.aya.normalize.Normalizer;
import org.aya.syntax.compile.JitCon;
import org.aya.syntax.compile.JitData;
import org.aya.syntax.compile.JitFn;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.core.term.repr.IntegerTerm;
import org.aya.syntax.core.term.repr.ListTerm;
import org.aya.syntax.literate.CodeOptions;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.function.Function;
import java.util.function.IntFunction;

import static org.aya.compiler.AbstractSerializer.javify;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RedBlackTreeTest {
  @Language("Aya") public static final String TreeSort = """
    open data Nat | O | S Nat
    open data Bool | True | False
    open data List Type
    | []
    | A => infixr :> A (List A)
    
    def isZero (a : Nat) : Bool
    | 0 => True
    | _ => False
    
    open data Color | red | black
    def Decider (A : Type) => Fn (x y : A) -> Bool
    
    variable A : Type
    
    open data RBTree (A : Type) : Type
    | rbLeaf
    | rbNode Color (RBTree A) A (RBTree A)
    
    def rbTreeToList (rb : RBTree A) (r : List A) : List A elim rb
    | rbLeaf => r
    | rbNode x t1 a t2 => rbTreeToList t1 (a :> rbTreeToList t2 r)
    
    def repaint (RBTree A) : RBTree A
    | rbNode c l a r => rbNode black l a r
    | rbLeaf => rbLeaf
    
    def sub (x y : Nat) : Nat
    | n, 0 => n
    | 0, S _ => 0
    | S n, S m => sub n m
    
    def le (x y : Nat) => isZero (sub x y)
    
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
    | [] => r
    | a :> l => aux l (repaint (insert a r dec_le)) dec_le
    def tree_sort (dec_le : Decider A) (l : List A) => rbTreeToList (aux l rbLeaf dec_le) []
    def tree_sortNat (l : List Nat) => tree_sort le l
    """;

  @Test public void test1() throws IOException {
    var result = CompileTest.tyck(TreeSort);

    var tester = new CompileTester(CompileTest.serializeFrom(result));
    Files.writeString(Paths.get("src/test/gen/baka.java"), tester.code);
    tester.compile();

    JitData List = tester.loadInstance("baka", "List");
    JitCon nil = tester.loadInstance("baka", "List", javify("[]"));
    JitCon cons = tester.loadInstance("baka", "List", javify(":>"));
    JitData Nat = tester.loadInstance("baka", "Nat");
    JitCon O = tester.loadInstance("baka", "Nat", "O");
    JitCon S = tester.loadInstance("baka", "Nat", "S");
    JitFn tree_sortNat = tester.loadInstance("baka", "tree_sortNat");

    var NatCall = new DataCall(Nat, 0, ImmutableSeq.empty());
    var ListNatCall = new DataCall(List, 0, ImmutableSeq.of(NatCall));

    IntFunction<Term> mkInt = i -> new IntegerTerm(i, O, S, NatCall);

    Function<ImmutableIntSeq, Term> mkList = xs -> new ListTerm(xs.mapToObj(mkInt), nil, cons, ListNatCall);

    var seed = 114514L;
    var random = new Random(seed);
    var largeList = mkList.apply(ImmutableIntSeq.fill(200, () -> random.nextInt(150)));
    var args = ImmutableSeq.of(largeList);

    var normalizer = new Normalizer(result.info().makeTyckState());
    var sortResult = normalizer.normalize(tree_sortNat.invoke(null, args), CodeOptions.NormalizeMode.FULL);
    assertNotNull(sortResult);

    Profiler.profileMany(5, () ->
      normalizer.normalize(tree_sortNat.invoke(null, args), CodeOptions.NormalizeMode.FULL));

    System.out.println(sortResult.debuggerOnlyToString());
  }
}
