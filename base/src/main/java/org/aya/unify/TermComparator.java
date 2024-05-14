// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.unify;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import org.aya.generic.NameGenerator;
import org.aya.generic.SortKind;
import org.aya.prettier.AyaPrettierOptions;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.TeleDef;
import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.*;
import org.aya.syntax.core.term.xtt.DimTerm;
import org.aya.syntax.core.term.xtt.DimTyTerm;
import org.aya.syntax.ref.DefVar;
import org.aya.syntax.ref.LocalCtx;
import org.aya.syntax.ref.LocalVar;
import org.aya.syntax.ref.MetaVar;
import org.aya.tyck.TyckState;
import org.aya.tyck.ctx.LocalLet;
import org.aya.tyck.error.LevelError;
import org.aya.tyck.tycker.AbstractTycker;
import org.aya.tyck.tycker.Contextful;
import org.aya.util.Ordering;
import org.aya.util.Pair;
import org.aya.util.error.Panic;
import org.aya.util.error.SourcePos;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public abstract sealed class TermComparator extends AbstractTycker permits Unifier {
  protected final @NotNull SourcePos pos;
  protected final @NotNull Ordering cmp;
  // If false, we refrain from solving meta, and return false if we encounter a non-identical meta.
  private boolean solveMeta = true;
  private @Nullable FailureData failure = null;
  private final @NotNull NameGenerator nameGen = new NameGenerator();

  public TermComparator(
    @NotNull TyckState state, @NotNull LocalCtx ctx, @NotNull LocalLet let,
    @NotNull Reporter reporter, @NotNull SourcePos pos, @NotNull Ordering cmp
  ) {
    super(state, ctx, reporter, let);
    this.pos = pos;
    this.cmp = cmp;
  }

  /**
   * Trying to solve {@param meta} with {@param rhs}
   *
   * @param rhs in whnf
   */
  protected abstract @Nullable Term doSolveMeta(@NotNull MetaCall meta, @NotNull Term rhs, @Nullable Term type);

  protected @Nullable Term solveMeta(@NotNull MetaCall meta, @NotNull Term rhs, @Nullable Term type) {
    if (!solveMeta) return null;
    return doSolveMeta(meta, whnf(rhs), type);
  }

  /// region Utilities

  private void fail(@NotNull Term lhs, @NotNull Term rhs) {
    if (failure == null) {
      failure = new FailureData(lhs, rhs);
    }
  }

  private @NotNull Panic noRules(@NotNull Term term) {
    return new Panic(STR."\{term.getClass()}: \{term.toDoc(AyaPrettierOptions.debug()).debugRender()}");
  }
  /// endregion Utilities

  /** Compare arguments ONLY. */
  private @Nullable Term compareApprox(@NotNull Term lhs, @NotNull Term rhs) {
    return switch (new Pair<>(lhs, rhs)) {
      case Pair(FnCall lFn, FnCall rFn) -> compareCallApprox(lFn, rFn, lFn.ref());
      case Pair(PrimCall lFn, PrimCall rFn) -> compareCallApprox(lFn, rFn, lFn.ref());
      case Pair(ConCallLike lCon, ConCallLike rCon) -> compareConApprox(lCon, rCon);
      default -> null;
    };
  }

  /**
   * Compare the arguments of two callable ONLY, this method will NOT try to normalize and then compare (while the old project does).
   */
  private @Nullable Term compareCallApprox(
    @NotNull Callable lhs, @NotNull Callable rhs,
    @NotNull DefVar<? extends TeleDef, ? extends TeleDecl<?>> typeProvider
  ) {
    if (lhs.ref() != rhs.ref()) return null;

    var retTy = new ErrorTerm(null);   // TODO: Synthesizer
    var argsTy = TeleDef.defTele(typeProvider).map(Param::type);

    if (compareMany(lhs.args(), rhs.args(), argsTy)) return retTy;
    return null;
  }

  private @Nullable Term compareConApprox(@NotNull ConCallLike lhs, @NotNull ConCallLike rhs) {
    if (lhs.ref() != rhs.ref()) return null;

    var retTy = TeleDef.defResult(lhs.ref());   // TODO: use synthesizer instead
    var dataArgsTy = TeleDef.defTele(lhs.head().dataRef()).map(Param::type);
    var conArgsTy = lhs.ref().core.selfTele.map(Param::type);
    // compare data args first
    if (!compareMany(lhs.head().dataArgs(), rhs.head().dataArgs(), dataArgsTy)) return null;
    if (!compareMany(lhs.conArgs(), rhs.conArgs(), conArgsTy)) return null;
    return retTy;
  }

  /**
   * Compare two terms with the given {@param type} (if not null)
   *
   * @return true if they are 'the same' under {@param type}, false otherwise.
   */
  public boolean compare(@NotNull Term preLhs, @NotNull Term preRhs, @Nullable Term type) {
    if (preLhs == preRhs) return true;
    if (compareApprox(preLhs, preRhs) != null) {
      // TODO: unify the result of {compareApprox} and {type}
      //       Same for below
      return true;
    }

    var lhs = whnf(preLhs);
    var rhs = whnf(preRhs);
    if ((!(lhs == preLhs && rhs == preRhs)) && compareApprox(lhs, rhs) != null) return true;

    if (rhs instanceof MetaCall rMeta) {
      // In case we're comparing two metas with one IsType and the other has OfType,
      // prefer solving the IsType one as the OfType one.
      if (lhs instanceof MetaCall lMeta && lMeta.ref().req() == MetaVar.Misc.IsType)
        return solveMeta(lMeta, rMeta, type) != null;
      return solveMeta(rMeta, lhs, type) != null;
    }
    // ^ Beware of the order!!
    if (lhs instanceof MetaCall lMeta) {
      return solveMeta(lMeta, rhs, type) != null;
    } else if (type == null) {
      return compareUntyped(lhs, rhs) != null;
    }

    var result = doCompareTyped(preLhs, preRhs, type);
    if (!result) fail(lhs, rhs);
    return result;
  }

  /**
   * Compare whnf {@param lhs} and whnf {@param rhs} with {@param type} information
   *
   * @param type the whnf type.
   * @return whether they are 'the same' and their types are {@param type}
   */
  private boolean doCompareTyped(@NotNull Term lhs, @NotNull Term rhs, @NotNull Term type) {
    return switch (type) {
      // TODO: ClassCall
      case LamTerm _ -> throw new Panic("LamTerm is never type");
      case ConCallLike _ -> throw new Panic("ConCall is never type");
      case TupTerm _ -> throw new Panic("TupTerm is never type");
      case ErrorTerm _ -> true;
      case PiTerm pi -> switch (new Pair<>(lhs, rhs)) {
        case Pair(LamTerm(var lbody), LamTerm(var rbody)) -> subscoped(() -> {
          var var = putIndex(pi.param());
          return compare(
            lbody.instantiate(var),
            rbody.instantiate(var),
            pi.body().instantiate(var)
          );
        });
        case Pair(LamTerm lambda, _) -> compareLambda(lambda, rhs, pi);
        case Pair(_, LamTerm rambda) -> compareLambda(rambda, lhs, pi);
        default -> false;
      };
      case SigmaTerm(var paramSeq) -> {
        var size = paramSeq.size();
        var list = ImmutableSeq.fill(size, i -> ProjTerm.make(lhs, i));
        var rist = ImmutableSeq.fill(size, i -> ProjTerm.make(rhs, i));

        yield compareMany(list, rist, paramSeq);
      }
      default -> compareUntyped(lhs, rhs) != null;
    };
  }

  /**
   * Compare whnfed {@param preLhs} and whnfed {@param preRhs} without type information.
   *
   * @return the whnfed type of {@param preLhs} and {@param preRhs} if they are 'the same', null otherwise.
   */
  private @Nullable Term compareUntyped(@NotNull Term preLhs, @NotNull Term preRhs) {
    {
      var result = compareApprox(preLhs, preRhs);
      if (result != null) return result;
    }

    var lhs = whnf(preLhs);
    var rhs = whnf(preRhs);
    if (!(lhs == preLhs && rhs == preRhs)) {
      var result = compareApprox(lhs, rhs);
      if (result != null) return result;
    }

    var result = doCompareUntyped(lhs, rhs);
    if (result != null) return whnf(result);
    fail(lhs, rhs);
    return null;
  }

  private @Nullable Term doCompareUntyped(@NotNull Term lhs, @NotNull Term rhs) {
    // TODO: return correct type level
    if (lhs instanceof Formation form) return doCompareType(form, rhs) ? SortTerm.Set0 : null;
    return switch (lhs) {
      case AppTerm(var f, var a) -> {
        if (!(rhs instanceof AppTerm(var g, var b))) yield null;
        var fTy = compareUntyped(f, g);
        if (!(fTy instanceof PiTerm pi)) yield null;
        if (!compare(a, b, pi.param())) yield null;
        yield pi.body().instantiate(a);
      }
      case ProjTerm(var lof, var ldx) -> {
        // Since {lhs} and {rhs} are whnf, at this point, {lof} is unable to evaluate.
        // Thus the only thing we can do is check whether {lof} and {rhs.of(}} (if rhs is ProjTerm) are 'the same'.
        if (!(rhs instanceof ProjTerm(var rof, var rdx))) yield null;
        if (!(compareUntyped(lof, rof) instanceof SigmaTerm(var params))) yield null;
        if (ldx != rdx) yield null;
        // Make type
        var spine = ImmutableSeq.fill(ldx /* ldx is 0-based */, i -> ProjTerm.make(lof, i));    // 0 = lof.0, 1 = lof.1, ...
        // however, for {lof.ldx}, the nearest(0) element is {lof.(idx - 1)}, so we need to reverse the spine.
        yield params.get(ldx).instantiateTele(spine.view());
      }
      case FreeTerm(var lvar) -> rhs instanceof FreeTerm(var rvar) && lvar == rvar ? localCtx().get(lvar) : null;
      case DimTerm l -> rhs instanceof DimTerm r && l == r ? l : null;
      case MetaCall mCall -> solveMeta(mCall, rhs, null);
      // We already compare arguments in compareApprox, if we arrive here,
      // it means their arguments don't match (even the ref don't match),
      // so we are unable to do more if we can't normalize them.
      case FnCall _ -> null;

      default -> throw noRules(lhs);
    };
  }

  /**
   * Compare {@param lambda} and {@param rhs} with {@param type}
   */
  private boolean compareLambda(@NotNull LamTerm lambda, @NotNull Term rhs, @NotNull PiTerm type) {
    return subscoped(() -> {
      var var = putIndex(type.param());
      var lhsBody = lambda.body().instantiate(var);
      var rhsBody = AppTerm.make(rhs, new FreeTerm(var));
      return compare(lhsBody, rhsBody, type.body().instantiate(var));
    });
  }

  private boolean compareMany(
    @NotNull ImmutableSeq<Term> list,
    @NotNull ImmutableSeq<Term> rist,
    @NotNull ImmutableSeq<Term> types
  ) {
    assert list.sizeEquals(rist);
    assert rist.sizeEquals(types);

    var typeView = types.view();
    for (var i = 0; i < list.size(); ++i) {
      var l = list.get(i);
      var r = rist.get(i);
      var ty = typeView.getFirst();
      if (!compare(l, r, ty)) return false;
      typeView = typeView.drop(1).mapIndexed((j, x) -> x.replace(j, l));
    }

    return true;
  }

  /**
   * Compare {@param lTy} and {@param rTy}
   *
   * @param continuation invoked with {@code ? : lTy} in {@link Contextful#localCtx()} if {@param lTy} is the 'same' as {@param rTy}
   */
  private <R> R compareTypeWith(
    @NotNull Term lTy,
    @NotNull Term rTy,
    @NotNull Supplier<R> onFailed,
    @NotNull Function<LocalVar, R> continuation
  ) {
    if (!compare(lTy, rTy, null)) return onFailed.get();
    return subscoped(() -> {
      var name = putParam(new Param(nameGen.next(whnf(lTy)), lTy, true));
      return continuation.apply(name);
    });
  }

  private <R> R compareTypesWithAux(
    @NotNull SeqView<LocalVar> vars,
    @NotNull ImmutableSeq<Term> list,
    @NotNull ImmutableSeq<Term> rist,
    @NotNull Supplier<R> onFailed,
    @NotNull Function<ImmutableSeq<LocalVar>, R> continuation
  ) {
    if (!list.sizeEquals(rist)) return onFailed.get();
    if (list.isEmpty()) return continuation.apply(vars.toImmutableSeq());
    return compareTypeWith(
      list.getFirst().instantiateTeleVar(vars),
      rist.getFirst().instantiateTeleVar(vars), onFailed, var ->
        compareTypesWithAux(vars.appended(var), list.drop(1), rist.drop(1), onFailed, continuation));
  }

  /**
   * Compare types and run the {@param continuation} with those types in context (reverse order).
   *
   * @param onFailed     run while failed (size doesn't match or compare failed)
   * @param continuation a function that accept the {@link LocalVar} of all {@param list}
   */
  private <R> R compareTypesWith(
    @NotNull ImmutableSeq<Term> list,
    @NotNull ImmutableSeq<Term> rist,
    @NotNull Supplier<R> onFailed,
    @NotNull Function<ImmutableSeq<LocalVar>, R> continuation
  ) {
    return subscoped(() -> compareTypesWithAux(SeqView.empty(), list, rist, onFailed, continuation));
  }

  private boolean sortLt(@NotNull SortTerm l, @NotNull SortTerm r) {
    var lift = l.lift();
    var rift = r.lift();
    // ISet <= Set0
    // Set i <= Set j if i <= j
    // Type i <= Type j if i <= j
    return switch (l.kind()) {
      case Type -> r.kind() == SortKind.Type && lift <= rift;
      case ISet -> r.kind() == SortKind.Set || r.kind() == SortKind.ISet;
      case Set -> r.kind() == SortKind.Set && lift <= rift;
    };
  }

  private boolean compareSort(@NotNull SortTerm l, @NotNull SortTerm r) {
    return switch (cmp) {
      case Gt -> {
        if (!sortLt(r, l)) {
          fail(new LevelError(pos, l, r, false));
          yield false;
        } else yield true;
      }
      case Eq -> {
        if (!(l.kind() == r.kind() && l.lift() == r.lift())) {
          fail(new LevelError(pos, l, r, true));
          yield false;
        } else yield true;
      }
      case Lt -> {
        if (!sortLt(l, r)) {
          fail(new LevelError(pos, r, l, false));
          yield false;
        } else yield true;
      }
    };
  }

  /**
   * Compare two type formation
   * Note: don't confuse with {@link TermComparator#doCompareTyped(Term, Term, Term)}
   */
  private boolean doCompareType(@NotNull Formation preLhs, @NotNull Term preRhs) {
    if (preLhs.getClass() != preRhs.getClass()) return false;
    return switch (new Pair<>(preLhs, (Formation) preRhs)) {
      case Pair(DataCall lhs, DataCall rhs) -> {
        if (lhs.ref() != rhs.ref()) yield false;
        yield compareMany(lhs.args(), rhs.args(), TeleDef.defTele(lhs.ref())
          .map(x -> x.type().elevate(lhs.ulift())));
      }
      case Pair(DimTyTerm _, DimTyTerm _) -> true;
      case Pair(PiTerm lhs, PiTerm rhs) -> compareTypeWith(lhs.param(), rhs.param(), () -> false, var ->
        compare(lhs.body().instantiate(var),
          rhs.body().instantiate(var),
          null));
      case Pair(SigmaTerm lhs, SigmaTerm rhs) -> compareTypesWith(lhs.params(), rhs.params(), () -> false, _ -> true);
      case Pair(SortTerm lhs, SortTerm rhs) -> compareSort(lhs, rhs);
      default -> throw noRules(preLhs);
    };
  }

  private @NotNull LocalVar putParam(@NotNull Param param) {
    var var = new LocalVar(param.name());
    localCtx().put(var, param.type());
    return var;
  }

  public @NotNull LocalVar putIndex(@NotNull Term term) {
    return super.putIndex(nameGen, term);
  }

  public @NotNull FailureData getFailure() {
    var failure = this.failure;
    assert failure != null;
    return failure.map(this::freezeHoles);
  }

  public record FailureData(@NotNull Term lhs, @NotNull Term rhs) {
    public @NotNull FailureData map(@NotNull UnaryOperator<Term> f) {
      return new FailureData(f.apply(lhs), f.apply(rhs));
    }
  }

  public boolean checkEqn(@NotNull TyckState.Eqn eqn) {
    return compare(eqn.lhs(), eqn.rhs(), null);
  }
}
