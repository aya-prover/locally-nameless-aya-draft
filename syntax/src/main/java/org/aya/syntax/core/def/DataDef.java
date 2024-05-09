// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.def;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.SortTerm;
import org.aya.syntax.ref.DefVar;
import org.jetbrains.annotations.NotNull;

/**
 * core data definition, corresponding to {@link TeleDecl.DataDecl}
 *
 * @author kiva
 */
public final class DataDef extends TopLevelDef<SortTerm> {
  public final @NotNull DefVar<DataDef, TeleDecl.DataDecl> ref;
  public final @NotNull ImmutableSeq<ConDef> body;

  public DataDef(
    @NotNull DefVar<DataDef, TeleDecl.DataDecl> ref, @NotNull ImmutableSeq<Param> telescope,
    SortTerm result, @NotNull ImmutableSeq<ConDef> body
  ) {
    super(telescope, result);
    ref.core = this;
    this.ref = ref;
    this.body = body;
  }

  public static @NotNull DefVar<DataDef, TeleDecl.DataDecl> fromCtor(@NotNull DefVar<ConDef, TeleDecl.DataCon> conHead) {
    if (conHead.core != null) return conHead.core.dataRef;
    else return conHead.concrete.dataRef;
  }

  public @NotNull DefVar<DataDef, TeleDecl.DataDecl> ref() {
    return ref;
  }

  // /**
  //  * @author ice1000
  //  */
  // public record CtorTelescopes(
  //   @NotNull ImmutableSeq<Term.Param> ownerTele,
  //   @NotNull ImmutableSeq<Term.Param> selfTele,
  //   @NotNull DataCall ret
  // ) {
  //   public CtorTelescopes(@NotNull CtorDef def) {
  //     this(def.ownerTele.map(Term.Param::implicitify), def.selfTele, (DataCall) def.result);
  //   }
  //
  //   public @NotNull Term toConCall(DefVar<CtorDef, TeleDecl.DataCtor> conVar, int ulift) {
  //     var body = new ConCall(fromCtor(conVar), conVar,
  //       ret.args(), ulift, selfTele.map(Term.Param::toArg));
  //     return LamTerm.make(ownerTele.view().concat(selfTele).map(LamTerm::param), body).rename();
  //   }
  // }
}
