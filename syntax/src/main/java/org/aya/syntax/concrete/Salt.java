// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.concrete;

import org.aya.generic.SortKind;
import org.aya.util.PosedUnaryOperator;
import org.aya.util.error.InternalException;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Salt implements PosedUnaryOperator<Expr> {
  public static class DesugarInterruption extends Exception {}

  private @Nullable Integer levelVar(@NotNull WithPos<Expr> expr) {
    return switch (expr.data()) {
      case Expr.BinOpSeq _ -> levelVar(expr.descent(this));
      case Expr.LitInt i -> i.integer();
      default -> null;
    };
  }

  @Override
  public @NotNull Expr apply(@NotNull SourcePos sourcePos, @NotNull Expr expr) {
    // we may desugar type app (type with universe level) first
    return switch (expr) {
      case Expr.App(var f, var arg)
        when f.data() instanceof Expr.RawSort typeF
        && typeF.kind() != SortKind.ISet    // Do not report at desugar stage, it should be reported at tyck stage
        && arg.sizeEquals(1) -> {
        var level = levelVar(new WithPos<>(sourcePos, expr));
        if (level == null) yield new Expr.Error(expr);

        yield switch (typeF.kind()) {
          case Type -> new Expr.Type(level);
          case Set -> new Expr.Set(level);
          case ISet -> throw new InternalException("unreachable");
        };
      }
      case Expr.Sugar satou -> desugar(sourcePos, satou);
      default -> expr;
    };
  }

  public @NotNull Expr desugar(@NotNull SourcePos sourcePos, @NotNull Expr.Sugar satou) {
    return switch (satou) {
      case Expr.BinOpSeq(var seq) -> {
        // TODO: BinOpParser
        // TODO: This silly desugarer is unable to desugar (f a b) c correctly
        assert seq.isNotEmpty();
        yield apply(sourcePos, new Expr.App(seq.getFirst().arg(), seq.drop(1)));
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
