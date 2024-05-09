// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.collection.immutable.ImmutableSeq;
import org.aya.TestUtil;
import org.aya.syntax.SyntaxTestUtil;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.core.def.Def;
import org.aya.util.error.Panic;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

public class TyckTest {
  @Test
  public void test0() {
    @Language("Aya") String code = """
      data Nat | O | S Nat
      data FreeMonoid (A : Type) | e | cons A (FreeMonoid A)

      def id {A : Type} (a : A) : A => a
      def lam (A : Type) : Fn (a : A) -> Type => fn a => A
      def tup (A : Type) (B : A -> Type) (a : A) (b : Fn (a : A) -> B a)
        : Sig (a : A) ** B a => (id a, id (b a))
      """;

    var result = tyck(code);
  }

  public static @NotNull ImmutableSeq<Def> tyck(@Language("Aya") @NotNull String code) {
    var decls = SyntaxTestUtil.parse(code)
      .map(stmt -> stmt instanceof Decl decl ? decl : Panic.unreachable());

    SyntaxTestUtil.resolve(ImmutableSeq.narrow(decls));
    return SillyTycker.tyck(decls, TestUtil.THROWING);
  }
}
