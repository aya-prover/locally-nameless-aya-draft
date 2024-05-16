// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.def;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.DefVar;
import org.jetbrains.annotations.NotNull;

/**
 * @author ice1000, kiva
 */
public final class ConDef extends SubLevelDef {
  public final @NotNull DefVar<DataDef, TeleDecl.DataDecl> dataRef;
  public final @NotNull DefVar<ConDef, TeleDecl.DataCon> ref;
  public final @NotNull ImmutableSeq<Pat> pats;

  /**
   * @param ownerTele See "/note/glossary.md"
   * @param selfTele  Ditto
   */
  public ConDef(
    @NotNull DefVar<DataDef, TeleDecl.DataDecl> dataRef, @NotNull DefVar<ConDef, TeleDecl.DataCon> ref,
    @NotNull ImmutableSeq<Pat> pats,
    @NotNull ImmutableSeq<Param> ownerTele, @NotNull ImmutableSeq<Param> selfTele,
    @NotNull Term result, boolean coerce
  ) {
    super(ownerTele, selfTele, result, coerce);
    this.pats = pats;
    ref.core = this;
    this.dataRef = dataRef;
    this.ref = ref;
  }

  public @NotNull DefVar<ConDef, TeleDecl.DataCon> ref() {
    return ref;
  }

  @Override public @NotNull ImmutableSeq<Param> telescope() {
    return fullTelescope().toImmutableSeq();
  }
}
