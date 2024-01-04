package org.aya.syntax.concrete.expr;

import org.aya.syntax.ref.LocalVar;
import org.aya.util.error.SourceNode;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public sealed interface Expr {
  record Node(@NotNull SourcePos sourcePos, @NotNull Expr expr) implements SourceNode {}

  record Lam(@NotNull LocalVar var, @NotNull Node body) implements Expr {}

  record Ref(@NotNull LocalVar var) implements Expr {}

  record App(@NotNull Node fun, @NotNull Node arg) implements Expr {}
}
