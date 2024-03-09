// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.unify;

import kala.collection.immutable.ImmutableSeq;
import org.aya.generic.SortKind;
import org.aya.syntax.core.def.TeleDef;
import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.ConCallLike;
import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.core.term.call.FnCall;
import org.aya.tyck.error.LevelError;
import org.aya.tyck.tycker.Problematic;
import org.aya.tyck.tycker.StateBased;
import org.aya.util.Ordering;
import org.aya.util.Pair;
import org.aya.util.error.InternalException;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public abstract class TermComparator implements StateBased, Problematic {
  protected final @NotNull SourcePos pos;
  protected final @NotNull Ordering cmp;
  // If false, we refrain from solving meta, and return false if we encounter a non-identical meta.
  protected boolean solveMeta = true;
  private FailureData failure;

  public TermComparator(@NotNull SourcePos pos, @NotNull Ordering cmp) {
    this.pos = pos;
    this.cmp = cmp;
  }

  public boolean compare(@NotNull Term lhs, @NotNull Term rhs, @Nullable Term type) {
    // TODO
    if (type == null) return compareUntyped(lhs, rhs) != null;
    return doCompareTyped(lhs, rhs, type);
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
      case LamTerm _ -> throw new InternalException("LamTerm is never type");
      case ConCallLike _ -> throw new InternalException("ConCall is never type");
      case TupTerm _ -> throw new InternalException("TupTerm is never type");
      case ErrorTerm _ -> true;
      case PiTerm pi -> switch (new Pair<>(lhs, rhs)) {
        case Pair(LamTerm(var lbody), LamTerm(var rbody)) -> state().dCtx().with(pi.param(),
          () -> compare(lbody, rbody, pi.body()));
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
   * Compare whnf {@param lhs} and whnf {@param rhs} without type information.
   *
   * @return the type of {@param lhs} and {@param rhs} if they are 'the same', null otherwise.
   */
  private @Nullable Term compareUntyped(@NotNull Term lhs, @NotNull Term rhs) {
    // TODO
    return doCompareUntyped(lhs, rhs);
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
        // Since the {lhs} and {rhs} are whnf, at this point, {lof} is unable to evaluate.
        // Thus the only thing we can do is check whether {lof} and {rhs.of(}} (if rhs is ProjTerm) is 'the same'.
        if (!(rhs instanceof ProjTerm(var rof, var rdx))) yield null;
        if (!(compareUntyped(lof, rof) instanceof SigmaTerm(var params))) yield null;
        if (ldx != rdx) yield null;
        // Make type
        var spine = ImmutableSeq.fill(ldx /* ldx is 0-based */, i -> ProjTerm.make(lof, i));    // 0 = lof.0, 1 = lof.1, ...
        // however, for {lof.ldx}, the nearest(0) element is {lof.(idx - 1)}, so we need to reverse the spine.
        yield params.get(ldx).instantiateAll(spine.view().reversed());
      }
      case FreeTerm(var lvar) -> {
        if (rhs instanceof FreeTerm(var rvar) && lvar == rvar) yield state().ctx().get(lvar);
        yield null;
      }
      case LocalTerm(var ldx) -> {
        if (rhs instanceof LocalTerm(var rdx) && ldx == rdx) yield state().dCtx().get(ldx);
        yield null;
      }
      case FnCall _ -> throw new InternalException("FnCall is compared in compareApprox");
      default -> throw new UnsupportedOperationException("TODO");
    };
  }

  /**
   * Compare {@param lambda} and {@param rhs} with {@param type}
   */
  private boolean compareLambda(@NotNull LamTerm lambda, @NotNull Term rhs, @NotNull PiTerm type) {
    return state().dCtx().with(type.param(), () -> {
      // 0 : type.param()
      var lhsBody = lambda.body();
      var rhsBody = AppTerm.make(rhs, new LocalTerm(0));
      return compare(lhsBody, rhsBody, type.body());
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

  private <R> R compareTypeWith(
    @NotNull Term lTy,
    @NotNull Term rTy,
    @NotNull Supplier<R> onFailed,
    @NotNull Supplier<R> continuation
  ) {
    if (!compare(lTy, rTy, null)) return onFailed.get();
    return state().dCtx().with(lTy, continuation);
  }

  /**
   * Compare types and run the {@param continuation} with those types in context (reverse order).
   * @param onFailed run while failed (size doesn't match or compare failed)
   */
  private <R> R compareTypesWith(
    @NotNull ImmutableSeq<Term> list,
    @NotNull ImmutableSeq<Term> rist,
    @NotNull Supplier<R> onFailed,
    @NotNull Supplier<R> continuation
  ) {
    if (! list.sizeEquals(rist)) return onFailed.get();
    if (list.isEmpty()) return continuation.get();
    return compareTypeWith(list.getFirst(), rist.getFirst(), onFailed, () ->
      compareTypesWith(list.drop(1), rist.drop(1), onFailed, continuation));
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
        if (! sortLt(r, l)) {
          reporter().report(new LevelError(pos, l, r, false));
          yield false;
        } else yield true;
      }
      case Eq -> {
        if (! (l.kind() == r.kind() && l.lift() == r.lift())) {
          reporter().report(new LevelError(pos, l, r, true));
          yield false;
        } else yield true;
      }
      case Lt -> {
        if (! sortLt(l, r)) {
          reporter().report(new LevelError(pos, r, l, false));
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
      case Pair(PiTerm lhs, PiTerm rhs) -> compareTypeWith(lhs.param(), rhs.param(), () -> false, () ->
        compare(lhs.body(), rhs.body(), null));
      case Pair(SigmaTerm lhs, SigmaTerm rhs) -> compareTypesWith(lhs.params(), rhs.params(), () -> false, () -> true);
      case Pair(SortTerm lhs, SortTerm rhs) -> compareSort(lhs, rhs);
      default -> false;
    };
  }

  public record FailureData(@NotNull Term lhs, @NotNull Term rhs) {
    public @NotNull FailureData map(@NotNull UnaryOperator<Term> f) {
      return new FailureData(f.apply(lhs), f.apply(rhs));
    }
  }
}
