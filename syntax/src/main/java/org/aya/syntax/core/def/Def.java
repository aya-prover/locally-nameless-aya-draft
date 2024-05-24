// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.def;

import kala.collection.immutable.ImmutableSeq;
import org.aya.generic.AyaDocile;
import org.aya.prettier.CorePrettier;
import org.aya.pretty.doc.Doc;
import org.aya.syntax.compile.JitTele;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.PiTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.DefVar;
import org.aya.util.ForLSP;
import org.aya.util.prettier.PrettierOptions;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public sealed interface Def extends AyaDocile permits SubLevelDef, TopLevelDef {
  @Override default @NotNull Doc toDoc(@NotNull PrettierOptions options) {
    return new CorePrettier(options).def(this);
  }

  //region Pretty & IDE only APIs
  /**
   * For pretty printing and IDE only, same for defTele and defResult.
   * This does not work well with compiled Aya.
   */
  @ForLSP static @NotNull Term defType(@NotNull DefVar<? extends Def, ? extends Decl> defVar) {
    return PiTerm.make(defTele(defVar).map(Param::type), defResult(defVar));
  }
  @ForLSP static @NotNull ImmutableSeq<Param>
  defTele(@NotNull DefVar<? extends Def, ? extends Decl> defVar) {
    if (defVar.core != null) return defVar.core.telescope();
    // guaranteed as this is already a core term
    var signature = defVar.concrete.signature;
    assert signature != null : defVar.name();
    return signature.rawParams();
  }
  @ForLSP @Contract(pure = true)
  static @NotNull Term defResult(@NotNull DefVar<? extends Def, ? extends Decl> defVar) {
    if (defVar.core != null) return defVar.core.result();
      // guaranteed as this is already a core term
    else return Objects.requireNonNull(defVar.concrete.signature).result();
  }
  //endregion

  static @NotNull JitTele defSignature(@NotNull DefVar<? extends Def, ? extends Decl> defVar) {
    if (defVar.core != null) return new JitTele.LocallyNameless(defVar.core.telescope(), defVar.core.result());
    // guaranteed as this is already a core term
    var signature = defVar.concrete.signature;
    assert signature != null : defVar.name();
    return new JitTele.LocallyNameless(signature.rawParams(), signature.result());
  }

  @NotNull DefVar<? extends Def, ?> ref();
  @NotNull Term result();
  @NotNull ImmutableSeq<Param> telescope();
}
