// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.unify;

import kala.collection.mutable.MutableList;
import org.aya.generic.NameGenerator;
import org.aya.prettier.AyaPrettierOptions;
import org.aya.syntax.core.def.TeleDef;
import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.Callable;
import org.aya.syntax.core.term.call.MetaCall;
import org.aya.syntax.core.term.xtt.*;
import org.aya.syntax.ref.LocalCtx;
import org.aya.syntax.ref.LocalVar;
import org.aya.tyck.TyckState;
import org.aya.tyck.tycker.AbstractTycker;
import org.aya.tyck.tycker.ContextBased;
import org.aya.tyck.tycker.StateBased;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Synthesizer(
  @NotNull NameGenerator nameGen,
  @NotNull AbstractTycker tycker
) implements StateBased, ContextBased {
  public Synthesizer(@NotNull AbstractTycker tycker) {
    this(new NameGenerator(), tycker);
  }

  public @Nullable Term trySynth(@NotNull Term term) {
    var result = synthesize(term);
    return result == null ? null : whnf(result);
  }

  public @NotNull Term synth(@NotNull Term term) {
    var result = trySynth(term);
    assert result != null : term.toDoc(AyaPrettierOptions.debug()).debugRender();
    return result;
  }

  public @NotNull Term synthDontNormalize(@NotNull Term term) {
    var result = synthesize(term);
    assert result != null : term.toDoc(AyaPrettierOptions.debug()).debugRender();
    return result;
  }

  /**
   * @param term a whnfed term
   * @return type of term if success
   */
  private @Nullable Term synthesize(@NotNull Term term) {
    return switch (term) {
      case AppTerm(var f, var a) -> {
        var fTy = trySynth(f);
        yield fTy instanceof PiTerm pi ? pi.body().instantiate(a) : null;
      }
      case PiTerm pi -> {
        var pTy = trySynth(pi.param());
        if (!(pTy instanceof SortTerm pSort)) yield null;

        var bTy = subscoped(() -> {
          var param = putIndex(pi.param());
          return trySynth(pi.body().instantiate(param));
        });

        if (!(bTy instanceof SortTerm bSort)) yield null;
        yield SortTerm.lub(pSort, bSort);
      }
      case SigmaTerm sigma -> {
        var pTys = MutableList.<SortTerm>create();
        var params = MutableList.<Term>create();

        var succ = subscoped(() -> {
          for (var p : sigma.params()) {
            var freeP = p.instantiateTele(params.view());
            var pTy = trySynth(freeP);
            if (!(pTy instanceof SortTerm pSort)) return false;
            pTys.append(pSort);

            var param = putIndex(freeP);
            params.append(new FreeTerm(param));
          }

          return true;
        });

        if (!succ) yield null;

        // This is safe since a [SigmaTerm] has at least 2 parameters.
        yield pTys.reduce(SortTerm::lub);
      }
      case TupTerm _ -> null;
      case LamTerm _ -> null;
      case FreeTerm(var var) -> localCtx().get(var);
      case LocalTerm _ -> throw new Panic("LocalTerm");
      case MetaPatTerm meta -> meta.meta().type();
      case ProjTerm(Term of, int index) -> {
        var ofTy = trySynth(of);
        if (!(ofTy instanceof SigmaTerm ofSigma)) yield null;
        yield ofSigma.params().get(index - 1)
          // the type of projOf.{index - 1} may refer to the previous parameters
          .instantiateTele(ProjTerm.projSubst(of, index).view());
      }
      case Callable.Tele teleCall -> TeleDef.defResult(teleCall.ref())
        .instantiateTele(teleCall.args().view())
        .elevate(teleCall.ulift());

      case MetaCall _ -> throw new UnsupportedOperationException("TODO");
      case CoeTerm coe -> coe.family();
      case PAppTerm papp -> {
        var fTy = trySynth(papp.fun());
        if (!(fTy instanceof EqTerm eq)) yield null;
        yield eq.A().instantiate(papp.arg());
      }
      case ErrorTerm error -> ErrorTerm.typeOf(error);
      case SortTerm sort -> sort.succ();
      case DimTerm _ -> DimTyTerm.INSTANCE;
      case DimTyTerm _ -> SortTerm.ISet;
      case EqTerm eq -> synthesize(eq.A());
    };
  }

  public @NotNull LocalVar putIndex(@NotNull Term type) {
    return tycker.putIndex(nameGen, type);
  }
  @Override public @NotNull TyckState state() {
    return tycker.state;
  }
  @Override public @NotNull LocalCtx localCtx() {
    return tycker.localCtx();
  }
  @Override public @NotNull LocalCtx setLocalCtx(@NotNull LocalCtx ctx) {
    return tycker.setLocalCtx(ctx);
  }
}
