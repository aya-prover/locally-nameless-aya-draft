// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.error;

import org.aya.pretty.doc.Doc;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.core.term.Term;
import org.aya.util.error.SourcePos;
import org.aya.util.prettier.PrettierOptions;
import org.aya.util.reporter.Problem;
import org.jetbrains.annotations.NotNull;

public sealed interface LicitError extends Problem {
  @Override default @NotNull Severity level() {return Severity.ERROR;}
  @Override default @NotNull Stage stage() {return Stage.TYCK;}

  record LicitMismatch(
    @Override @NotNull Expr expr,
    @Override @NotNull SourcePos sourcePos,
    @NotNull Term type
  ) implements LicitError {
    @Override public @NotNull Doc describe(@NotNull PrettierOptions options) {
      return Doc.vcat(
        Doc.english("Cannot check"),
        Doc.par(1, expr.toDoc(options)),
        Doc.english("against the Pi type"),
        Doc.par(1, type.toDoc(options)),
        Doc.english("because explicitness do not match"));
    }
  }

  /*record UnexpectedImplicitArg(@Override @NotNull Expr.NamedArg expr) implements LicitError {
    @Override public @NotNull SourcePos sourcePos() {
      return expr.sourcePos();
    }

    @Override public @NotNull Doc describe(@NotNull PrettierOptions options) {
      return Doc.sep(Doc.english("Unexpected implicit argument"),
        Doc.code(expr.toDoc(options)));
    }
  }*/
}
