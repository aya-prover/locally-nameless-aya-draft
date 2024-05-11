// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.control.Either;
import org.aya.generic.Modifier;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.*;
import org.aya.syntax.core.term.FreeTerm;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.PiTerm;
import org.aya.syntax.core.term.SortTerm;
import org.aya.syntax.core.term.call.DataCall;
import org.aya.tyck.error.BadTypeError;
import org.aya.tyck.error.PrimError;
import org.aya.tyck.pat.ClauseTycker;
import org.aya.tyck.tycker.Problematic;
import org.aya.tyck.tycker.TeleTycker;
import org.aya.util.error.Panic;
import org.aya.util.error.WithPos;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static org.aya.tyck.tycker.TeleTycker.loadTele;

public record StmtTycker(@NotNull Reporter reporter) implements Problematic {
  public @NotNull Def check(Decl predecl, ExprTycker tycker) {
    if (predecl instanceof TeleDecl<?> decl) {
      if (decl.signature != null) loadTele(decl.telescope.map(Expr.Param::ref), decl.signature, tycker);
      else checkHeader(decl, tycker);
    }

    return switch (predecl) {
      case TeleDecl.FnDecl fnDecl -> {
        var signature = fnDecl.signature;
        assert signature != null;

        var factory = FnDef.factory((retTy, body) ->
          new FnDef(fnDecl.ref,
            signature.param().map(WithPos::data),
            retTy, fnDecl.modifiers, body));
        var teleVars = fnDecl.telescope.map(Expr.Param::ref);

        yield switch (fnDecl.body) {
          case TeleDecl.ExprBody(var expr) -> {
            var result = tycker.inherit(expr, signature.result().instantiateTeleVar(teleVars.view()));
            var wellBody = result.wellTyped();
            var wellTy = result.type();
            // TODO: wellTy may contains meta, zonk them!
            wellBody = wellBody.bindTele(teleVars.view());
            // we still need to bind [wellTy] in case it was a hole
            wellTy = wellTy.bindTele(teleVars.view());
            yield factory.apply(wellTy, Either.left(wellBody));
          }
          case TeleDecl.BlockBody blockBody -> {
            var clauseTycker = new ClauseTycker(tycker);
            var orderIndependent = fnDecl.modifiers.contains(Modifier.Overlap);
            if (orderIndependent) {
              throw new UnsupportedOperationException("Dame Desu!");
            } else {
              throw new UnsupportedOperationException("Dame Desu!");
            }
          }
        };
      }
      case TeleDecl.DataCon dataCon -> Objects.requireNonNull(dataCon.ref.core);   // see checkHeader
      case TeleDecl.DataDecl dataDecl -> {
        var sig = dataDecl.signature;
        assert sig != null;
        var kitsuneTachi = dataDecl.body.map(kon -> (ConDef) check(kon, tycker));
        yield new DataDef(dataDecl.ref, sig.param().map(WithPos::data), sig.result(), kitsuneTachi);
      }
      case TeleDecl.PrimDecl primDecl -> throw new UnsupportedOperationException("TODO");
    };
  }

  private void checkHeader(TeleDecl<?> decl, ExprTycker tycker) {
    var teleTycker = new TeleTycker.Default(tycker);

    switch (decl) {
      case TeleDecl.DataCon con -> checkKitsune(con, tycker);
      case TeleDecl.DataDecl data -> {
        var result = data.result;
        if (result == null) result = new WithPos<>(data.sourcePos(), new Expr.Type(0));
        var signature = teleTycker.checkSignature(data.telescope, result);
        var sort = SortTerm.Type0;
        if (signature.result() instanceof SortTerm userSort) sort = userSort;
        else fail(BadTypeError.univ(tycker.state, result, signature.result()));
        data.signature = new Signature<>(signature.param(), sort);
      }
      case TeleDecl.FnDecl fn -> {
        var result = fn.result;
        if (result == null) result = new WithPos<>(fn.sourcePos(), new Expr.Hole(false, null));
        fn.signature = teleTycker.checkSignature(fn.telescope, result);
      }
      case TeleDecl.PrimDecl prim -> {
        // This directly corresponds to the tycker.localCtx = new LocalCtx();
        //  at the end of this case clause.
        assert tycker.localCtx().isEmpty();
        var core = prim.ref.core;
        if (prim.telescope.isEmpty() && prim.result == null) {
          var pos = decl.sourcePos();
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
        tycker.unifyTyReported(
          PiTerm.make(tele.param().view().map(p -> p.data().type()), tele.result()),
          PiTerm.make(core.telescope.view().map(Param::type), core.result),
          prim.result);
        prim.signature = tele;
        tycker.solveMetas();
        assert tycker.localCtx().isEmpty() : "If this fails, replace it with tycker.setLocalCtx(new LocalCtx());";
      }
    }
  }

  /**
   * Kitsune says kon!
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
    // TODO: update this if there are patterns
    var ownerTele = dataSig.param().map(x -> x.descent((_, p) -> p.implicitize()));
    var dataTele = dataDecl.telescope.map(Expr.Param::ref);
    // dataTele already in localCtx
    // The result that a ctor should be, unless... TODO: it is a Path result
    var freeDataCall = new DataCall(dataRef, 0, dataTele.map(FreeTerm::new));
    // TODO: check patterns if there are
    var ctorTy = conDecl.result;
    if (ctorTy != null) {
      // TODO: handle Path result
      // TODO: unify ctorTy and freeDataCall
      throw new UnsupportedOperationException("TODO");
    }

    var teleTycker = new TeleTycker.Con(exprTycker, dataSig.result());
    var wellTele = teleTycker.checkTele(conDecl.telescope);
    // the result will NEVER refer to the telescope of ctor, unless... TODO: it is a Path result
    var halfSig = new Signature<>(wellTele, freeDataCall)
      .bindTele(dataTele.view());     // TODO: bind pattern bindings if indexed data

    if (!(halfSig.result() instanceof DataCall dataResult)) {
      Panic.unreachable();
      return;
    }

    // The signature of con should be full (the same as [konCore.telescope()])
    conDecl.signature = new Signature<>(ownerTele.concat(halfSig.param()), dataResult);

    // TODO: handle ownerTele and coerce
    var konCore = new ConDef(dataRef, ref,
      ownerTele.map(WithPos::data),
      halfSig.param().map(WithPos::data),
      dataResult, false);
    ref.core = konCore;
  }
}
