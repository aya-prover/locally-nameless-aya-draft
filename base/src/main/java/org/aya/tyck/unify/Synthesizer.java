// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.unify;

import kala.collection.mutable.MutableList;
import org.aya.generic.NameGenerator;
import org.aya.syntax.core.def.TeleDef;
import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.Callable;
import org.aya.syntax.core.term.call.MetaCall;
import org.aya.syntax.core.term.xtt.*;
import org.aya.syntax.ref.LocalCtx;
import org.aya.syntax.ref.LocalVar;
import org.aya.tyck.TyckState;
import org.aya.tyck.tycker.AbstractTycker;
import org.aya.util.error.Panic;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Synthesizer extends AbstractTycker {
  private final @NotNull NameGenerator nameGen = new NameGenerator();

  public Synthesizer(@NotNull TyckState state, @NotNull LocalCtx ctx, @NotNull Reporter reporter) {
    super(state, ctx, reporter);
  }

  public @Nullable Term synthesizeWHNF(@NotNull Term term) {
    var result = synthesize(term);
    return result == null ? null : whnf(result);
  }

  public @Nullable Term synthesize(@NotNull Term term) {
    return switch (term) {
      case AppTerm(var f, var a) -> {
        var fTy = synthesizeWHNF(f);
        yield fTy instanceof PiTerm pi ? pi.body().instantiate(a) : null;
      }
      case PiTerm pi -> {
        var pTy = synthesizeWHNF(pi.param());
        if (!(pTy instanceof SortTerm pSort)) yield null;

        var bTy = subscoped(() -> {
          var dummy = new LocalVar(nameGen.next(whnf(pi.param())));
          localCtx().put(dummy, pi.param());
          return synthesizeWHNF(pi.body().instantiate(dummy));
        });

        if (!(bTy instanceof SortTerm bSort)) yield null;

        // TODO: yield lub
        throw new UnsupportedOperationException("TODO");
      }
      case LamTerm _ -> null;
      case SigmaTerm sigmaTerm -> {
        var pTys = MutableList.<SortTerm>create();
        var params = MutableList.<LocalVar>create();

        var succ = subscoped(() -> {
          for (var p : sigmaTerm.params()) {
            var freeP = p.instantiateTele(params.view().map(FreeTerm::new));
            var pTy = synthesizeWHNF(freeP);
            if (!(pTy instanceof SortTerm pSort)) return false;
            pTys.append(pSort);

            var param = new LocalVar(nameGen.next(whnf(freeP)));
            params.append(param);
            localCtx().put(param, freeP);
          }

          return true;
        });

        if (!succ) yield null;

        // TODO: yield lub pTys
        throw new UnsupportedOperationException("TODO");
      }
      case TupTerm _ -> null;
      case FreeTerm(var var) -> localCtx().get(var);
      case LocalTerm _ -> throw new Panic("LocalTerm");
      case MetaPatTerm meta -> meta.meta().type();
      case ProjTerm(Term of, int index) -> {
        var ofTy = synthesizeWHNF(of);
        if (!(ofTy instanceof SigmaTerm ofSigma)) yield null;
        yield ofSigma.params().get(index - 1)
          // the type of projOf.{index - 1} may refer to the previous parameters
          .instantiateTele(ProjTerm.projSubst(of, index).view());
      }
      case Callable.Tele teleCall -> TeleDef.defResult(teleCall.ref())
        .instantiateTele(teleCall.args().view())
        .elevate(teleCall.ulift());

      case MetaCall _ -> throw new UnsupportedOperationException("TODO");
      case CoeTerm _ -> throw new UnsupportedOperationException("TODO");
      case PAppTerm _ -> throw new UnsupportedOperationException("TODO");
      case ErrorTerm errorTerm -> ErrorTerm.typeOf(errorTerm);
      case SortTerm sortTerm -> sortTerm.succ();
      case DimTerm _ -> throw new UnsupportedOperationException("TODO");
      case DimTyTerm _ -> throw new UnsupportedOperationException("TODO");
      case EqTerm _ -> throw new UnsupportedOperationException("TODO");
    };
  }
}
