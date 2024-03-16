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
import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.core.term.call.FnCall;
import org.aya.syntax.ref.DefVar;
import org.aya.tyck.Result;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public interface AppTycker {
  @FunctionalInterface
  interface Factory<Ex extends Exception> extends
    CheckedBiFunction<ImmutableSeq<Param>, Function<ImmutableSeq<Term>, Result>, Result, Ex> {
  }

  @SuppressWarnings("unchecked")
  static <Ex extends Exception> @NotNull Result checkDefApplication(
    @NotNull DefVar<? extends Def, ? extends Decl> defVar,
    Factory<Ex> makeArgs
  ) throws Ex {
    var core = defVar.core;
    var concrete = defVar.concrete;

    if (core instanceof FnDef || concrete instanceof TeleDecl.FnDecl) {
      var fnVar = (DefVar<FnDef, TeleDecl.FnDecl>) defVar;
      return makeArgs.applyChecked(TeleDef.defTele(fnVar), args -> new Result.Default(
        new FnCall(fnVar, 0, args),
        TeleDef.defType(fnVar)
      ));
    } else if (core instanceof DataDef || concrete instanceof TeleDecl.DataDecl) {
      var dataVar = (DefVar<DataDef, TeleDecl.DataDecl>) defVar;
      return makeArgs.applyChecked(TeleDef.defTele(dataVar), args -> new Result.Default(
        new DataCall(dataVar, 0, args),
        TeleDef.defType(dataVar)
      ));
    } else if (core instanceof CtorDef || concrete instanceof TeleDecl.DataCtor) {
      var ctorVar = (DefVar<CtorDef, TeleDecl.DataCtor>) defVar;
      // TODO: original code looks terrible
      throw new UnsupportedOperationException("TODO");
    }

    throw new UnsupportedOperationException("TODO");
  }
}
