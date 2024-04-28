// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.control.Either;
import org.aya.generic.Modifier;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.Def;
import org.aya.syntax.core.def.FnDef;
import org.aya.syntax.core.def.Signature;
import org.aya.syntax.core.term.SortTerm;
import org.aya.tyck.error.BadTypeError;
import org.aya.tyck.error.PrimError;
import org.aya.tyck.tycker.Problematic;
import org.aya.tyck.tycker.TeleTycker;
import org.aya.util.error.WithPos;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

import static org.aya.tyck.tycker.TeleTycker.checkTele;
import static org.aya.tyck.tycker.TeleTycker.loadTele;

public record StmtTycker(@NotNull Reporter reporter) implements Problematic {
  private @NotNull Def check(Decl predecl, ExprTycker tycker) {
    if (predecl instanceof TeleDecl<?> decl) {
      if (decl.signature != null) loadTele(decl.signature, tycker);
      else checkHeader(decl, tycker);
    }

    return switch (predecl) {
      case TeleDecl.FnDecl fnDecl -> {
        var signature = fnDecl.signature;
        assert signature != null;

        var factory = FnDef.factory((retTy, body) ->
          new FnDef(fnDecl.ref,
            signature.param().map(x -> x.data().forget()),
            retTy, fnDecl.modifiers, body));
        var teleVars = signature.param().map(x -> x.data().ref());

        yield switch (fnDecl.body) {
          case TeleDecl.ExprBody exprBody -> {
            var result = tycker.inherit(exprBody.expr(), signature.result());
            var wellBody = result.wellTyped();
            var wellTy = result.type();
            // TODO: wellTy may contains meta, zonk them!
            wellBody = TeleTycker.bindResult(wellBody, teleVars);
            // we still need to bind [wellTy] in case it was a hole
            wellTy = TeleTycker.bindResult(wellTy, teleVars);
            yield factory.apply(wellTy, Either.left(wellBody));
          }
          case TeleDecl.BlockBody blockBody -> {
            var orderIndependent = fnDecl.modifiers.contains(Modifier.Overlap);
            if (orderIndependent) {
              throw new UnsupportedOperationException("Dame Desu!");
            } else {
              throw new UnsupportedOperationException("Dame Desu!");
            }
          }
        };
      }
      case TeleDecl.DataCtor dataCtor -> throw new UnsupportedOperationException("TODO");
      case TeleDecl.DataDecl dataDecl -> throw new UnsupportedOperationException("TODO");
    };
  }

  private void checkHeader(TeleDecl<?> decl, ExprTycker tycker) {
    switch (decl) {
      case TeleDecl.DataCtor con -> {}
      case TeleDecl.DataDecl data -> {
        assert data.result != null;
        var signature = checkTele(data.telescope, data.result, tycker);
        SortTerm sort = SortTerm.Type0;
        if (signature.result() instanceof SortTerm userSort) {
          sort = userSort;
        } else {
          reporter.report(BadTypeError.univ(tycker.state, data.result, signature.result()));
        }
        data.signature = new Signature<>(signature.param(), sort);
      }
      case TeleDecl.FnDecl fn -> fn.signature = checkTele(fn.telescope, fn.result, tycker);
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
          // ErrorExpr on prim.result means the result type is unspecified.
          if (prim.result == null) {
            reporter.report(new PrimError.NoResultType(prim));
            return;
          }
        }
        // var tele = checkTele(prim.telescope, prim.result, tycker);
        // tycker.unifyTyReported(
        //   PiTerm.make(tele, result),
        //   PiTerm.make(core.telescope, core.result),
        //   prim.result);
        // prim.signature = new Def.Signature<>(tele, result);
        // tycker.solveMetas();
        // tycker.ctx = new SeqLocalCtx();
      }
    }
  }
}
