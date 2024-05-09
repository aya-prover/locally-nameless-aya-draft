// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import org.aya.generic.Constants;
import org.aya.generic.SortKind;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.MetaCall;
import org.aya.syntax.core.term.xtt.DimTyTerm;
import org.aya.syntax.core.term.xtt.EqTerm;
import org.aya.syntax.core.term.xtt.PAppTerm;
import org.aya.syntax.ref.*;
import org.aya.tyck.error.*;
import org.aya.tyck.tycker.AbstractTycker;
import org.aya.tyck.tycker.AppTycker;
import org.aya.tyck.tycker.Unifiable;
import org.aya.unify.TermComparator;
import org.aya.unify.Unifier;
import org.aya.util.Ordering;
import org.aya.util.error.Panic;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

public final class ExprTycker extends AbstractTycker implements Unifiable {
  public ExprTycker(
    @NotNull TyckState state,
    @NotNull LocalCtx ctx,
    @NotNull Reporter reporter
  ) {
    super(state, ctx, reporter);
  }

  /**
   * @param type may not be in whnf, because we want unnormalized type to be used for unification.
   */
  public @NotNull Result inherit(@NotNull WithPos<Expr> expr, @NotNull Term type) {
    return switch (expr.data()) {
      case Expr.Lambda(var ref, var body) -> switch (type) {
        case PiTerm(Term dom, Term cod) -> {
          // unifyTyReported(param, dom, expr);
          var core = subscoped(() -> {
            localCtx().put(ref, dom);
            return inherit(body, cod.instantiate(new FreeTerm(ref))).bind(ref);
          });
          yield new Result.Default(new LamTerm(core.wellTyped()), type);
        }
        case EqTerm eq -> {
          var core = subscoped(() -> {
            localCtx().put(ref, DimTyTerm.INSTANCE);
            var coreBody = inherit(body, eq.b()).bind(ref);
            // TODO: check boundaries
            return coreBody.wellTyped();
          });
          yield new Result.Default(new LamTerm(core), eq);
        }
        default -> fail(expr.data(), type, BadTypeError.pi(state, expr, type));
      };
      case Expr.Hole hole -> {
        var freshHole = freshMeta(Constants.randomName(hole), expr.sourcePos(), new MetaVar.OfType(type));
        if (hole.explicit()) reporter.report(new Goal(state, freshHole, hole.accessibleLocal().get()));
        yield new Result.Default(freshHole, type);
      }
      case Expr.Tuple(var elems) when type instanceof SigmaTerm sigmaTerm -> {
        var result = sigmaTerm.check(elems, (elem, ty) -> inherit(elem, ty).wellTyped());
        Term wellTyped;

        if (result.isErr()) {
          switch (result.getErr()) {
            case TooManyElement, TooManyParameter ->
              fail(new TupleError.ElemMismatchError(expr.sourcePos(), sigmaTerm.params().size(), elems.size()));
            case CheckFailed -> Panic.unreachable();
          }

          wellTyped = new ErrorTerm(expr.data());
        } else {
          wellTyped = new TupTerm(result.get());
        }

        yield new Result.Default(wellTyped, sigmaTerm);
      }
      default -> {
        var syn = synthesize(expr);
        unifyTyReported(syn.type(), type, expr);
        yield syn;
      }
    };
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
      case MetaCall hole -> {
        unifyTyReported(hole, SortTerm.Type0, errorMsg);
        yield SortTerm.Type0;
      }
      default -> {
        fail(BadTypeError.univ(state, errorMsg, term));
        yield SortTerm.Type0;
      }
    };
  }

  /**
   * Make sure you handle all possible types, that is, if you call {@link #ty(WithPos)} in {@link #synthesize(WithPos)}
   * with some condition {@code C}, then you have to handle {@code C} in this method.
   */
  private @NotNull Term doTy(@NotNull WithPos<Expr> expr) {
    return switch (expr.data()) {
      case Expr.Sort sort -> new SortTerm(sort.kind(), sort.lift());
      case Expr.Pi(var param, var last) -> {
        var wellParam = ty(param.typeExpr());
        yield subscoped(() -> {
          localCtx().put(param.ref(), wellParam);
          var wellLast = ty(last).bind(param.ref());
          return new PiTerm(wellParam, wellLast);
        });
      }
      case Expr.Sigma(var elems) -> subscoped(() -> {
        var tele = MutableList.<LocalVar>create();
        return new SigmaTerm(elems.map(elem -> {
          var result = ty(elem.typeExpr());
          var boundResult = result.bindTele(tele.view());
          localCtx().put(elem.ref(), result);
          tele.append(elem.ref());
          return boundResult;
        }));
      });
      default -> synthesize(expr).wellTyped();
    };
  }

  public @NotNull Result synthesize(@NotNull WithPos<Expr> expr) {
    return doSynthesize(expr);
  }

  private @NotNull Result doSynthesize(@NotNull WithPos<Expr> expr) {
    return switch (expr.data()) {
      case Expr.Sugar s ->
        throw new IllegalArgumentException(STR."\{s.getClass()} is desugared, should be unreachable");
      case Expr.App(var f, var a) -> {
        if (!(f.data() instanceof Expr.Ref(var ref))) throw new IllegalStateException("function must be Expr.Ref");
        yield checkApplication(ref, expr.sourcePos(), a);
      }
      case Expr.Hole hole -> throw new UnsupportedOperationException("TODO");
      case Expr.Lambda lam -> inherit(expr, generatePi(lam, expr.sourcePos()));
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
      case Expr.Sort sort -> sort(expr);
      case Expr.Tuple(var items) -> {
        var results = items.map(this::synthesize);
        var wellTypeds = results.map(Result::wellTyped);
        var tys = results.map(Result::type);
        var wellTyped = new TupTerm(wellTypeds);
        var ty = new SigmaTerm(tys);

        yield new Result.Default(wellTyped, ty);
      }
      case Expr.Let let -> throw new UnsupportedOperationException("TODO");
      case Expr.Array array -> throw new UnsupportedOperationException("TODO");
      case Expr.Unresolved _ -> throw new UnsupportedOperationException("?");
      case Expr.Error error -> throw new Panic("Expr.Error");
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
      return fail(expr, BadTypeError.pi(state, new WithPos<>(sourcePos, expr), notPi.actual));
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
              fail(new LicitError.BadImplicitArg(arg));
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
                fail(new LicitError.BadImplicitArg(arg));
                break;
              }
            }
          }
          result.append(inherit(arg.arg(), param.type()).wellTyped());
          argIx++;
          paramIx++;
        }
        if (argIx < args.size()) {
          return generateApplication(args.drop(argIx), k.apply(result.toImmutableSeq()));
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
      if (arg.name() != null || !arg.explicit()) fail(new LicitError.BadNamedArg(arg));
      switch (acc.type()) {
        case PiTerm(var param, var body) -> {
          var wellTy = inherit(arg.arg(), param).wellTyped();
          return new Result.Default(AppTerm.make(acc.wellTyped(), wellTy), body.instantiate(wellTy));
        }
        case EqTerm(Term A, Term a, Term b) -> {
          var wellTy = inherit(arg.arg(), DimTyTerm.INSTANCE).wellTyped();
          return new Result.Default(new PAppTerm(acc.wellTyped(), wellTy, a, b).make(), A.instantiate(wellTy));
        }
        default -> throw new NotPi(acc.type());
      }
    });
  }

  @Override
  public @NotNull TermComparator unifier(@NotNull SourcePos pos, @NotNull Ordering order) {
    return new Unifier(state(), localCtx(), reporter(), pos, order, true);    // TODO: allowDelay?
  }

  protected static final class NotPi extends Exception {
    public final @NotNull Term actual;

    public NotPi(@NotNull Term actual) {
      this.actual = actual;
    }
  }

}
