// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.def;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.term.AppTerm;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.PiTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.xtt.DimTyTerm;
import org.aya.syntax.ref.DefVar;
import org.aya.util.error.WithPos;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Objects;

import static org.aya.syntax.core.term.SortTerm.Type0;

/**
 * @author ice1000
 */
public final class PrimDef extends TopLevelDef<Term> {
  public PrimDef(
    @NotNull DefVar<@NotNull PrimDef, TeleDecl.@NotNull PrimDecl> ref,
    @NotNull ImmutableSeq<Param> telescope,
    @NotNull Term result, @NotNull ID name
  ) {
    super(telescope, result);
    this.ref = ref;
    this.id = name;
    ref.core = this;
  }

  public PrimDef(@NotNull DefVar<@NotNull PrimDef, TeleDecl.@NotNull PrimDecl> ref, @NotNull Term result, @NotNull ID name) {
    this(ref, ImmutableSeq.empty(), result, name);
  }

  @Override public @NotNull ImmutableSeq<Param> telescope() {
    if (telescope.isEmpty()) return telescope;
    if (ref.concrete != null) {
      var signature = ref.concrete.signature;
      if (signature != null) return signature.param().map(WithPos::data);
    }
    return telescope;
  }

  @Override public @NotNull Term result() {
    if (ref.concrete != null) {
      var signature = ref.concrete.signature;
      if (signature != null) return signature.result();
    }
    return result;
  }

  /** <code>I -> Type</code> */
  public static @NotNull Term intervalToType() {
    return new PiTerm(DimTyTerm.INSTANCE, Type0);
  }

  /** Let A be argument, then <code>A i -> A j</code>. Handles index shifting. */
  public static @NotNull PiTerm familyI2J(Term term, Term i, Term j) {
    return new PiTerm(AppTerm.make(term, i), AppTerm.make(term, j).elevate(1));
  }

  public enum ID {
    STRING("String"),
    STRCONCAT("strcat"),
    I("I"),
    PARTIAL("Partial"),
    PATH("Path"),
    COE("coe"),
    HCOMP("hcomp");

    public final @NotNull
    @NonNls String id;

    @Override public String toString() {
      return id;
    }

    public static @Nullable ID find(@NotNull String id) {
      for (var value : PrimDef.ID.values())
        if (Objects.equals(value.id, id)) return value;
      return null;
    }

    ID(@NotNull String id) {
      this.id = id;
    }
  }

  /*
  public static class Factory {
    public Factory() {
      var init = new Initializer();
      seeds = ImmutableMap.from(ImmutableSeq.of(
        init.stringType,
        init.stringConcat,
        init.intervalType,
        init.partialType,
        init.coe,
        init.hcomp
      ).map(seed -> Tuple.of(seed.name, seed)));
    }
  */

  public final @NotNull DefVar<@NotNull PrimDef, TeleDecl.@NotNull PrimDecl> ref;
  public final @NotNull ID id;

  public @NotNull DefVar<@NotNull PrimDef, TeleDecl.@NotNull PrimDecl> ref() {
    return ref;
  }
}
