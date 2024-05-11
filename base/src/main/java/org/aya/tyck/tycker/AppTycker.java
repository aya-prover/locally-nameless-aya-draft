// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.tycker;

import kala.collection.immutable.ImmutableSeq;
import kala.function.CheckedBiFunction;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.*;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.ConCall;
import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.core.term.call.FnCall;
import org.aya.syntax.ref.DefVar;
import org.aya.tyck.Jdg;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public interface AppTycker {
  @FunctionalInterface
  interface Factory<Ex extends Exception> extends
    CheckedBiFunction<ImmutableSeq<Param>, Function<ImmutableSeq<Term>, Jdg>, Jdg, Ex> {
  }

  @SuppressWarnings("unchecked")
  static <Ex extends Exception> @NotNull Jdg checkDefApplication(
    @NotNull DefVar<? extends Def, ? extends Decl> defVar,
    Factory<Ex> makeArgs
  ) throws Ex {
    var core = defVar.core;
    var concrete = defVar.concrete;

    if (core instanceof FnDef || concrete instanceof TeleDecl.FnDecl) {
      var fnVar = (DefVar<FnDef, TeleDecl.FnDecl>) defVar;
      return makeArgs.applyChecked(TeleDef.defTele(fnVar), args -> new Jdg.Default(
        new FnCall(fnVar, 0, args),
        TeleDef.defResult(fnVar).instantiateTele(args.view())
      ));
    } else if (core instanceof DataDef || concrete instanceof TeleDecl.DataDecl) {
      var dataVar = (DefVar<DataDef, TeleDecl.DataDecl>) defVar;
      return makeArgs.applyChecked(TeleDef.defTele(dataVar), args -> new Jdg.Default(
        new DataCall(dataVar, 0, args),
        TeleDef.defResult(dataVar).instantiateTele(args.view())
      ));
    } else if (core instanceof ConDef || concrete instanceof TeleDecl.DataCon) {
      var conVar = (DefVar<ConDef, TeleDecl.DataCon>) defVar;
      var conCore = conVar.core;
      assert conCore != null;
      var dataVar = conCore.dataRef;

      var fullTele = TeleDef.defTele(conVar);   // ownerTele + selfTele
      var ownerTele = conCore.ownerTele;

      return makeArgs.applyChecked(fullTele, args -> {
        var ownerArgs = args.take(ownerTele.size());
        var conArgs = args.drop(ownerTele.size());

        var wellTyped = new ConCall(dataVar, conVar, ownerArgs, 0, conArgs);
        var type = TeleDef.defResult(conVar).instantiateTele(args.view());
        return new Jdg.Default(wellTyped, type);
      });
    }

    return Panic.unreachable();
  }
}
