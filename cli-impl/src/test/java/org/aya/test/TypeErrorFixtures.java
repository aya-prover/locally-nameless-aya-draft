// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.test;

import org.intellij.lang.annotations.Language;

public interface TypeErrorFixtures {
  @Language("Aya") String testTypeMismatch = """
    open import Arith::Nat
    def test => 1 + Type
    """;

  @Language("Aya") String testNonSigmaProj = """
    open import Arith::Nat
    def test (a : Nat) => a 1
    """;
}
