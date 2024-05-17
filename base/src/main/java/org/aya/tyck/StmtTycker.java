// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Either;
import kala.control.Option;
import org.aya.generic.Modifier;
import org.aya.normalize.PrimFactory;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.concrete.Pattern;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.*;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.core.pat.PatToTerm;
import org.aya.syntax.core.repr.AyaShape;
import org.aya.syntax.core.term.FreeTerm;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.PiTerm;
import org.aya.syntax.core.term.SortTerm;
import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.ref.LocalCtx;
import org.aya.tyck.ctx.LocalLet;
import org.aya.tyck.error.BadTypeError;
import org.aya.tyck.error.PrimError;
import org.aya.tyck.error.UnifyInfo;
import org.aya.tyck.pat.ClauseTycker;
import org.aya.tyck.tycker.Problematic;
import org.aya.tyck.tycker.TeleTycker;
import org.aya.util.error.Panic;
import org.aya.util.error.WithPos;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static org.aya.tyck.tycker.TeleTycker.loadTele;

public record StmtTycker(
  @NotNull Reporter reporter,
  @NotNull AyaShape.Factory shapeFactory,
  @NotNull PrimFactory primFactory
) implements Problematic {
  private @NotNull ExprTycker mkTycker() {
    return new ExprTycker(new TyckState(shapeFactory, primFactory), new LocalCtx(), new LocalLet(), reporter);
  }
  public @NotNull Def check(Decl predecl) {
    var checked = doCheck(predecl);
    shapeFactory.bonjour(checked);
    return checked;
  }
  private @NotNull Def doCheck(Decl predecl) {
    ExprTycker tycker = null;
    if (predecl instanceof TeleDecl<?> decl) {
      if (decl.signature == null) checkHeader(decl, tycker = mkTycker());
    }

    return switch (predecl) {
      case TeleDecl.FnDecl fnDecl -> {
        var signature = fnDecl.signature;
        assert signature != null;

        var factory = FnDef.factory((retTy, body) ->
          new FnDef(fnDecl.ref,
            signature.rawParams(),
            retTy, fnDecl.modifiers, body));
        var teleVars = fnDecl.telescope.map(Expr.Param::ref);

        yield switch (fnDecl.body) {
          case TeleDecl.ExprBody(var expr) -> {
            if (tycker == null) {
              tycker = mkTycker();
              loadTele(fnDecl.teleVars(), fnDecl.signature, tycker);
            }
            var result = tycker.inherit(expr, signature.result().instantiateTeleVar(teleVars.view()));
            var wellBody = result.wellTyped();
            var wellTy = result.type();
            wellBody = tycker.freezeHoles(wellBody.bindTele(teleVars.view()));
            // we still need to bind [wellTy] in case it was a hole
            wellTy = tycker.freezeHoles(wellTy.bindTele(teleVars.view()));
            yield factory.apply(wellTy, Either.left(wellBody));
          }
          case TeleDecl.BlockBody(var clauses, var elims) -> {
            // we do not load signature here, so we need a fresh ExprTycker
            var clauseTycker = new ClauseTycker(mkTycker());

            var orderIndependent = fnDecl.modifiers.contains(Modifier.Overlap);
            if (orderIndependent) {
              throw new UnsupportedOperationException("Dame Desu!");
            } else {
              var patResult = clauseTycker.check(teleVars, signature, fnDecl.entireSourcePos(), clauses, elims);
              yield factory.apply(signature.result(), Either.right(patResult.wellTyped()));
            }
          }
        };
      }
      case TeleDecl.DataCon con -> Objects.requireNonNull(con.ref.core);   // see checkHeader
      case TeleDecl.PrimDecl prim -> Objects.requireNonNull(prim.ref.core);
      case TeleDecl.DataDecl data -> {
        var sig = data.signature;
        assert sig != null;
        if (tycker == null) {
          tycker = mkTycker();
          loadTele(data.teleVars(), sig, tycker);
        }
        for (var kon : data.body) checkHeader(kon, tycker);
        yield new DataDef(data.ref, sig.rawParams(), sig.result(),
          data.body.map(kon -> kon.ref.core));
      }
    };
  }

  private void checkHeader(@NotNull TeleDecl<?> decl, @NotNull ExprTycker tycker) {
    switch (decl) {
      case TeleDecl.DataCon con -> checkKitsune(con, tycker);
      case TeleDecl.PrimDecl prim -> checkPrim(tycker, prim);
      case TeleDecl.DataDecl data -> {
        var teleTycker = new TeleTycker.Default(tycker);
        var result = data.result;
        if (result == null) result = new WithPos<>(data.sourcePos(), new Expr.Type(0));
        var signature = teleTycker.checkSignature(data.telescope, result);
        var sort = SortTerm.Type0;
        if (signature.result() instanceof SortTerm userSort) sort = userSort;
        else fail(BadTypeError.univ(tycker.state, result, signature.result()));
        data.signature = new Signature<>(signature.param(), sort);
      }
      case TeleDecl.FnDecl fn -> {
        var teleTycker = new TeleTycker.Default(tycker);
        var result = fn.result;
        if (result == null) result = new WithPos<>(fn.sourcePos(), new Expr.Hole(false, null));
        fn.signature = teleTycker.checkSignature(fn.telescope, result);
      }
    }
  }

  /**
   * Kitsune says kon!
   *
   * @apiNote invoke this method after loading the telescope of data!
   */
  private void checkKitsune(@NotNull TeleDecl.DataCon dataCon, @NotNull ExprTycker exprTycker) {
    var ref = dataCon.ref;
    if (ref.core != null) return;
    var conDecl = ref.concrete;
    var dataRef = dataCon.dataRef;
    var dataDecl = dataRef.concrete;
    assert dataDecl != null && conDecl != null : "no concrete";
    var dataSig = dataDecl.signature;
    assert dataSig != null : "the header of data should be tycked";
    var ownerTele = dataSig.param().map(x -> x.descent((_, p) -> p.implicitize()));
    var ownerBinds = dataDecl.telescope.map(Expr.Param::ref);
    // dataTele already in localCtx
    // The result that a ctor should be, unless... TODO: it is a Path result
    var freeDataCall = new DataCall(dataRef, 0, ownerBinds.map(FreeTerm::new));

    var wellPats = ImmutableSeq.<Pat>empty();
    if (dataCon.patterns.isNotEmpty()) {
      // do not do coverage check
      var lhsResult = new ClauseTycker(exprTycker = mkTycker()).checkLhs(dataSig, null,
        new Pattern.Clause(dataCon.entireSourcePos(), dataCon.patterns, Option.none()));
      wellPats = lhsResult.clause().pats();
      exprTycker.setLocalCtx(lhsResult.localCtx());
      lhsResult.addLocalLet(ownerBinds, exprTycker);
      freeDataCall = new DataCall(dataRef, 0, wellPats.map(PatToTerm::visit));

      var allBinds = Pat.collectBindings(wellPats.view());
      ownerTele = allBinds
        .map(x -> new WithPos<>(x.var().definition(),
          new Param(x.var().name(), x.type(), false)));
      ownerBinds = allBinds.map(Pat.CollectBind::var);
    }

    var ctorTy = conDecl.result;
    if (ctorTy != null) {
      // TODO: handle Path result
      // TODO: unify ctorTy and freeDataCall
      throw new UnsupportedOperationException("TODO");
    }

    var teleTycker = new TeleTycker.Con(exprTycker, dataSig.result());
    var selfTele = teleTycker.checkTele(conDecl.telescope);
    // the result will NEVER refer to the telescope of ctor, unless... TODO: it is a Path result
    var selfSig = new Signature<>(selfTele, freeDataCall)
      .bindTele(ownerBinds.view());

    if (!(selfSig.result() instanceof DataCall dataResult)) {
      Panic.unreachable();
      return;
    }

    // The signature of con should be full (the same as [konCore.telescope()])
    conDecl.signature = new Signature<>(ownerTele.concat(selfSig.param()), dataResult);

    ref.core = new ConDef(dataRef, ref,
      wellPats,
      ownerTele.map(WithPos::data),
      selfSig.rawParams(),
      dataResult, false);
  }

  private void checkPrim(@NotNull ExprTycker tycker, TeleDecl.PrimDecl prim) {
    var teleTycker = new TeleTycker.Default(tycker);
    // This directly corresponds to the tycker.localCtx = new LocalCtx();
    //  at the end of this case clause.
    assert tycker.localCtx().isEmpty();
    var core = prim.ref.core;
    if (prim.telescope.isEmpty() && prim.result == null) {
      var pos = prim.sourcePos();
      prim.signature = new Signature<>(core.telescope.map(param -> new WithPos<>(pos, param)), core.result);
      return;
    }
    if (prim.telescope.isNotEmpty()) {
      if (prim.result == null) {
        fail(new PrimError.NoResultType(prim));
        return;
      }
    }
    assert prim.result != null;
    var tele = teleTycker.checkSignature(prim.telescope, prim.result);
    tycker.unifyTermReported(
      PiTerm.make(tele.param().view().map(p -> p.data().type()), tele.result()),
      // No checks, slightly faster than TeleDef.defType
      PiTerm.make(core.telescope.view().map(Param::type), core.result),
      prim.entireSourcePos(),
      msg -> new PrimError.BadSignature(prim, msg, new UnifyInfo(tycker.state)));
    prim.signature = tele;
    tycker.solveMetas();
    tycker.setLocalCtx(new LocalCtx());
  }
}
