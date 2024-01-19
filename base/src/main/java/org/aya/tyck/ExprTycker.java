// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import org.aya.syntax.concrete.Expr;
import org.aya.syntax.core.term.*;
import org.aya.syntax.ref.LocalCtx;
import org.aya.tyck.tycker.AbstractExprTycker;
import org.aya.util.Arg;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

public final class ExprTycker extends AbstractExprTycker {
  public ExprTycker(@NotNull TyckState state, @NotNull LocalCtx ctx, @NotNull Reporter reporter) {
    super(state, ctx, reporter);
  }

  @Override
  public @NotNull Term whnf(@NotNull Term term) {
    throw new UnsupportedOperationException("TODO");
  }

  public @NotNull Result synthesize(@NotNull Expr expr) {
    return doSynthesize(expr);
  }

  private @NotNull Result doSynthesize(@NotNull Expr expr) {
    return switch (expr) {
      case Expr.App app -> throw new UnsupportedOperationException("TODO");
      case Expr.Array array -> throw new UnsupportedOperationException("TODO");
      case Expr.Error error -> throw new UnsupportedOperationException("TODO");
      case Expr.Hole hole -> throw new UnsupportedOperationException("TODO");
      case Expr.Lambda(var param, var body) -> {
        var paramResult = synthesize(param.type().data());

        yield subscoped(() -> {
          localCtx().put(param.ref(), paramResult.wellTyped());
          var bodyResult = synthesize(body.data());
          var lamTerm = new LamTerm(param.explicit(), bodyResult.wellTyped().bind(param.ref()));
          var ty = new PiTerm(
            new Arg<>(paramResult.type(), param.explicit()),
            bodyResult.type()   // TODO: do we need to `.bind` on type?
          );
          return new Result.Default(lamTerm, ty);
        });
      }
      case Expr.Let let -> throw new UnsupportedOperationException("TODO");
      case Expr.LitInt litInt -> throw new UnsupportedOperationException("TODO");
      case Expr.LitString litString -> throw new UnsupportedOperationException("TODO");
      case Expr.Pi(var param, var body) -> throw new UnsupportedOperationException("TODO");
      case Expr.RawSort rawSort -> throw new UnsupportedOperationException("TODO");
      case Expr.Ref ref -> new Result.Default(new FreeTerm(ref.var()), localCtx().get(ref.var()));
      case Expr.Sigma sigma -> throw new UnsupportedOperationException("TODO");
      case Expr.Sort sort -> throw new UnsupportedOperationException("TODO");
      case Expr.Tuple(var items) -> {
        var results = items.map(i -> synthesize(i.data()));
        var wellTypeds = results.map(Result::wellTyped);
        var tys = results.map(Result::type);
        var wellTyped = new TupTerm(wellTypeds);
        var ty = new SigmaTerm(tys);

        yield new Result.Default(wellTyped, ty);
      }
      case Expr.Do aDo -> throw new UnsupportedOperationException("desugared");
      case Expr.BinOpSeq _ -> throw new UnsupportedOperationException("deesugared");
      case Expr.Idiom _ -> throw new UnsupportedOperationException("desugared");
      case Expr.Unresolved _ -> throw new UnsupportedOperationException("?");
    };
  }
}
