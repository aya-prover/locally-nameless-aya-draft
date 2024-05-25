// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.def;

import kala.collection.immutable.ImmutableSeq;
import org.aya.generic.AyaDocile;
import org.aya.prettier.CorePrettier;
import org.aya.pretty.doc.Doc;
import org.aya.syntax.compile.JitTele;
import org.aya.syntax.compile.JitDef;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.PiTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.DefVar;
import org.aya.syntax.ref.ModulePath;
import org.aya.util.ForLSP;
import org.aya.util.binop.Assoc;
import org.aya.util.prettier.PrettierOptions;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A well-typed generic definition,
 * used before JIT compilation.
 *
 * @author zaoqi
 */
public sealed interface TyckDef extends AyaDocile, AnyDef permits SubLevelDef, TopLevelDef {
  @Override default @NotNull Doc toDoc(@NotNull PrettierOptions options) {
    return new CorePrettier(options).def(this);
  }

  //region Pretty & IDE only APIs
  /**
   * For pretty printing and IDE only, same for defTele and defResult.
   * This does not work well with compiled Aya.
   */
  @ForLSP static @NotNull Term defType(@NotNull DefVar<? extends TyckDef, ? extends Decl> defVar) {
    var signature = Objects.requireNonNull(defVar.signature);
    return PiTerm.make(signature.rawParams().map(Param::type), signature.result());
  }

  static @NotNull Term defType(@NotNull AnyDef var) {
    return switch (var) {
      case TyckDef tyckDef -> defType(tyckDef.ref());
      case JitDef jitDef -> jitDef.makePi();
    };
  }
  //endregion

  static @NotNull JitTele defSignature(@NotNull DefVar<? extends TyckDef, ? extends Decl> defVar) {
    if (defVar.core != null) return new JitTele.LocallyNameless(defVar.core.telescope(), defVar.core.result());
    // guaranteed as this is already a core term
    var signature = defVar.signature;
    assert signature != null : defVar.name();
    return new JitTele.LocallyNameless(signature.rawParams(), signature.result());
  }

  static @NotNull JitTele defSignature(@NotNull AnyDef def) {
    return switch (def) {
      case TyckDef tyckDef -> defSignature(tyckDef.ref());
      case JitDef jitDef -> jitDef;
    };
  }

  @NotNull DefVar<? extends TyckDef, ?> ref();
  @NotNull Term result();
  @NotNull ImmutableSeq<Param> telescope();

  @Override default @NotNull ModulePath fileModule() {
    var fileModule = ref().fileModule;
    assert fileModule != null;
    return fileModule;
  }

  @Override default @NotNull ModulePath module() {
    var module = ref().module;
    assert module != null;
    return module;
  }

  @Override
  default @NotNull String name() {
    return ref().name();
  }

  @Override
  default @NotNull Assoc assoc() {
    return ref().assoc();
  }
}
