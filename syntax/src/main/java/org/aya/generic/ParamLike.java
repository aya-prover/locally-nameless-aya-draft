// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.generic;

import kala.collection.mutable.MutableList;
import org.aya.prettier.BasePrettier;
import org.aya.pretty.doc.Doc;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.prettier.PrettierOptions;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

/**
 * @param <Expr> the type of the expression contained, either
 *               {@link org.aya.syntax.core.term.Term} or {@link org.aya.syntax.concrete.Expr}.
 */
public interface ParamLike<Expr extends AyaDocile> extends AyaDocile {
  boolean explicit();
  @NotNull LocalVar ref();
  @NotNull Expr type();
  @NotNull ParamLike<Expr> map(@NotNull UnaryOperator<Expr> mapper);
  default @NotNull Doc nameDoc() { return BasePrettier.linkDef(ref()); }

  @Override default @NotNull Doc toDoc(@NotNull PrettierOptions options) {
    return toDoc(nameDoc(), options);
  }

  default @NotNull Doc toDoc(@NotNull Doc names, @NotNull PrettierOptions options) {
    var type = type();
    var docs = MutableList.of(names);
    docs.append(Doc.symbol(":"));
    docs.append(type.toDoc(options));
    return Doc.licit(explicit(), Doc.sep(docs));
  }
}
