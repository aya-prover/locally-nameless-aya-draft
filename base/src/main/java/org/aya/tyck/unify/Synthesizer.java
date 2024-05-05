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
import org.aya.util.error.Panic;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

public final class Synthesizer extends AbstractTycker {
  private final @NotNull NameGenerator nameGen = new NameGenerator();

  public Synthesizer(@NotNull TyckState state, @NotNull LocalCtx ctx, @NotNull Reporter reporter) {
    super(state, ctx, reporter);
  }

  public @Nullable Term trySynthesizeWHNF(@NotNull Term term) {
    var result = synthesize(whnf(term));      // TODO: we don't whnf term in old project, that seems suspicious
    return result == null ? null : whnf(result);
  }

  public @NotNull Term synthesizeWHNF(@NotNull Term term) {
    var result = trySynthesizeWHNF(term);
    assert result != null : term.toDoc(AyaPrettierOptions.debug()).debugRender();
    return result;
  }

  /**
   * @param term a whnfed term
   * @return type of term if success
   */
  public @Nullable Term synthesize(@NotNull Term term) {
    return switch (term) {
      case AppTerm(var f, var a) -> {
        var fTy = trySynthesizeWHNF(f);
        yield fTy instanceof PiTerm pi ? pi.body().instantiate(a) : null;
      }
      case PiTerm pi -> {
        var pTy = trySynthesizeWHNF(pi.param());
        if (!(pTy instanceof SortTerm pSort)) yield null;

        var bTy = subscoped(() -> {
          var param = putIndex(pi.param());
          return trySynthesizeWHNF(pi.body().instantiate(param));
        });

        if (!(bTy instanceof SortTerm bSort)) yield null;

        yield SortTerm.lub(pSort, bSort);
      }
      case LamTerm _ -> null;
      case SigmaTerm sigmaTerm -> {
        var pTys = MutableList.<SortTerm>create();

        var succ = sigmaTerm.check(new SigmaIterator(this, sigmaTerm.params().iterator()), (t, param) -> {
          var pTy = trySynthesizeWHNF(param);
          if (!(pTy instanceof SortTerm pSort)) return null;
          pTys.append(pSort);
          return t;
        }) != null;

        // TODO: in case we want to recover the code XD
        // var params = MutableList.<Term>create();
        //
        // var succ = subscoped(() -> {
        //   for (var p : sigmaTerm.params()) {
        //     var freeP = p.instantiateTele(params.view());
        //     var pTy = trySynthesizeWHNF(freeP);
        //     if (!(pTy instanceof SortTerm pSort)) return false;
        //     pTys.append(pSort);
        //
        //     var param = putIndex(freeP);
        //     params.append(new FreeTerm(param));
        //   }
        //
        //   return true;
        // });
        //

        if (!succ) yield null;

        // This is safe since a [SigmaTerm] has at least 2 parameters.
        yield pTys.reduce(SortTerm::lub);
      }
      case TupTerm _ -> null;
      case FreeTerm(var var) -> localCtx().get(var);
      case LocalTerm _ -> throw new Panic("LocalTerm");
      case MetaPatTerm meta -> meta.meta().type();
      case ProjTerm(Term of, int index) -> {
        var ofTy = trySynthesizeWHNF(of);
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

  public @NotNull LocalVar putIndex(@NotNull Term type) {
    return super.putIndex(nameGen, type);
  }

  private record SigmaIterator(@NotNull Synthesizer synthesizer,
                               @NotNull Iterator<Term> typeIter) implements Iterator<FreeTerm> {
    @Override
    public boolean hasNext() {
      return typeIter.hasNext();
    }

    @Override
    public FreeTerm next() {
      var bind = synthesizer.putIndex(typeIter.next());
      return new FreeTerm(bind);
    }
  }
}
