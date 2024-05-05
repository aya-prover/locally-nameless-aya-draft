// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.unify;

import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.MetaCall;
import org.aya.syntax.ref.MetaVar;
import org.aya.tyck.TyckState;
import org.aya.tyck.error.BadExprError;
import org.aya.tyck.tycker.Problematic;
import org.aya.tyck.tycker.StateBased;
import org.aya.util.error.Panic;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

// TODO: unifier and synthesizer use something that look similar, i.e. TyckState and LocalCtx
public record DoubleChecker(
  @NotNull Unifier unifier,
  @NotNull Synthesizer synthesizer
) implements StateBased, Problematic {
  @Override
  public @NotNull TyckState state() {
    // TODO: which state we should use?
    return unifier.state();
  }

  @Override
  public @NotNull Reporter reporter() {
    return unifier.reporter();
  }

  public boolean inheritTy(@NotNull Term ty, @NotNull SortTerm expected) {
    if (ty instanceof MetaCall meta && meta.ref().req() == MetaVar.Misc.IsType) {
      // TODO
    }

    if (!(synthesizer.trySynthesizeWHNF(ty) instanceof SortTerm tyty)) return false;
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
          var param = unifier.putIndex(pParam);
          return inherit(pBody.instantiate(param), expectedTy);
        });
      }
      case SigmaTerm sigma -> throw new UnsupportedOperationException("TODO");
      case TupTerm(var elems) when whnf(expected) instanceof SigmaTerm sigmaTy -> {
        // TODO: assertion?
        if (!elems.sizeEquals(sigmaTy.params())) yield false;

        yield sigmaTy.check(elems, (elem, param) -> {
          if (inherit(whnf(elem), param)) return elem;
          return null;
        }) != null;
      }
      case LamTerm(var body) when whnf(expected) instanceof PiTerm(var pParam, var pLast) -> unifier.subscoped(() -> {
        var param = unifier.putIndex(pParam);
        return inherit(body.instantiate(param), pLast.instantiate(param));
      });
      case TupTerm _, LamTerm _ -> {
        fail(new BadExprError(preterm, unifier.pos, expected));
        yield false;
      }

      // TODO: @ice1000 more
      default -> unifier.compare(synthesizer.synthesizeWHNF(preterm), expected, null);
    };
  }
}
