// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import org.aya.generic.SortKind;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.core.def.TeleDef;
import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.Callable;
import org.aya.syntax.ref.DefVar;
import org.aya.syntax.ref.LocalCtx;
import org.aya.syntax.ref.LocalVar;
import org.aya.tyck.tycker.AbstractExprTycker;
import org.aya.tyck.tycker.ExprTyckerUtils;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ExprTycker extends AbstractExprTycker {
  public ExprTycker(@NotNull TyckState state, @NotNull LocalCtx ctx, @NotNull Reporter reporter) {
    super(state, ctx, reporter);
  }

  @Override
  public @NotNull Term whnf(@NotNull Term term) {
    // TODO
    return term;
  }

  public @NotNull Result inherit(@NotNull Expr expr, @NotNull Term type) {
    return synthesize(expr);
  }

  public @NotNull Term ty(@NotNull Expr expr) {
    return doTy(expr);
  }

  private @NotNull Term doTy(@NotNull Expr expr) {
    return switch (expr) {
      case Expr.Sort sort -> new SortTerm(sort.kind(), sort.lift());
      case Expr.Pi(var param, var last) -> {
        var wellParam = ty(param.type().data());
        yield subscoped(() -> {
          localCtx().put(param.ref(), wellParam);
          var wellLast = ty(last.data());
          return new PiTerm(wellParam, wellLast);
        });
      }
      default -> synthesize(expr).wellTyped();
    };
  }

  public @NotNull Result synthesize(@NotNull Expr expr) {
    return doSynthesize(expr);
  }

  private @NotNull Result doSynthesize(@NotNull Expr expr) {
    return switch (expr) {
      case Expr.App(var f, var a) -> {
        var resultF = synthesize(f.data());
        if (!(whnf(resultF.type()) instanceof PiTerm fTy)) throw new UnsupportedOperationException("TODO");

        var wellF = whnf(resultF.wellTyped());
        if (wellF instanceof Callable.Tele callF) {
          yield checkAppOnCall(callF, fTy, a);
        }

        var param = fTy.param();
        var wellArg = inherit(a.term(), param).wellTyped();
        var app = AppTerm.make(wellF, wellArg);
        var ty = fTy.body().instantiate(wellArg);

        yield new Result.Default(app, ty);
      }
      case Expr.Hole hole -> throw new UnsupportedOperationException("TODO");
      case Expr.Lambda(var param, var body) -> {
        var paramResult = synthesize(param.type().data());

        yield subscoped(() -> {
          localCtx().put(param.ref(), paramResult.wellTyped());
          var bodyResult = synthesize(body.data());
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
        case DefVar<?, ?> defVar -> ExprTyckerUtils.inferDef(defVar);
        default -> throw new UnsupportedOperationException("TODO");
      };
      case Expr.Sigma sigma -> throw new UnsupportedOperationException("TODO");
      case Expr.Pi(var param, var body) -> {
        var ty = ty(expr);
        yield new Result.Default(ty, new SortTerm(SortKind.Type, 0));
      }
      case Expr.Sort sort -> {
        var ty = ty(sort);
        yield new Result.Default(ty, ty);       // FIXME: Type in Type
      }
      case Expr.Tuple(var items) -> {
        var results = items.map(i -> synthesize(i.data()));
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

  private @NotNull PiTerm ensurePi(Term term) {
    if (term instanceof PiTerm pi) return pi;
    // TODO
    throw new UnsupportedOperationException("TODO: report NotPi");
  }

  private @NotNull Result checkAppOnCall(@NotNull Callable.Tele f, @NotNull PiTerm fTy, @NotNull Expr.NamedArg arg) {
    var argLicit = arg.explicit();
    var tele = TeleDef.defTele(f.ref());
    var param = tele.get(f.args().size());    // always success

    while (param.explicit() != argLicit
      || (arg.name() != null && Objects.equals(param.name(), arg.name()))) {
      if (argLicit || arg.name() != null) {
        // We need to insert hole if:
        // * the parameter is implicit but the argument is explicit
        // * the parameter and the argument is both implicit but the argument is an named argument,
        //   and the parameter is not the one.

        // do insert hole
        var hole = mockTerm(param, arg.sourcePos());
        f = f.applyTo(hole);
        var newTy = fTy.body().instantiate(hole);
        fTy = ensurePi(newTy);
        param = tele.get(f.args().size());
      } else {
        // the parameter is explicit but the argument is implicit
        // which is TODO not cool.
        throw new UnsupportedOperationException("TODO: report not cool");
      }
    }

    // for now, we can safely apply {arg} to {f}

    var wellArg = inherit(arg.term(), fTy.param()).wellTyped();
    return new Result.Default(f.applyTo(wellArg), fTy.body().instantiate(wellArg));
  }
}
