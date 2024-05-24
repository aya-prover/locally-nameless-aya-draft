// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.def;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.SortTerm;
import org.aya.syntax.ref.DefVar;
import org.jetbrains.annotations.NotNull;

/**
 * core data definition, corresponding to {@link Decl.DataDecl}
 *
 * @author kiva
 */
public final class DataDef extends TopLevelDef<SortTerm> {
  public final @NotNull DefVar<DataDef, Decl.DataDecl> ref;
  public final @NotNull ImmutableSeq<ConDef> body;

  public DataDef(
    @NotNull DefVar<DataDef, Decl.DataDecl> ref, @NotNull ImmutableSeq<Param> telescope,
    SortTerm result, @NotNull ImmutableSeq<ConDef> body
  ) {
    super(telescope, result);
    ref.core = this;
    this.ref = ref;
    this.body = body;
  }

  public static @NotNull DefVar<DataDef, Decl.DataDecl> fromCtor(@NotNull DefVar<ConDef, Decl.DataCon> conHead) {
    if (conHead.core != null) return conHead.core.dataRef;
    else return conHead.concrete.dataRef;
  }

  public @NotNull DefVar<DataDef, Decl.DataDecl> ref() { return ref; }
}
