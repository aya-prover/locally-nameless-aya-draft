// Copyright (c) 2020-2023 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.def;

import org.aya.generic.AyaDocile;
import org.aya.pretty.doc.Doc;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.DefVar;
import org.aya.util.prettier.PrettierOptions;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * @author zaoqi
 */
public sealed interface Def extends AyaDocile permits ClassDef, TeleDef {
  @NotNull DefVar<?, ?> ref();

  void descentConsume(@NotNull Consumer<Term> f, @NotNull Consumer<Pat> g);

  @Override default @NotNull Doc toDoc(@NotNull PrettierOptions options) {
    throw new UnsupportedOperationException("TODO");
  }
}
