// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import org.aya.generic.SortKind;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.core.term.*;
import org.aya.syntax.ref.*;
import org.aya.tyck.error.BadTypeError;
import org.aya.tyck.error.LicitError;
import org.aya.tyck.error.NoRuleError;
import org.aya.tyck.tycker.AbstractTycker;
import org.aya.tyck.tycker.AppTycker;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

public final class ExprTycker extends AbstractTycker {
  public ExprTycker(@NotNull TyckState state, @NotNull LocalCtx ctx, @NotNull DeBruijnCtx dCtx, @NotNull Reporter reporter) {
    super(state, ctx, dCtx, reporter);
  }

  public @NotNull Result inherit(@NotNull WithPos<Expr> expr, @NotNull Term type) {
    return synthesize(expr);
  }

  public @NotNull Term ty(@NotNull WithPos<Expr> expr) {
    return doTy(expr);
  }

  public @NotNull Result.Sort sort(@NotNull WithPos<Expr> expr) {
    return new Result.Sort(sort(expr, ty(expr)));
  }

  private @NotNull SortTerm sort(@NotNull WithPos<Expr> errorMsg, @NotNull Term term) {
    return switch (whnf(term)) {
      case SortTerm u -> u;
      // case MetaTerm hole -> {
      //   unifyTyReported(hole, SortTerm.Type0, errorMsg);
      //   yield SortTerm.Type0;
      // }
      default -> {
        reporter.report(BadTypeError.univ(state, errorMsg.sourcePos(), errorMsg.data(), term));
        yield SortTerm.Type0;
      }
    };
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
        yield checkApplication(ref, expr.sourcePos(), a);
      }
      case Expr.Hole hole -> throw new UnsupportedOperationException("TODO");
      case Expr.Lambda(var param, var body) -> {
        var paramResult = ty(param.type());
        yield subscoped(() -> {
          localCtx().put(param.ref(), paramResult);
          var bodyResult = synthesize(body).bind(param.ref());
          var lamTerm = new LamTerm(bodyResult.wellTyped());
          var ty = new PiTerm(paramResult, bodyResult.type());
          return new Result.Default(lamTerm, ty);
        });
      }
      case Expr.LitInt litInt -> throw new UnsupportedOperationException("TODO");
      case Expr.LitString litString -> throw new UnsupportedOperationException("TODO");
      case Expr.Ref(var ref) -> checkApplication(ref, expr.sourcePos(), ImmutableSeq.empty());
      case Expr.Sigma _ -> {
        var ty = ty(expr);
        // TODO: type level
        yield new Result.Default(ty, new SortTerm(SortKind.Type, 0));
      }
      case Expr.Pi _ -> {
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
      case Expr.Unresolved _ -> throw new UnsupportedOperationException("?");
      case Expr.Sugar _ -> throw new IllegalArgumentException("these exprs are desugared, should be unreachable");
      default -> fail(expr.data(), new NoRuleError(expr.data(), expr.sourcePos(), null));
    };
  }

  private @NotNull Result checkApplication(
    @NotNull AnyVar f, @NotNull SourcePos sourcePos,
    @NotNull ImmutableSeq<Expr.NamedArg> args
  ) {
    try {
      return doCheckApplication(f, args);
    } catch (NotPi notPi) {
      var expr = new Expr.App(new WithPos<>(sourcePos, new Expr.Ref(f)), args);
      return fail(expr, BadTypeError.pi(state, sourcePos, expr, notPi.actual));
    }
  }

  private @NotNull Result doCheckApplication(
    @NotNull AnyVar f, @NotNull ImmutableSeq<Expr.NamedArg> args
  ) throws NotPi {
    return switch (f) {
      case LocalVar lVar -> generateApplication(args,
        new Result.Default(new FreeTerm(lVar), localCtx().get(lVar)));
      case DefVar<?, ?> defVar -> AppTycker.checkDefApplication(defVar, (params, k) -> {
        int argIx = 0, paramIx = 0;
        var result = MutableList.<Term>create();
        while (argIx < args.size() && paramIx < params.size()) {
          var arg = args.get(argIx);
          var param = params.get(paramIx);
          // Implicit insertion
          if (arg.explicit() != param.explicit()) {
            if (!arg.explicit()) {
              reporter.report(new LicitError.BadImplicitArg(arg));
              break;
            } else if (arg.name() == null) {
              // here, arg.explicit() == true and param.explicit() == false
              result.append(mockTerm(param, arg.sourcePos()));
              paramIx++;
              continue;
            } else {
              while (paramIx < params.size() && !param.nameEq(arg.name())) {
                result.append(mockTerm(param, arg.sourcePos()));
                paramIx++;
              }
              // ^ insert implicits before the named argument
              if (paramIx == params.size()) {
                reporter.report(new LicitError.BadImplicitArg(arg));
                break;
              }
            }
          }
          result.append(inherit(arg.arg(), param.type()).wellTyped());
          argIx++;
          paramIx++;
        }
        if (argIx < args.size()) {
          generateApplication(args.drop(argIx), k.apply(result.toImmutableSeq()));
        } else if (paramIx < params.size()) {
          // TODO: eta-expand
          throw new UnsupportedOperationException("TODO");
        }
        return k.apply(result.toImmutableSeq());
      });
      default -> throw new UnsupportedOperationException("TODO");
    };
  }

  private Result generateApplication(@NotNull ImmutableSeq<Expr.NamedArg> args, Result start) throws NotPi {
    return args.foldLeftChecked(start, (acc, arg) -> {
      if (arg.name() != null || !arg.explicit()) reporter.report(new LicitError.BadNamedArg(arg));
      var pi = ensurePi(acc.type());
      var wellTy = inherit(arg.arg(), pi.param()).wellTyped();
      return new Result.Default(new AppTerm(acc.wellTyped(), wellTy), pi.substBody(wellTy));
    });
  }

  protected static final class NotPi extends Exception {
    public final @NotNull Term actual;

    public NotPi(@NotNull Term actual) {
      this.actual = actual;
    }
  }

  private @NotNull PiTerm ensurePi(Term term) throws NotPi {
    if (term instanceof PiTerm pi) return pi;
    throw new NotPi(term);
  }
}
