// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.generic;

import org.aya.pretty.doc.Doc;
import org.aya.util.prettier.PrettierOptions;
import org.jetbrains.annotations.Debug;
import org.jetbrains.annotations.NotNull;

/**
 * @author ice1000
 */
@FunctionalInterface
@Debug.Renderer(text = "debuggerOnlyToDoc().debugRender()")
public interface AyaDocile /*extends Docile*/ {
  /**
   * Load PrettierOptions by using it explicitly so IDEA won't show cannot load blahblah
   * in the debugger window.
   *
   * @apiNote This should not be used in any other place.
   * @deprecated use {@link #toDoc(PrettierOptions)} instead
   */
  @Deprecated default @NotNull Doc debuggerOnlyToDoc() {
    throw new UnsupportedOperationException("TODO");
  }

  @NotNull Doc toDoc(@NotNull PrettierOptions options);
  // @Override default @NotNull Doc toDoc() {
  //   return toDoc(PrettierOptions.DEBUG);
  // }
}