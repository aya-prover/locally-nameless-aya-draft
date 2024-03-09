// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.def;

import kala.collection.Seq;
import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.PiTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.DefVar;
import org.aya.util.error.WithPos;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author ice1000
 */
public sealed interface TeleDef extends Def permits SubLevelDef, TopLevelDef {
  static @NotNull Term defType(@NotNull DefVar<? extends TeleDef, ? extends TeleDecl<?>> defVar) {
    return PiTerm.make(defTele(defVar).map(Param::type), defResult(defVar));
  }

  static @NotNull ImmutableSeq<Param> defTele(@NotNull DefVar<? extends TeleDef, ? extends TeleDecl<?>> defVar) {
    if (defVar.core != null) return defVar.core.telescope();
    // guaranteed as this is already a core term
    var signature = defVar.concrete.signature;
    assert signature != null : defVar.name();
    return signature.param().map(WithPos::data);
  }
  static @NotNull Seq<CtorDef> dataBody(@NotNull DefVar<? extends DataDef, ? extends TeleDecl.DataDecl> defVar) {
    if (defVar.core != null) return defVar.core.body;
      // guaranteed as this is already a core term
    else return defVar.concrete.checkedBody;
  }

  @SuppressWarnings("unchecked") @Contract(pure = true)
  static <T extends Term> @NotNull T
  defResult(@NotNull DefVar<? extends TeleDef, ? extends TeleDecl<? extends T>> defVar) {
    if (defVar.core != null) return (T) defVar.core.result();
      // guaranteed as this is already a core term
    else return Objects.requireNonNull(defVar.concrete.signature).result();
  }

  @Override @NotNull DefVar<? extends TeleDef, ? extends Decl> ref();
  @NotNull Term result();
  @NotNull ImmutableSeq<Param> telescope();
}
