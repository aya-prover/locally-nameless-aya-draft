// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.tycker;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.*;
import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.core.term.call.FnCall;
import org.aya.syntax.ref.DefVar;
import org.aya.tyck.Result;
import org.jetbrains.annotations.NotNull;

public final class ExprTyckerUtils {
  private ExprTyckerUtils() {}

  @SuppressWarnings("unchecked")
  public static @NotNull Result inferDef(@NotNull DefVar<? extends Def, ? extends Decl> defVar) {
    var core = defVar.core;
    var concrete = defVar.concrete;

    if (core instanceof FnDef || concrete instanceof TeleDecl.FnDecl) {
      var fnVar = (DefVar<FnDef, TeleDecl.FnDecl>) defVar;
      new Result.Default(
        new FnCall(fnVar, 0, ImmutableSeq.empty()),
        TeleDef.defType(fnVar)
      );
    } else if (core instanceof DataDef || concrete instanceof TeleDecl.DataDecl) {
      var dataVar = (DefVar<DataDef, TeleDecl.DataDecl>) defVar;
      new Result.Default(
        new DataCall(dataVar, 0, ImmutableSeq.empty()),
        TeleDef.defType(dataVar)
      );
    } else if (core instanceof CtorDef || concrete instanceof TeleDecl.DataCtor) {
      var ctorVar = (DefVar<CtorDef, TeleDecl.DataCtor>) defVar;
      // TODO: original code looks terrible
      throw new UnsupportedOperationException("TODO");
    }

    throw new UnsupportedOperationException("TODO");
  }
}
