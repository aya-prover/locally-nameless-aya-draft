// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.tycker;

import org.aya.generic.Modifier;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.FnDef;
import org.aya.syntax.core.def.TeleDef;
import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.Callable;
import org.aya.syntax.ref.DefVar;
import org.aya.tyck.Result;
import org.aya.tyck.TyckState;
import org.aya.util.Arg;
import org.jetbrains.annotations.NotNull;

/**
 * TODO: update document
 * This is the second base-base class of a tycker.
 * It has the zonking stuffs and basic def-call related functions.
 * Apart from that, it also deals with core term references in concrete terms.
 *
 * @author ice1000
 * @see #whnf(Term)
 * @see #defCall
 * @see #conOwnerSubst(ConCall)
 */
public sealed interface StateBased permits AbstractExprTycker {
  @NotNull TyckState state();

  @NotNull Term whnf(@NotNull Term term);

  /**
   * Elaborate partial applied call
   * {@code someCtor} -> {@code \ params -> someCtor params }
   */
  default <D extends TeleDef, S extends TeleDecl<? extends Term>> @NotNull Result
  defCall(DefVar<D, S> defVar, Callable.Factory<D, S> function) {
    var tele = TeleDef.defTele(defVar);
    var spine = tele.mapIndexed((i, type) -> type.<Term>map(_ -> new LocalTerm(tele.size() - 1 - i)));    // λ. λ. λ. someCtor 2 1 0
    Term body = function.make(defVar, 0, spine);
    var type = PiTerm.make(tele, TeleDef.defResult(defVar));
    if ((defVar.core instanceof FnDef fn && fn.modifiers.contains(Modifier.Inline)) /*|| defVar.core instanceof PrimDef*/) {
      body = whnf(body);
    }

    return new Result.Default(LamTerm.make(spine.map(Arg::explicit), body), type);
  }

}
