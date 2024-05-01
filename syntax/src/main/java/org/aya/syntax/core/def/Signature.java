// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.def;

import kala.collection.immutable.ImmutableSeq;
import org.aya.generic.AyaDocile;
import org.aya.pretty.doc.Doc;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.Term;
import org.aya.util.error.WithPos;
import org.aya.util.prettier.PrettierOptions;
import org.jetbrains.annotations.NotNull;

/**
 * Signature of a definition, used in concrete and tycking.
 *
 * @apiNote All terms in signature are as bound as possible.
 *
 * @author ice1000
 */
public record Signature<T extends Term>(
  @NotNull ImmutableSeq<WithPos<Param>> param,
  @NotNull T result
) implements AyaDocile {
  @Override public @NotNull Doc toDoc(@NotNull PrettierOptions options) {
    // return Doc.sep(Doc.sep(param.view().map(p -> p.toDoc(options))), Doc.symbol("->"), result.toDoc(options));
    throw new UnsupportedOperationException("TODO");
  }
}
