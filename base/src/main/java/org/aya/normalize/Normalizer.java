// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.normalize;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.immutable.ImmutableSet;
import kala.control.Either;
import kala.control.Option;
import kala.control.Result;
import org.aya.generic.Modifier;
import org.aya.syntax.compile.JitFn;
import org.aya.syntax.core.def.FnDef;
import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.*;
import org.aya.syntax.core.term.marker.BetaRedex;
import org.aya.syntax.core.term.marker.StableWHNF;
import org.aya.syntax.core.term.xtt.CoeTerm;
import org.aya.syntax.core.term.xtt.DimTerm;
import org.aya.syntax.literate.CodeOptions;
import org.aya.syntax.ref.AnyVar;
import org.aya.tyck.TyckState;
import org.aya.tyck.tycker.Stateful;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

import static org.aya.normalize.PatMatcher.State.Stuck;

/**
 * Unlike in pre-v0.30 Aya, we use only one normalizer, only doing head reduction,
 * and we merge conservative normalizer and the whnf normalizer.
 * <p>
 * Even though it has a field {@link #state}, do not make it extend {@link Stateful},
 * because there is a method called whnf in it, which clashes with the one here.
 */
public record Normalizer(@NotNull TyckState state, @NotNull ImmutableSet<AnyVar> opaque)
  implements UnaryOperator<Term> {
  public Normalizer(@NotNull TyckState state) {
    this(state, ImmutableSet.empty());
  }

  @Override public Term apply(Term term) { return whnf(term, false); }
  private @NotNull Term whnf(@NotNull Term term, boolean usePostTerm) {
    if (term instanceof StableWHNF || term instanceof FreeTerm) return term;
    var postTerm = term.descent(this);
    var defaultValue = usePostTerm ? postTerm : term;

    return switch (postTerm) {
      case StableWHNF _, FreeTerm _ -> Panic.unreachable();
      case BetaRedex app -> {
        var result = app.make();
        yield result == app ? result : whnf(result, usePostTerm);
      }
      case FnCall(var fn, int ulift, var args) -> switch (fn) {
        case JitFn instance -> {
          var result = instance.invoke(term, args);
          if (term != result) yield whnf(result.elevate(ulift), usePostTerm);
          yield result;
        }
        case FnDef.Delegate delegate -> {
          FnDef core = delegate.core();
          if (core == null) yield defaultValue;
          if (!isOpaque(core)) yield switch (core.body) {
            case Either.Left(var body) -> whnf(body.instantiateTele(args.view()), usePostTerm);
            case Either.Right(var clauses) -> {
              var result = tryUnfoldClauses(clauses, args, ulift, core.is(Modifier.Overlap));
              // we may get stuck
              if (result.isEmpty()) yield defaultValue;
              yield whnf(result.get(), usePostTerm);
            }
          };
          yield defaultValue;
        }
      };
      case RuleReducer reduceRule -> {
        var result = reduceRule.rule().apply(reduceRule.args());
        if (result != null) yield apply(result);
        // We can't handle it, try to delegate to FnCall
        yield reduceRule instanceof RuleReducer.Fn fnRule
          ? whnf(fnRule.toFnCall(), usePostTerm)
          : reduceRule;
      }
      case ConCall(var head, var args) when head.ref().hasEq() && args.getLast() instanceof DimTerm dim ->
        head.ref().equality(args, dim == DimTerm.I0);
      case PrimCall prim -> state.primFactory().unfold(prim, state);
      case MetaPatTerm metaTerm -> metaTerm.inline(this);
      case MetaCall meta -> state.computeSolution(meta, term1 -> whnf(term1, usePostTerm));
      case CoeTerm(var type, var r, var s) -> {
        if (r instanceof DimTerm || r instanceof FreeTerm) {
          if (r.equals(s)) yield new LamTerm(new LocalTerm(0));
        }
        yield defaultValue;
      }
      // TODO: handle other cases
      // ice: what are the other cases?
      // h: i don't know
      default -> defaultValue;
    };
  }

  private boolean isOpaque(@NotNull FnDef fn) {
    return opaque.contains(fn.ref) || fn.is(Modifier.Opaque);
  }

  public @NotNull Option<Term> tryUnfoldClauses(
    @NotNull ImmutableSeq<Term.Matching> clauses, @NotNull ImmutableSeq<Term> args,
    int ulift, boolean orderIndependent
  ) {
    for (var matchy : clauses) {
      var matcher = new PatMatcher(false, this);
      switch (matcher.apply(matchy.patterns(), args)) {
        case Result.Err(var st) -> {
          if (!orderIndependent && st == Stuck) return Option.none();
        }
        case Result.Ok(var subst) -> {
          return Option.some(matchy.body().elevate(ulift).instantiateTele(subst.view()));
        }
      }
    }
    return Option.none();
  }

  private class Full implements UnaryOperator<Term> {
    @Override public Term apply(Term term) { return whnf(term, true).descent(this); }
  }

  /**
   * Do NOT use this in the type checker.
   * This is for REPL/literate mode and testing.
   */
  public @NotNull Term normalize(Term term, CodeOptions.NormalizeMode mode) {
    return switch (mode) {
      case HEAD -> apply(term);
      case FULL -> new Full().apply(term);
      case NULL -> new Finalizer.Freeze(() -> state).zonk(term);
    };
  }
}
