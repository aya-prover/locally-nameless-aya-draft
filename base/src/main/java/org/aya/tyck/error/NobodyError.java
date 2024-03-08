// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.error;

/*public record NobodyError(
  @Override @NotNull SourcePos sourcePos,
  @NotNull DefVar<FnDef, TeleDecl.FnDecl> var
) implements TyckError {
  @Override public @NotNull Doc describe(@NotNull PrettierOptions options) {
    return Doc.sep(
      Doc.english("The empty pattern-matching function"),
      BasePrettier.defVar(var),
      Doc.english("does not have a telescope"));
  }
}*/
