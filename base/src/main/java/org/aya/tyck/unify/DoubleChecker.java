// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.unify;

import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.MetaCall;
import org.aya.syntax.core.term.xtt.DimTyTerm;
import org.aya.syntax.core.term.xtt.EqTerm;
import org.aya.syntax.ref.LocalCtx;
import org.aya.syntax.ref.MetaVar;
import org.aya.tyck.TyckState;
import org.aya.tyck.error.BadExprError;
import org.aya.tyck.tycker.ContextBased;
import org.aya.tyck.tycker.Problematic;
import org.aya.tyck.tycker.StateBased;
import org.aya.util.error.Panic;
import org.aya.util.reporter.Problem;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

public record DoubleChecker(
  @NotNull Unifier unifier,
  @NotNull Synthesizer synthesizer
) implements StateBased, ContextBased, Problematic {
  public DoubleChecker(@NotNull Unifier unifier) {
    this(unifier, new Synthesizer(unifier));
  }

  public boolean inheritTy(@NotNull Term ty, @NotNull SortTerm expected) {
    if (ty instanceof MetaCall meta && meta.ref().req() == MetaVar.Misc.IsType) {
      // TODO
    }

    if (!(synthesizer.trySynth(ty) instanceof SortTerm tyty)) return false;
    // TODO: check type
    throw new UnsupportedOperationException("TODO");
  }

  public boolean inherit(@NotNull Term preterm, @NotNull Term expected) {
    if (expected instanceof MetaCall) {
      // TODO: the original code seems useless (can be handled in default case)
    }

    return switch (preterm) {
      case ErrorTerm _ -> true;
      case PiTerm(var pParam, var pBody) -> {
        if (!(whnf(expected) instanceof SortTerm expectedTy)) yield Panic.unreachable();
        if (!inheritTy(pParam, expectedTy)) yield false;
        yield unifier.subscoped(() -> {
          var param = synthesizer.putIndex(pParam);
          return inherit(pBody.instantiate(param), expectedTy);
        });
      }
      case SigmaTerm sigma -> sigma.params().view()
        .allMatch(param -> inherit(param, expected));
      case TupTerm(var elems) when whnf(expected) instanceof SigmaTerm sigmaTy -> {
        // This is not an assertion because the input is not guaranteed to be well-typed
        if (!elems.sizeEquals(sigmaTy.params())) yield false;

        yield sigmaTy.check(elems, (elem, param) -> {
          if (inherit(whnf(elem), param)) return elem;
          return null;
        }).isOk();
      }
      case LamTerm(var body) -> subscoped(() -> switch (whnf(expected)) {
        case PiTerm(var dom, var cod) ->  {
          var param = synthesizer.putIndex(dom);
          yield inherit(body.instantiate(param), cod.instantiate(param));
        }
        case EqTerm eq -> {
          var param = synthesizer.putIndex(DimTyTerm.INSTANCE);
          yield inherit(body.instantiate(param), eq.A());
        }
        default -> failF(new BadExprError(preterm, unifier.pos, expected));
      });
      case TupTerm _ -> failF(new BadExprError(preterm, unifier.pos, expected));

      default -> unifier.compare(synthesizer.synthDontNormalize(preterm), expected, null);
    };
  }
  private boolean failF(@NotNull Problem problem) {
    fail(problem);
    return false;
  }
  @Override public @NotNull LocalCtx localCtx() {
    return unifier.localCtx();
  }
  @Override public @NotNull LocalCtx setLocalCtx(@NotNull LocalCtx ctx) {
    return unifier.setLocalCtx(ctx);
  }
  @Override public @NotNull TyckState state() {
    return unifier.state();
  }
  @Override public @NotNull Reporter reporter() {
    return unifier.reporter();
  }
}
