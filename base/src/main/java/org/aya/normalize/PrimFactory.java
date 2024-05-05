// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.normalize;

import kala.collection.Map;
import kala.collection.immutable.ImmutableMap;
import kala.collection.immutable.ImmutableSeq;
import kala.control.Option;
import kala.tuple.Tuple;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.PrimDef;
import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.PrimCall;
import org.aya.syntax.core.term.xtt.CoeTerm;
import org.aya.syntax.core.term.xtt.DimTerm;
import org.aya.syntax.core.term.xtt.DimTyTerm;
import org.aya.syntax.core.term.xtt.EqTerm;
import org.aya.syntax.ref.DefVar;
import org.aya.tyck.TyckState;
import org.aya.util.ForLSP;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.aya.syntax.core.def.PrimDef.*;
import static org.aya.syntax.core.term.SortTerm.Type0;

public final class PrimFactory {
  private final @NotNull Map<@NotNull ID, @NotNull PrimSeed> seeds;
  private final @NotNull EnumMap<@NotNull ID, @NotNull PrimDef> defs = new EnumMap<>(ID.class);

  public PrimFactory() {
    seeds = ImmutableMap.from(ImmutableSeq.of(
      stringType,
      intervalType,
      pathType,
      coe
    ).map(seed -> Tuple.of(seed.name, seed)));
  }

  @FunctionalInterface
  public interface Unfolder extends BiFunction<@NotNull PrimCall, @NotNull TyckState, @NotNull Term> {
  }

  public record PrimSeed(
    @NotNull ID name,
    @NotNull Unfolder unfold,
    @NotNull Function<@NotNull DefVar<PrimDef, TeleDecl.PrimDecl>, @NotNull PrimDef> supplier,
    @NotNull ImmutableSeq<@NotNull ID> dependency
  ) {
    public @NotNull PrimDef supply(@NotNull DefVar<PrimDef, TeleDecl.PrimDecl> ref) {
      return supplier.apply(ref);
    }
  }

  final @NotNull PrimSeed coe = new PrimSeed(ID.COE, (prim, _) -> {
    var args = prim.args();
    return new CoeTerm(args.get(2), args.get(0), args.get(1));
  }, ref -> {
    // coe (r s : I) (A : I -> Type) : A r -> A s
    var r = DimTyTerm.param("r");
    var s = DimTyTerm.param("s");
    var paramA = new Param("A", intervalToType(), true);
    var result = familyI2J(new LocalTerm(0), new LocalTerm(2), new LocalTerm(1));

    return new PrimDef(ref, ImmutableSeq.of(r, s, paramA), result, ID.COE);
  }, ImmutableSeq.of(ID.I));

  final @NotNull PrimSeed pathType = new PrimSeed(ID.PATH, (prim, _) -> {
    var args = prim.args();
    return new EqTerm(args.get(0), args.get(1), args.get(2));
  }, ref -> {
    // (A : I -> Type) (a : A 0) (b : A 1) : Type
    var paramA = new Param("A", intervalToType(), true);
    var paramLeft = new Param("a", AppTerm.make(new LocalTerm(0), DimTerm.I0), true);
    var paramRight = new Param("b", AppTerm.make(new LocalTerm(0), DimTerm.I1), true);
    var result = new EqTerm(new LocalTerm(2), new LocalTerm(1), new LocalTerm(0));
    return new PrimDef(ref, ImmutableSeq.of(paramA, paramLeft, paramRight), result, ID.PATH);
  }, ImmutableSeq.of(ID.I));

  final @NotNull PrimSeed stringType =
    new PrimSeed(ID.STRING, this::primCall,
      ref -> new PrimDef(ref, Type0, ID.STRING), ImmutableSeq.empty());

  /*
  private final @NotNull PrimSeed hcomp = new PrimSeed(ID.HCOMP, this::hcomp, ref -> {
    var varA = new LocalVar("A");
    var paramA = new Term.Param(varA, Type0, false);
    var restr = IntervalTerm.paramImplicit("phi");
    var varU = new LocalVar("u");
    var paramFuncU = new Term.Param(varU,
      new PiTerm(
        IntervalTerm.param(LocalVar.IGNORED),
        new PartialTyTerm(new RefTerm(varA), AyaRestrSimplifier.INSTANCE.isOne(restr.toTerm()))),
      true);
    var varU0 = new LocalVar("u0");
    var paramU0 = new Term.Param(varU0, new RefTerm(varA), true);
    var result = new RefTerm(varA);
    return new PrimDef(
      ref,
      ImmutableSeq.of(paramA, restr, paramFuncU, paramU0),
      result,
      ID.HCOMP
    );
  }, ImmutableSeq.of(ID.I));

  public final @NotNull PrimSeed stringConcat =
    new PrimSeed(ID.STRCONCAT, Initializer::concat, ref -> new PrimDef(
      ref,
      ImmutableSeq.of(
        new Term.Param(new LocalVar("str1"), getCall(ID.STRING, ImmutableSeq.empty()), true),
        new Term.Param(new LocalVar("str2"), getCall(ID.STRING, ImmutableSeq.empty()), true)
      ),
      getCall(ID.STRING, ImmutableSeq.empty()),
      ID.STRCONCAT
    ), ImmutableSeq.of(ID.STRING));

  private static @NotNull Term concat(@NotNull PrimCall prim, @NotNull TyckState state) {
    var first = prim.args().get(0).normalize(state, NormalizeMode.WHNF);
    var second = prim.args().get(1).normalize(state, NormalizeMode.WHNF);

    if (first instanceof StringTerm str1 && second instanceof StringTerm str2) {
      return new StringTerm(str1.string() + str2.string());
    }

    return new PrimCall(prim.ref(), prim.ulift(), ImmutableSeq.of(
      new Arg<>(first, true), new Arg<>(second, true)));
  }

  private @NotNull Term hcomp(@NotNull PrimCall prim, @NotNull TyckState state) {
    var A = prim.args().get(0);
    var phi = prim.args().get(1);
    var u = prim.args().get(2);
    var u0 = prim.args().get(3);
    return new HCompTerm(A, AyaRestrSimplifier.INSTANCE.isOne(phi), u, u0);
  }
  */

  private @NotNull PrimCall primCall(@NotNull PrimCall prim, @NotNull TyckState tyckState) {return prim;}

  public final @NotNull PrimSeed intervalType = new PrimSeed(ID.I,
    ((_, _) -> DimTyTerm.INSTANCE),
    ref -> new PrimDef(ref, SortTerm.ISet, ID.I),
    ImmutableSeq.empty());

  public @NotNull PrimDef factory(@NotNull ID name, @NotNull DefVar<PrimDef, TeleDecl.PrimDecl> ref) {
    assert suppressRedefinition() || !have(name);
    var rst = seeds.get(name).supply(ref);
    defs.put(name, rst);
    return rst;
  }

  public @NotNull PrimCall getCall(@NotNull ID id, @NotNull ImmutableSeq<Term> args) {
    return new PrimCall(getOption(id).get().ref(), 0, args);
  }

  public @NotNull PrimCall getCall(@NotNull ID id) {
    return getCall(id, ImmutableSeq.empty());
  }

  public @NotNull Option<PrimDef> getOption(@NotNull ID name) {
    return Option.ofNullable(defs.get(name));
  }

  public boolean have(@NotNull ID name) {
    return defs.containsKey(name);
  }

  /** whether redefinition should be treated as error */
  @ForLSP public boolean suppressRedefinition() {
    return false;
  }

  public @NotNull PrimDef getOrCreate(@NotNull ID name, @NotNull DefVar<PrimDef, TeleDecl.PrimDecl> ref) {
    return getOption(name).getOrElse(() -> factory(name, ref));
  }

  public @NotNull Option<ImmutableSeq<@NotNull ID>> checkDependency(@NotNull ID name) {
    return seeds.getOption(name).map(seed -> seed.dependency().filterNot(this::have));
  }

  public @NotNull Term unfold(@NotNull PrimCall primCall, @NotNull TyckState state) {
    var id = primCall.id();
    return seeds.get(id).unfold.apply(primCall, state);
  }

  public void clear() {
    defs.clear();
  }

  public void clear(@NotNull ID name) {
    defs.remove(name);
  }
}
