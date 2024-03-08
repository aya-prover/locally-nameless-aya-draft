// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.collection.immutable.ImmutableSeq;
import org.aya.generic.SortKind;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.core.term.*;
import org.aya.syntax.ref.DefVar;
import org.aya.syntax.ref.LocalCtx;
import org.aya.syntax.ref.LocalVar;
import org.aya.tyck.tycker.AbstractExprTycker;
import org.aya.tyck.tycker.AppTycker;
import org.aya.util.error.WithPos;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

public final class ExprTycker extends AbstractExprTycker {
  public ExprTycker(@NotNull TyckState state, @NotNull LocalCtx ctx, @NotNull Reporter reporter) {
    super(state, ctx, reporter);
  }

  @Override
  public @NotNull Term whnf(@NotNull Term term) {
    // TODO
    return term;
  }

  public @NotNull Result inherit(@NotNull WithPos<Expr> expr, @NotNull Term type) {
    return synthesize(expr);
  }

  public @NotNull Term ty(@NotNull WithPos<Expr> expr) {
    return doTy(expr);
  }

  private @NotNull Term doTy(@NotNull WithPos<Expr> expr) {
    return switch (expr.data()) {
      case Expr.Sort sort -> new SortTerm(sort.kind(), sort.lift());
      case Expr.Pi(var param, var last) -> {
        var wellParam = ty(param.type());
        yield subscoped(() -> {
          localCtx().put(param.ref(), wellParam);
          var wellLast = ty(last);
          return new PiTerm(wellParam, wellLast);
        });
      }
      default -> synthesize(expr).wellTyped();
    };
  }

  public @NotNull Result synthesize(@NotNull WithPos<Expr> expr) {
    return doSynthesize(expr);
  }

  private @NotNull Result doSynthesize(@NotNull WithPos<Expr> expr) {
    return switch (expr.data()) {
      case Expr.App(var f, var a) -> checkApplication(f, a);
      case Expr.Hole hole -> throw new UnsupportedOperationException("TODO");
      case Expr.Lambda(var param, var body) -> {
        var paramResult = synthesize(param.type());

        yield subscoped(() -> {
          localCtx().put(param.ref(), paramResult.wellTyped());
          var bodyResult = synthesize(body);
          var lamTerm = new LamTerm(bodyResult.wellTyped().bind(param.ref()));
          var ty = new PiTerm(
            paramResult.type(),
            bodyResult.type()   // TODO: do we need to `.bind` on type?
          );
          return new Result.Default(lamTerm, ty);
        });
      }
      case Expr.LitInt litInt -> throw new UnsupportedOperationException("TODO");
      case Expr.LitString litString -> throw new UnsupportedOperationException("TODO");
      case Expr.Ref ref -> switch (ref.var()) {
        case LocalVar lVar -> new Result.Default(new FreeTerm(lVar), localCtx().get(lVar));
        case DefVar<?, ?> defVar -> AppTycker.checkDefApplication(defVar, _ -> ImmutableSeq.empty());
        default -> throw new UnsupportedOperationException("TODO");
      };
      case Expr.Sigma sigma -> throw new UnsupportedOperationException("TODO");
      case Expr.Pi(var param, var body) -> {
        var ty = ty(expr);
        yield new Result.Default(ty, new SortTerm(SortKind.Type, 0));
      }
      case Expr.Sort _ -> {
        var ty = ty(expr);
        yield new Result.Default(ty, ty);       // FIXME: Type in Type
      }
      case Expr.Tuple(var items) -> {
        var results = items.map(this::synthesize);
        var wellTypeds = results.map(Result::wellTyped);
        var tys = results.map(Result::type);
        var wellTyped = new TupTerm(wellTypeds);
        var ty = new SigmaTerm(tys);

        yield new Result.Default(wellTyped, ty);
      }
      case Expr.Error error -> throw new UnsupportedOperationException("TODO");
      case Expr.Let let -> throw new UnsupportedOperationException("TODO");
      case Expr.Array array -> throw new UnsupportedOperationException("TODO");
      case Expr.RawSort rawSort -> throw new UnsupportedOperationException("desugared");
      case Expr.Do aDo -> throw new UnsupportedOperationException("desugared");
      case Expr.BinOpSeq _ -> throw new UnsupportedOperationException("deesugared");
      case Expr.Idiom _ -> throw new UnsupportedOperationException("desugared");
      case Expr.Unresolved _ -> throw new UnsupportedOperationException("?");
    };
  }

  private @NotNull Result checkApplication(WithPos<Expr> f, ImmutableSeq<Expr.NamedArg> a) {
    throw new UnsupportedOperationException("TODO");
  }

  private @NotNull PiTerm ensurePi(Term term) {
    if (term instanceof PiTerm pi) return pi;
    // TODO
    throw new UnsupportedOperationException("TODO: report NotPi");
  }
}
