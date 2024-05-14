// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableTreeSet;
import org.aya.generic.Constants;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.MetaCall;
import org.aya.syntax.core.term.xtt.DimTyTerm;
import org.aya.syntax.core.term.xtt.EqTerm;
import org.aya.syntax.core.term.xtt.PAppTerm;
import org.aya.syntax.ref.*;
import org.aya.tyck.ctx.LocalSubstitution;
import org.aya.tyck.error.*;
import org.aya.tyck.tycker.AbstractTycker;
import org.aya.tyck.tycker.AppTycker;
import org.aya.tyck.tycker.LocalDeful;
import org.aya.tyck.tycker.Unifiable;
import org.aya.unify.TermComparator;
import org.aya.unify.Unifier;
import org.aya.util.Ordering;
import org.aya.util.error.Panic;
import org.aya.util.error.SourceNode;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ExprTycker extends AbstractTycker implements Unifiable, LocalDeful {
  public final @NotNull MutableTreeSet<WithPos<Expr.WithTerm>> withTerms =
    MutableTreeSet.create(Comparator.comparing(SourceNode::sourcePos));
  private @NotNull LocalSubstitution localDefinitions;

  public void addWithTerm(@NotNull Expr.WithTerm with, @NotNull SourcePos pos, @NotNull Term type) {
    withTerms.add(new WithPos<>(pos, with));
    with.theCoreType().set(type);
  }

  public ExprTycker(
    @NotNull TyckState state,
    @NotNull LocalCtx ctx,
    @NotNull LocalSubstitution localDefinitions,
    @NotNull Reporter reporter
  ) {
    super(state, ctx, reporter);
    this.localDefinitions = localDefinitions;
  }

  public void solveMetas() {
    state.solveMetas(reporter);
    withTerms.forEach(with -> with.data().theCoreType().update(this::freezeHoles));
  }

  /**
   * @param type may not be in whnf, because we want unnormalized type to be used for unification.
   */
  public @NotNull Jdg inherit(@NotNull WithPos<Expr> expr, @NotNull Term type) {
    return switch (expr.data()) {
      case Expr.Lambda(var ref, var body) -> switch (type) {
        case PiTerm(Term dom, Term cod) -> {
          // unifyTyReported(param, dom, expr);
          var core = subscoped(() -> {
            localCtx().put(ref, dom);
            return inherit(body, cod.instantiate(new FreeTerm(ref))).bind(ref);
          });
          yield new Jdg.Default(new LamTerm(core.wellTyped()), type);
        }
        case EqTerm eq -> {
          var core = subscoped(() -> {
            localCtx().put(ref, DimTyTerm.INSTANCE);
            var coreBody = inherit(body, eq.b()).bind(ref);
            // TODO: check boundaries
            return coreBody.wellTyped();
          });
          yield new Jdg.Default(new LamTerm(core), eq);
        }
        default -> fail(expr.data(), type, BadTypeError.pi(state, expr, type));
      };
      case Expr.Hole hole -> {
        var freshHole = freshMeta(Constants.randomName(hole), expr.sourcePos(), new MetaVar.OfType(type));
        if (hole.explicit()) reporter.report(new Goal(state, freshHole, hole.accessibleLocal().get()));
        yield new Jdg.Default(freshHole, type);
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

        yield new Jdg.Default(wellTyped, sigmaTerm);
      }
      case Expr.Let let -> checkLet(let, e -> inherit(e, type));
      default -> {
        var syn = synthesize(expr);
        unifyTyReported(type, syn.type(), expr);
        yield syn;
      }
    };
  }

  public @NotNull Term ty(@NotNull WithPos<Expr> expr) {
    return switch (expr.data()) {
      case Expr.Sort sort -> new SortTerm(sort.kind(), sort.lift());
      case Expr.Pi(var param, var last) -> {
        var wellParam = ty(param.typeExpr());
        addWithTerm(param, param.sourcePos(),  wellParam);
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
      case Expr.Let let -> checkLet(let, e -> lazyJdg(ty(e))).wellTyped();
      default -> synthesize(expr).wellTyped();
    };
  }

  public @NotNull Jdg.Sort sort(@NotNull WithPos<Expr> expr) {
    return new Jdg.Sort(sort(expr, ty(expr)));
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

  public @NotNull Jdg synthesize(@NotNull WithPos<Expr> expr) {
    var result = doSynthesize(expr);
    if (expr.data() instanceof Expr.WithTerm with) {
      addWithTerm(with, expr.sourcePos(), result.type());
    }
    return result;
  }

  public @NotNull Jdg doSynthesize(@NotNull WithPos<Expr> expr) {
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
      case Expr.Ref(var ref) -> {
        if (ref instanceof LocalVar localRef && localDefinitions.contains(localRef)) {
          yield localDefinitions.get(localRef);
        }

        yield checkApplication(ref, expr.sourcePos(), ImmutableSeq.empty());
      }
      case Expr.Sigma _, Expr.Pi _ -> lazyJdg(ty(expr));
      case Expr.Sort _ -> sort(expr);
      case Expr.Tuple(var items) -> {
        var results = items.map(this::synthesize);
        var wellTypeds = results.map(Jdg::wellTyped);
        var tys = results.map(Jdg::type);
        var wellTyped = new TupTerm(wellTypeds);
        var ty = new SigmaTerm(tys);

        yield new Jdg.Default(wellTyped, ty);
      }
      case Expr.Let let -> checkLet(let, this::synthesize);
      case Expr.Array array -> throw new UnsupportedOperationException("TODO");
      case Expr.Unresolved _ -> throw new UnsupportedOperationException("?");
      case Expr.Error error -> throw new Panic("Expr.Error");
      default -> fail(expr.data(), new NoRuleError(expr.data(), expr.sourcePos(), null));
    };
  }

  private @NotNull Jdg checkApplication(
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

  private @NotNull Jdg doCheckApplication(
    @NotNull AnyVar f, @NotNull ImmutableSeq<Expr.NamedArg> args
  ) throws NotPi {
    return switch (f) {
      case LocalVar lVar -> generateApplication(args,
        new Jdg.Default(new FreeTerm(lVar), localCtx().get(lVar)));
      case DefVar<?, ?> defVar -> AppTycker.checkDefApplication(defVar, (params, k) -> {
        int argIx = 0, paramIx = 0;
        var result = MutableList.<Term>create();
        while (argIx < args.size() && paramIx < params.size()) {
          var arg = args.get(argIx);
          var param = params.get(paramIx).descent(t -> t.instantiateTele(result.view()));
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

  private Jdg generateApplication(@NotNull ImmutableSeq<Expr.NamedArg> args, Jdg start) throws NotPi {
    return args.foldLeftChecked(start, (acc, arg) -> {
      if (arg.name() != null || !arg.explicit()) fail(new LicitError.BadNamedArg(arg));
      switch (acc.type()) {
        case PiTerm(var param, var body) -> {
          var wellTy = inherit(arg.arg(), param).wellTyped();
          return new Jdg.Default(AppTerm.make(acc.wellTyped(), wellTy), body.instantiate(wellTy));
        }
        case EqTerm(Term A, Term a, Term b) -> {
          var wellTy = inherit(arg.arg(), DimTyTerm.INSTANCE).wellTyped();
          return new Jdg.Default(new PAppTerm(acc.wellTyped(), wellTy, a, b).make(), A.instantiate(wellTy));
        }
        default -> throw new NotPi(acc.type());
      }
    });
  }

  /**
   * tyck a let expr with the given checker
   *
   * @param checker check the type of the body of {@param let}
   */
  private @NotNull Jdg checkLet(@NotNull Expr.Let let, @NotNull Function<WithPos<Expr>, Jdg> checker) {
    // pushing telescopes into lambda params, for example:
    // `let f (x : A) : B x` is desugared to `let f : Pi (x : A) -> B x`
    var letBind = let.bind();
    var typeExpr = Expr.buildPi(letBind.sourcePos(),
      letBind.telescope().view(), letBind.result());
    // as well as the body of the binding, for example:
    // `let f x := g` is desugared to `let f := \x => g`
    var definedAsExpr = Expr.buildLam(letBind.sourcePos(),
      letBind.telescope().view(), letBind.definedAs());

    // Now everything is in form `let f : G := g in h`

    var type = freezeHoles(ty(typeExpr));
    var definedAsResult = inherit(definedAsExpr, type);

    return subscoped(() -> {
      localDefinitions.put(let.bind().bindName(), definedAsResult);
      return checker.apply(let.body());
    });
  }

  /// region Overrides

  @Override public @NotNull TermComparator unifier(@NotNull SourcePos pos, @NotNull Ordering order) {
    return new Unifier(state(), localCtx(), reporter(), pos, order, true);
    // TODO: allowDelay?
  }

  @Override public @NotNull LocalSubstitution localDefinition() { return localDefinitions; }

  @Override public @NotNull LocalSubstitution setLocalDefinition(@NotNull LocalSubstitution newOne) {
    var old = localDefinitions;
    this.localDefinitions = newOne;
    return old;
  }

  @Override public <R> R subscoped(@NotNull Supplier<R> action) {
    return super.subscoped(() -> LocalDeful.super.subscoped(action));
  }

  /// endregion Overrides

  protected static final class NotPi extends Exception {
    public final @NotNull Term actual;

    public NotPi(@NotNull Term actual) { this.actual = actual; }
  }
}
