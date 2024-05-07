// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.def;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import org.aya.generic.AyaDocile;
import org.aya.pretty.doc.Doc;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.LocalVar;
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
  public @NotNull Signature<?> bindAt(@NotNull LocalVar var, int index) {
    var boundParam = param.mapIndexed((i, p) ->
      p.replace(p.data().map(t -> t.bindAt(var, index + i))));
    // bindAt may not preserve type here, consider [result] is a [FreeTerm].
    var boundResult = result.bindAt(var, param.size() + index);
    return new Signature<>(boundParam, boundResult);
  }

  /**
   * @see #bindTele(SeqView)
   */
  public @NotNull Signature<?> bindAll(@NotNull SeqView<LocalVar> vars) {
    // omg, construct [vars.size()] objects!!
    return vars.<Signature<?>>foldLeftIndexed(this, (idx, acc, var) -> acc.bindAt(var, idx));
  }

  /**
   * Bind addition telescope on this signature
   *
   * @param vars telescope
   */
  public @NotNull Signature<?> bindTele(@NotNull SeqView<LocalVar> vars) {
    return bindAll(vars.reversed());
  }

  @Override public @NotNull Doc toDoc(@NotNull PrettierOptions options) {
    // return Doc.sep(Doc.sep(param.view().map(p -> p.toDoc(options))), Doc.symbol("->"), result.toDoc(options));
    throw new UnsupportedOperationException("TODO");
  }
}
