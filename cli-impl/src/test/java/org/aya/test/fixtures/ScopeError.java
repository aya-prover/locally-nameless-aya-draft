// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.test.fixtures;

import org.intellij.lang.annotations.Language;

@SuppressWarnings("unused")
public interface ScopeError {
  @Language("Aya") String testDidYouMeanDisamb = """
    open data Nat1 | zero
    open data Nat2 | zero
    def one => zero
    """;
  @Language("Aya") String testDidYouMean = """
    data Nat | zero | suc Nat
    def one => suc zero
    """;
  @Language("Aya") String testImportDefineShadow = """
    open data Bool | true | false
    module A {
      def foo : Bool => true
    }
    open A
    def foo : Bool => false
    """;
  @Language("Aya") String testImportDefineShadow2 = """
    open data Bool | true | false
    module A {
      def foo : Bool => true
    }
    def foo : Bool => false
    open A
    """;
  @Language("Aya") String testInfRec = "def undefined => undefined";
  @Language("Aya") String testIssue247 = """
    data Z : Type
    | zero
    | zero
    """;
  @Language("Aya") String testRedefPrim = "prim I prim I";
  @Language("Aya") String testUnknownPrim = "prim senpaiSuki";
  @Language("Aya") String testUnknownVar = """
    open data Nat : Type | zero
    def p => Nat::suc Nat::zero
    """;
}
