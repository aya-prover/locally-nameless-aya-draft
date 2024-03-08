// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import org.aya.generic.SortKind;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.core.term.*;
import org.aya.syntax.ref.*;
import org.aya.tyck.tycker.AbstractExprTycker;
import org.aya.tyck.tycker.AppTycker;
import org.aya.util.error.WithPos;
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
      case Expr.App(var f, var a) -> {
        if (!(f.data() instanceof Expr.Ref(var ref))) throw new IllegalStateException("function must be Expr.Ref");
        yield checkApplication(ref, a);
      }
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
      case Expr.Ref(var ref) -> checkApplication(ref, ImmutableSeq.empty());
      case Expr.Sigma _ -> {
        var ty = ty(expr);
        // TODO: type level
        yield new Result.Default(ty, new SortTerm(SortKind.Type, 0));
      }
      case Expr.Pi _  -> {
        var ty = ty(expr);
        // TODO: type level
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
      case Expr.Sugar _ -> throw new UnsupportedOperationException("desugared");
      case Expr.Unresolved _ -> throw new UnsupportedOperationException("?");
    };
  }

  private @NotNull Result checkApplication(@NotNull AnyVar f, @NotNull ImmutableSeq<Expr.NamedArg> args) {
    return switch (f) {
      case LocalVar lVar -> args.foldLeft(new Result.Default(new FreeTerm(lVar), localCtx().get(lVar)), (acc, arg) -> {
        if (arg.name() != null || !arg.explicit()) throw new UnsupportedOperationException("TODO: named arg");
        var pi = ensurePi(acc.type());
        var wellTy = inherit(arg.arg(), pi.param()).wellTyped();
        return new Result.Default(new AppTerm(acc.wellTyped(), wellTy), pi.substBody(wellTy));
      });
      case DefVar<?, ?> defVar -> AppTycker.checkDefApplication(defVar, params -> {
        int argIx = 0, paramIx = 0;
        var result = MutableList.<Term>create();
        while (argIx < args.size() && paramIx < params.size()) {
          var arg = args.get(argIx);
          var param = params.get(paramIx);
          // Implicit insertion
          if (arg.explicit() != param.explicit()) {
            if (!arg.explicit()) {
              throw new UnsupportedOperationException("TODO: implicit application to explicit param");
            } else if (arg.name() == null) {
              // here, arg.explicit() == true and param.explicit() == false
              result.append(mockTerm(param, arg.sourcePos()));
              paramIx++;
              continue;
            } else {
              while (paramIx < params.size() && !Objects.equals(param.name(), arg.name())) {
                result.append(mockTerm(param, arg.sourcePos()));
                paramIx++;
              }
              // ^ insert implicits before the named argument
              if (paramIx == params.size()) {
                // report TODO: named arg not found
                break;
              }
            }
          }
          result.append(inherit(arg.arg(), param.type()).wellTyped());
          argIx++;
          paramIx++;
        }
        return result.toImmutableSeq();
      });
      default -> throw new UnsupportedOperationException("TODO");
    };
  }


  private @NotNull PiTerm ensurePi(Term term) {
    if (term instanceof PiTerm pi) return pi;
    // TODO
    throw new UnsupportedOperationException("TODO: report NotPi");
  }
}
