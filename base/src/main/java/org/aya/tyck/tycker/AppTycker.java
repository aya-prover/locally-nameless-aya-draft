// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.tycker;

import kala.collection.immutable.ImmutableArray;
import kala.collection.immutable.ImmutableSeq;
import kala.function.CheckedBiFunction;
import org.aya.syntax.compile.JitTele;
import org.aya.syntax.concrete.stmt.decl.DataCon;
import org.aya.syntax.concrete.stmt.decl.DataDecl;
import org.aya.syntax.concrete.stmt.decl.FnDecl;
import org.aya.syntax.concrete.stmt.decl.PrimDecl;
import org.aya.syntax.core.def.*;
import org.aya.syntax.core.repr.AyaShape;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.*;
import org.aya.syntax.ref.DefVar;
import org.aya.tyck.Jdg;
import org.aya.tyck.TyckState;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public interface AppTycker {
  @FunctionalInterface
  interface Factory<Ex extends Exception> extends
    CheckedBiFunction<JitTele, Function<Term[], Jdg>, Jdg, Ex> {
  }

  @SuppressWarnings("unchecked")
  static <Ex extends Exception> @NotNull Jdg checkDefApplication(
    @NotNull DefVar<?, ?> defVar,
    @NotNull TyckState state, @NotNull Factory<Ex> makeArgs
  ) throws Ex {
    var core = defVar.core;
    var concrete = defVar.concrete;

    if (core instanceof FnDef || concrete instanceof FnDecl) {
      var fnVar = (DefVar<FnDef, FnDecl>) defVar;
      var signature = TyckDef.defSignature(fnVar);
      return makeArgs.applyChecked(signature, args -> {
        var shape = state.shapeFactory().find(new FnDef.Delegate(fnVar));
        var argsSeq = ImmutableArray.from(args);
        var result = signature.result(args);
        if (shape.isDefined()) {
          var operator = AyaShape.ofFn(fnVar, shape.get());
          if (operator != null) {
            return new Jdg.Default(new RuleReducer.Fn(operator, 0, argsSeq), result);
          }
        }
        return new Jdg.Default(new FnCall(fnVar, 0, argsSeq), result);
      });
    } else if (core instanceof DataDef || concrete instanceof DataDecl) {
      var dataVar = (DefVar<DataDef, DataDecl>) defVar;
      var signature = TyckDef.defSignature(dataVar);
      return makeArgs.applyChecked(signature, args -> new Jdg.Default(
        new DataCall(dataVar, 0, ImmutableArray.from(args)),
        signature.result(args)
      ));
    } else if (core instanceof PrimDef || concrete instanceof PrimDecl) {
      var primVar = (DefVar<PrimDef, PrimDecl>) defVar;
      var signature = TyckDef.defSignature(primVar);
      return makeArgs.applyChecked(signature, args -> new Jdg.Default(
        state.primFactory().unfold(new PrimCall(primVar, 0, ImmutableArray.from(args)), state),
        signature.result(args)
      ));
    } else if (core instanceof ConDef || concrete instanceof DataCon) {
      var conVar = (DefVar<ConDef, DataCon>) defVar;
      var conCore = conVar.core;
      assert conCore != null;
      var dataVar = conCore.dataRef;

      var fullSignature = TyckDef.defSignature(conVar);   // ownerTele + selfTele
      var ownerTele = conCore.ownerTele;

      return makeArgs.applyChecked(fullSignature, args -> {
        var realArgs = ImmutableArray.from(args);
        var ownerArgs = realArgs.take(ownerTele.size());
        var conArgs = realArgs.drop(ownerTele.size());

        var type = fullSignature.result(realArgs);
        var shape = state.shapeFactory().find(new DataDef.Delegate(dataVar))
          .mapNotNull(recog -> {
            if (recog.shape() == AyaShape.NAT_SHAPE) {
              var head = AyaShape.ofCon(conVar, recog, new DataCall(dataVar, 0, ImmutableSeq.empty()));
              assert head != null : "bad ShapeFactory";
              return new RuleReducer.Con(head, 0, ImmutableSeq.empty(), realArgs);
            }

            return null;
          })
          .getOrNull();
        if (shape != null) return new Jdg.Default(shape, type);
        var wellTyped = new ConCall(conVar, 0, ownerArgs, conArgs);
        return new Jdg.Default(wellTyped, type);
      });
    }

    return Panic.unreachable();
  }
}
