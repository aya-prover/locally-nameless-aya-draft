// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.def;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.concrete.stmt.decl.DataCon;
import org.aya.syntax.concrete.stmt.decl.DataDecl;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.xtt.EqTerm;
import org.aya.syntax.ref.DefVar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author ice1000, kiva
 */
public final class ConDef extends SubLevelDef implements ConDefLike {
  public final @NotNull DefVar<DataDef, DataDecl> dataRef;
  public final @NotNull DefVar<ConDef, DataCon> ref;
  public final @NotNull ImmutableSeq<Pat> pats;
  public final @Nullable EqTerm equality;

  /**
   * @param ownerTele See "/note/glossary.md"
   * @param selfTele  Ditto
   */
  public ConDef(
    @NotNull DefVar<DataDef, DataDecl> dataRef, @NotNull DefVar<ConDef, DataCon> ref,
    @NotNull ImmutableSeq<Pat> pats, @Nullable EqTerm equality, @NotNull ImmutableSeq<Param> ownerTele,
    @NotNull ImmutableSeq<Param> selfTele, @NotNull Term result, boolean coerce
  ) {
    super(ownerTele, selfTele, result, coerce);
    this.pats = pats;
    this.equality = equality;
    ref.core = this;
    this.dataRef = dataRef;
    this.ref = ref;
  }

  @Override public @NotNull DefVar<ConDef, DataCon> ref() { return ref; }
  @Override public @NotNull ImmutableSeq<Param> telescope() {
    return fullTelescope().toImmutableSeq();
  }
  @Override public boolean isEq() { return equality != null; }
}
