// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.concrete;

import org.aya.util.PosedUnaryOperator;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public class Salt implements PosedUnaryOperator<Expr> {
  @Override
  public @NotNull Expr apply(@NotNull SourcePos sourcePos, @NotNull Expr expr) {
    return expr instanceof Expr.Sugar satou ? desugar(sourcePos, satou) : expr;
  }

  public @NotNull Expr desugar(@NotNull SourcePos sourcePos, @NotNull Expr.Sugar satou) {
    return switch (satou) {
      case Expr.BinOpSeq(var seq) -> {
        // TODO: BinOpParser
        assert seq.isNotEmpty();
        yield new Expr.App(seq.getFirst().arg(), seq.drop(1));
      }
      case Expr.Do aDo -> throw new UnsupportedOperationException("TODO");
      case Expr.Idiom idiom -> throw new UnsupportedOperationException("TODO");
      case Expr.RawSort(var kind) -> switch (kind) {
        case Type -> new Expr.Type(0);
        case Set -> new Expr.Set(0);
        case ISet -> Expr.ISet.INSTANCE;
      };
    };
  }
}
