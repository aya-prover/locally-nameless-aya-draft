// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.test.fixtures;

import org.intellij.lang.annotations.Language;

@SuppressWarnings("unused")
public interface TerckError {
  @Language("Aya") String testNonTermination = """
    open import Arith::Nat
    def f Nat : Nat | n => g (suc n)
    def g Nat : Nat
    | 0 => 0
    | suc n => f n
    """;
}
