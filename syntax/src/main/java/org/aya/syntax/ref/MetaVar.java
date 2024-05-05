// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.ref;

import org.aya.generic.AyaDocile;
import org.aya.pretty.doc.Doc;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.MetaCall;
import org.aya.util.error.SourcePos;
import org.aya.util.prettier.PrettierOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * if the <code>args</code> of the {@link MetaCall} is larger than ctxSize,
 * then in case there is {@link OfType}, we will need to type check the argument
 * and check the solution against the iterated <strong>codomain</strong> instead of the type itself.
 *
 * @param ctxSize size of the original context.
 * @param pos     error report of this MetaCall will be associated with this position.
 * @see MetaCall
 */
public record MetaVar(
  @Override @NotNull String name,
  @NotNull SourcePos pos,
  int ctxSize, @NotNull Requirement req
) implements AnyVar {
  @Override public boolean equals(@Nullable Object o) {return this == o;}

  @Override public int hashCode() {return System.identityHashCode(this);}

  public sealed interface Requirement extends AyaDocile {}
  public enum Misc implements Requirement {
    Whatever,
    IsType,
    ;
    @Override public @NotNull Doc toDoc(@NotNull PrettierOptions options) {
      return switch (this) {
        case Whatever -> Doc.plain("Nothing!");
        case IsType -> Doc.sep(Doc.plain("_"), Doc.symbols(":", "?"));
      };
    }
  }
  public record OfType(@NotNull Term type) implements Requirement {
    @Override public @NotNull Doc toDoc(@NotNull PrettierOptions options) {
      return Doc.sep(Doc.symbol("?"), Doc.symbol(":"), type.toDoc(options));
    }
  }
}
