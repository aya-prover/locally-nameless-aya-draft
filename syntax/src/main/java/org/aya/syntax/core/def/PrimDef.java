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

  /*
  @FunctionalInterface
  interface Unfolder extends BiFunction<@NotNull PrimCall, @NotNull TyckState, @NotNull Term> {
  }

  record PrimSeed(
    @NotNull ID name,
    @NotNull Unfolder unfold,
    @NotNull Function<@NotNull DefVar<PrimDef, TeleDecl.PrimDecl>, @NotNull PrimDef> supplier,
    @NotNull ImmutableSeq<@NotNull ID> dependency
  ) {
    public @NotNull PrimDef supply(@NotNull DefVar<PrimDef, TeleDecl.PrimDecl> ref) {
      return supplier.apply(ref);
    }
  }
  */

  /** <code>I -> Type</code> */
  public static @NotNull Term intervalToType() {
    return new PiTerm(DimTyTerm.INSTANCE, Type0);
  }

  /** Let A be argument, then <code>A i -> A j</code> */
  public static @NotNull PiTerm familyI2J(Term term, Term i, Term j) {
    return new PiTerm(AppTerm.make(term, i), AppTerm.make(term, j));
  }

  public enum ID {
    STRING("String"),
    STRCONCAT("strcat"),
    I("I"),
    PARTIAL("Partial"),
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

    private final @NotNull EnumMap<@NotNull ID, @NotNull PrimDef> defs = new EnumMap<>(ID.class);

    // private final @NotNull Map<@NotNull ID, @NotNull PrimSeed> seeds;

    /*private final class Initializer {
      public final @NotNull PrimDef.PrimSeed coe = new PrimSeed(ID.COE, this::coe, ref -> {
        // coe (r s : I) (A : I -> Type) : A r -> A s
        var r = IntervalTerm.param("r");
        var s = IntervalTerm.param("s");
        var paramA = new Term.Param(new LocalVar("A"), intervalToType(), true);
        var result = familyI2J(paramA.toTerm(), r.toTerm(), s.toTerm());

        return new PrimDef(ref, ImmutableSeq.of(r, s, paramA), result, ID.COE);
      }, ImmutableSeq.of(ID.I));

      public final @NotNull PrimDef.PrimSeed sub = new PrimSeed(ID.SUB, this::primCall, ref -> {
        // Sub (A: Type) (φ: I) (u: Partial φ A) : Set
        var varA = new LocalVar("A");
        var varU = new LocalVar("u");
        var paramA = new Term.Param(varA, Type0, true);
        var phi = IntervalTerm.param("phi");
        var paramU = new Term.Param(varU, new PartialTyTerm(new RefTerm(varA), AyaRestrSimplifier.INSTANCE.isOne(phi.toTerm())), true);
        return new PrimDef(ref, ImmutableSeq.of(paramA, phi, paramU), Set0, ID.SUB);
      }, ImmutableSeq.of(ID.I, ID.PARTIAL));

      public final @NotNull PrimDef.PrimSeed outS = new PrimSeed(ID.OUTS, this::outS, ref -> {
        // outS {A} {φ} {u : Partial φ A} (Sub A φ u) : A
        var varA = new LocalVar("A");
        var varU = new LocalVar("u");
        var paramA = new Term.Param(varA, Type0, false);
        var paramPhi = IntervalTerm.paramImplicit("phi");
        var phi = paramPhi.toTerm();
        var A = new RefTerm(varA);
        var paramU = new Term.Param(varU, new PartialTyTerm(A, AyaRestrSimplifier.INSTANCE.isOne(phi)), false);

        var paramSub = new Term.Param(LocalVar.IGNORED,
          getCall(ID.SUB, ImmutableSeq.of(new Arg<>(A, true),
            new Arg<>(phi, true), new Arg<>(new RefTerm(varU), true))), true);
        return new PrimDef(ref, ImmutableSeq.of(paramA, paramPhi, paramU, paramSub), A, ID.OUTS);
      }, ImmutableSeq.of(ID.PARTIAL, ID.SUB));

      public final @NotNull PrimDef.PrimSeed inS = new PrimSeed(ID.INS, this::inS, ref -> {
        // inS {A} {φ} (u : A) : Sub A φ {|φ ↦ u|}
        var varA = new LocalVar("A");
        var varU = new LocalVar("u");
        var paramA = new Term.Param(varA, Type0, false);
        var paramPhi = IntervalTerm.paramImplicit("phi");
        var phi = paramPhi.toTerm();
        var A = new RefTerm(varA);
        var paramU = new Term.Param(varU, A, true);
        var u = new RefTerm(varU);

        var par = PartialTerm.from(phi, u, A);
        var ret = getCall(ID.SUB, ImmutableSeq.of(new Arg<>(A, true),
          new Arg<>(phi, true), new Arg<>(par, true)));
        return new PrimDef(ref, ImmutableSeq.of(paramA, paramPhi, paramU), ret, ID.INS);
      }, ImmutableSeq.of(ID.PARTIAL, ID.SUB));

      public final @NotNull PrimDef.PrimSeed stringType =
        new PrimSeed(ID.STRING, this::primCall,
          ref -> new PrimDef(ref, Type0, ID.STRING), ImmutableSeq.empty());

      @Contract("_, _ -> new")
      private @NotNull Term coe(@NotNull PrimCall prim, @NotNull TyckState state) {
        var r = prim.args().get(0).term();
        var s = prim.args().get(1).term();
        var type = prim.args().get(2).term();
        return new CoeTerm(type, r, s);
      }

      public final @NotNull PrimDef.PrimSeed partialType =
        new PrimSeed(ID.PARTIAL,
          (prim, state) -> {
            var iExp = prim.args().get(0).term();
            var ty = prim.args().get(1).term();

            return new PartialTyTerm(ty, AyaRestrSimplifier.INSTANCE.isOne(iExp));
          },
          ref -> new PrimDef(
            ref,
            ImmutableSeq.of(
              IntervalTerm.param("phi"),
              new Term.Param(new LocalVar("A"), Type0, true)
            ),
            SortTerm.Set0, ID.PARTIAL),
          ImmutableSeq.of(ID.I));
      private final @NotNull PrimDef.PrimSeed hcomp = new PrimSeed(ID.HCOMP, this::hcomp, ref -> {
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

      private Term inS(@NotNull PrimCall prim, @NotNull TyckState tyckState) {
        var phi = prim.args().get(1).term();
        var u = prim.args().getLast().term();
        return InTerm.make(phi, u);
      }

      private Term outS(@NotNull PrimCall prim, @NotNull TyckState tyckState) {
        var phi = prim.args().get(1).term();
        var par = prim.args().get(2).term();
        var u = prim.args().getLast().term();
        return OutTerm.make(phi, par, u);
      }

      public final @NotNull PrimDef.PrimSeed stringConcat =
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
        var first = prim.args().get(0).term().normalize(state, NormalizeMode.WHNF);
        var second = prim.args().get(1).term().normalize(state, NormalizeMode.WHNF);

        if (first instanceof StringTerm str1 && second instanceof StringTerm str2) {
          return new StringTerm(str1.string() + str2.string());
        }

        return new PrimCall(prim.ref(), prim.ulift(), ImmutableSeq.of(
          new Arg<>(first, true), new Arg<>(second, true)));
      }

      public final @NotNull PrimDef.PrimSeed intervalType =
        new PrimSeed(ID.I,
          ((prim, state) -> IntervalTerm.INSTANCE),
          ref -> new PrimDef(ref, SortTerm.ISet, ID.I),
          ImmutableSeq.empty());

      private @NotNull Term hcomp(@NotNull PrimCall prim, @NotNull TyckState state) {
        var A = prim.args().get(0).term();
        var phi = prim.args().get(1).term();
        var u = prim.args().get(2).term();
        var u0 = prim.args().get(3).term();
        return new HCompTerm(A, AyaRestrSimplifier.INSTANCE.isOne(phi), u, u0);
      }

      private @NotNull PrimCall primCall(@NotNull PrimCall prim, @NotNull TyckState tyckState) {return prim;}
    }

    public @NotNull PrimDef factory(@NotNull ID name, @NotNull DefVar<PrimDef, TeleDecl.PrimDecl> ref) {
      assert suppressRedefinition() || !have(name);
      var rst = seeds.get(name).supply(ref);
      defs.put(name, rst);
      return rst;
    }

    public @NotNull PrimCall getCall(@NotNull ID id, @NotNull ImmutableSeq<Arg<Term>> args) {
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

    *//** whether redefinition should be treated as error *//*
    @ForLSP public boolean suppressRedefinition() {
      return false;
    }

    public @NotNull PrimDef getOrCreate(@NotNull ID name, @NotNull DefVar<PrimDef, TeleDecl.PrimDecl> ref) {
      return getOption(name).getOrElse(() -> factory(name, ref));
    }

    public @NotNull Option<ImmutableSeq<@NotNull ID>> checkDependency(@NotNull ID name) {
      return seeds.getOption(name).map(seed -> seed.dependency().filterNot(this::have));
    }

    public @NotNull Term unfold(@NotNull ID name, @NotNull PrimCall primCall, @NotNull TyckState state) {
      return seeds.get(name).unfold.apply(primCall, state);
    }

    public void clear() {
      defs.clear();
    }

    public void clear(@NotNull ID name) {
      defs.remove(name);
    }
  }*/

  public final @NotNull DefVar<@NotNull PrimDef, TeleDecl.@NotNull PrimDecl> ref;
  public final @NotNull ID id;

  public @NotNull DefVar<@NotNull PrimDef, TeleDecl.@NotNull PrimDecl> ref() {
    return ref;
  }
}
