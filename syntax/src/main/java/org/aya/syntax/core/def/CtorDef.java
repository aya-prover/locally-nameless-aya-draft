// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.def;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.DefVar;
import org.jetbrains.annotations.NotNull;

/**
 * @author ice1000, kiva
 */
public final class CtorDef extends SubLevelDef {
  public final @NotNull DefVar<DataDef, TeleDecl.DataDecl> dataRef;
  public final @NotNull DefVar<CtorDef, TeleDecl.DataCtor> ref;

  /**
   * @param ownerTele See "/note/glossary.md"
   * @param selfTele  Ditto
   */
  public CtorDef(
    @NotNull DefVar<DataDef, TeleDecl.DataDecl> dataRef, @NotNull DefVar<CtorDef, TeleDecl.DataCtor> ref,
    @NotNull ImmutableSeq<Param> ownerTele, @NotNull ImmutableSeq<Param> selfTele,
    @NotNull Term result, boolean coerce
  ) {
    super(ownerTele, selfTele, result, coerce);
    ref.core = this;
    this.dataRef = dataRef;
    this.ref = ref;
  }

  public @NotNull DefVar<CtorDef, TeleDecl.DataCtor> ref() {
    return ref;
  }

  @Override public @NotNull ImmutableSeq<Param> telescope() {
    return fullTelescope().toImmutableSeq();
  }
}
