// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.test.fixtures;

import org.intellij.lang.annotations.Language;

@SuppressWarnings("unused")
public interface GoalAndMeta {
  @Language("Aya") String testUnsolved = """
    open import Arith::Nat
    def test : Nat => _
    """;

  @Language("Aya") String testGoal = """
    open import Arith::Nat
    def test (a : Nat) : Nat => {? a ?}
    """;

  @Language("Aya") String testUnsolvedMetaLit = """
    open import Arith::Nat
    open data Nat2 | OO | SS Nat2
    open data Option (A : Type)
      | some A
    def test => some 114514
    """;

  @Language("Aya") String dontTestUnsolvedMetaLit = """
    open import Arith::Nat
    open data Nat2 | OO | SS Nat2
    open data Empty
    
    def take Empty => Empty
    def test => take 114514
    """;
}
