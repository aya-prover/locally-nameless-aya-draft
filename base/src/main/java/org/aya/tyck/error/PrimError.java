// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.error;

import org.aya.pretty.doc.Doc;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.util.error.SourcePos;
import org.aya.util.prettier.PrettierOptions;
import org.jetbrains.annotations.NotNull;

public sealed interface PrimError extends TyckError {
  record NoResultType(@NotNull TeleDecl.PrimDecl prim) implements PrimError {
    @Override public @NotNull Doc describe(@NotNull PrettierOptions options) {
      return Doc.sep(Doc.code(prim.toDoc(options)), Doc.english("is expected to have a type"));
    }

    @Override public @NotNull SourcePos sourcePos() { return prim.sourcePos(); }
  }

  record BadSignature(
    @NotNull TeleDecl.PrimDecl prim,
    @NotNull UnifyInfo.Comparison comparison,
    @NotNull UnifyInfo info
  ) implements PrimError {
    @Override public @NotNull Doc describe(@NotNull PrettierOptions options) {
      var prologue = Doc.vcat(
        Doc.english("The prim declaration"),
        Doc.par(1, prim.toDoc(options)),
        Doc.english("is written to have the type"));
      return info.describeUnify(options, comparison, prologue, Doc.english("while the standard type is"));
    }
    @Override public @NotNull SourcePos sourcePos() {
      return prim.sourcePos();
    }
  }

  record BadInterval(@NotNull SourcePos sourcePos, int integer) implements PrimError {
    @Override public @NotNull Doc describe(@NotNull PrettierOptions options) {
      return Doc.sep(Doc.english("The point"),
        Doc.code(String.valueOf(integer)),
        Doc.english("does not live in interval"));
    }

    @Override public @NotNull Doc hint(@NotNull PrettierOptions options) {
      return Doc.sep(Doc.english("Did you mean: "), Doc.code("0"), Doc.plain("or"), Doc.code("1"));
    }
  }
}
