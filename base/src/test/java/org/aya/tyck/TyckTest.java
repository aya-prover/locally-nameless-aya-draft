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

public class TyckTest {
  @Test
  public void test0() {
    @Language("Aya") String code = """
      data Nat | O | S Nat

      def foo (A : Type) (a : A) : A => a
      def lam (A : Type) : Fn (a : A) -> Type => fn a => A
      // def tup (A : Type) (B : A -> Type) (a : A) (b : Fn (a : A) -> B a) : Sig (a : A) ** B a => (a, b a)
      """;

    var result = tyck(code);
  }

  public static @NotNull ImmutableSeq<Def> tyck(@Language("Aya") @NotNull String code) {
    var decls = SyntaxTestUtil.parse(code)
      .map(stmt -> {
        if (stmt instanceof Decl decl) return decl;
        throw new UnsupportedOperationException();
      });

    SyntaxTestUtil.resolve(ImmutableSeq.narrow(decls));
    return SillyTycker.tyck(decls, TestUtil.THROWING);
  }
}
