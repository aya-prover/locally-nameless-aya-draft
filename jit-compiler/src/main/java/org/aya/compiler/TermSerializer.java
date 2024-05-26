// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import org.aya.generic.NameGenerator;
import org.aya.generic.stmt.Shaped;
import org.aya.generic.term.SortKind;
import org.aya.syntax.compile.JitFn;
import org.aya.syntax.core.Closure;
import org.aya.syntax.core.def.FnDef;
import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.*;
import org.aya.syntax.core.term.marker.TyckInternal;
import org.aya.syntax.core.term.repr.IntegerOps;
import org.aya.syntax.core.term.repr.IntegerTerm;
import org.aya.syntax.core.term.repr.ListTerm;
import org.aya.syntax.core.term.xtt.*;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.IterableUtil;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Build the "constructor form" of {@link Term}, but in Java.
 */
public class TermSerializer extends AbstractExprializer<Term> {
  public static final String CLASS_LAMTERM = getName(LamTerm.class);
  public static final String CLASS_JITLAMTERM = getName(Closure.Jit.class);
  public static final String CLASS_APPTERM = getName(AppTerm.class);
  public static final String CLASS_SORTKIND = getName(SortKind.class);
  public static final String CLASS_INTOPS = getName(IntegerOps.class);
  public static final String CLASS_INTEGER = getName(IntegerTerm.class);
  public static final String CLASS_CONRULE = makeSub(CLASS_INTOPS, getName(IntegerOps.ConRule.class));
  public static final String CLASS_FNRULE = makeSub(CLASS_INTOPS, getName(IntegerOps.FnRule.class));
  public static final String CLASS_FNRULE_KIND = makeSub(CLASS_FNRULE, getName(IntegerOps.FnRule.Kind.class));
  public static final String CLASS_RULEREDUCER = getName(RuleReducer.class);
  public static final String CLASS_RULE_CON = makeSub(CLASS_RULEREDUCER, getName(RuleReducer.Con.class));
  public static final String CLASS_RULE_FN = makeSub(CLASS_RULEREDUCER, getName(RuleReducer.Fn.class));

  private final @NotNull ImmutableSeq<String> instantiates;
  private final @NotNull MutableMap<LocalVar, String> binds;

  public TermSerializer(@NotNull NameGenerator nameGen, @NotNull ImmutableSeq<String> instantiates) {
    this(new StringBuilder(), nameGen, instantiates);
  }

  public TermSerializer(@NotNull StringBuilder builder, @NotNull NameGenerator nameGen, @NotNull ImmutableSeq<String> instantiates) {
    super(builder, nameGen);
    this.instantiates = instantiates;
    this.binds = MutableMap.create();
  }

  private @NotNull TermSerializer serializeApplicable(@NotNull Shaped.Applicable<?, ?> applicable) {
    switch (applicable) {
      case IntegerOps.ConRule conRule -> buildNew(CLASS_CONRULE, () -> {
        var ref = getInstance(getReference(conRule.ref()));
        doSerialize(STR."\{ref}, ", "", ImmutableSeq.of(conRule.zero(), conRule.paramType()));
      });
      case IntegerOps.FnRule fnRule -> buildNew(CLASS_FNRULE, () -> {
        var ref = getInstance(getReference(fnRule.ref()));
        appendSep(ref);
        builder.append(makeSub(CLASS_FNRULE_KIND, fnRule.kind().toString()));
      });
      default -> Panic.unreachable();
    }

    return this;
  }

  /**
   * This code requires that {@link FnCall} and {@link RuleReducer.Fn}:
   * {@code ulift} is the second parameter, {@code args} is the third parameter
   */
  private @NotNull TermSerializer buildReducibleCall(
    @NotNull Runnable reducible,
    @NotNull String callName,
    int ulift,
    @NotNull ImmutableSeq<Term>... args
  ) {
    reducible.run();
    builder.append(".invoke(");
    // make fallback call
    buildNew(callName, () -> {
      reducible.run();
      sep();
      builder.append(0);
      sep();               // elevate later
      IterableUtil.forEach(ImmutableSeq.from(args), this::sep, t -> {
        buildImmutableSeq(CLASS_TERM, t);
      });
    });
    sep();
    // supply arguments
    buildImmutableSeq(CLASS_TERM, ImmutableSeq.from(args).flatMap(x -> x));
    builder.append(")");
    if (ulift > 0) builder.append(STR.".elevate(\{ulift})");

    return this;
  }

  @Override
  protected @NotNull TermSerializer doSerialize(@NotNull Term term) {
    switch (term) {
      case FreeTerm bind -> {
        // It is possible that we meet bind here,
        // the serializer will instantiate some variable while serializing LamTerm
        var subst = binds.getOrNull(bind.name());
        if (subst == null) {
          throw new Panic(STR."No substitution for \{bind.name()} during serialization");
        }

        builder.append(subst);
      }
      case TyckInternal _ -> Panic.unreachable();
      case AppTerm appTerm -> buildNew(CLASS_APPTERM, ImmutableSeq.of(
        appTerm.fun(), appTerm.arg()
      ));
      case LocalTerm(var idx) -> throw AyaRuntimeException.runtime(new Panic("LocalTerm"));
      case LamTerm lamTerm -> buildNew(CLASS_LAMTERM, () -> serializeClosure(lamTerm.body()));
      case DataCall(var ref, var ulift, var args) -> buildNew(CLASS_JITDATACALL, () -> {
        builder.append(getInstance(getReference(ref)));
        sep();
        builder.append(ulift);
        sep();
        buildImmutableSeq(CLASS_TERM, args);
      });
      case ConCall(var head, var args) -> buildNew(CLASS_JITCONCALL, () -> {
        builder.append(getInstance(getReference(head.ref())));
        sep();
        buildImmutableSeq(CLASS_TERM, head.ownerArgs());
        sep();
        builder.append(head.ulift());
        sep();
        buildImmutableSeq(CLASS_TERM, args);
      });
      case FnCall call -> {
        var ref = switch (call.ref()) {
          case JitFn jit -> getInstance(getReference(jit));
          case FnDef.Delegate def -> getInstance(getCoreReference(def.ref));
        };

        var ulift = call.ulift();
        var args = call.args();
        buildReducibleCall(() -> {
          builder.append(ref);
        }, CLASS_JITFNCALL, ulift, args);
      }
      case RuleReducer.Con conRuler -> {
        buildReducibleCall(() -> {
          serializeApplicable(conRuler.rule());
        }, CLASS_RULE_CON, conRuler.ulift(), conRuler.dataArgs(), conRuler.conArgs());
      }
      case SortTerm(var kind, var ulift) -> buildNew(getName(SortTerm.class), () -> {
        builder.append(STR."\{CLASS_SORTKIND}.\{kind.name()}"); sep();
        builder.append(ulift);
      });
      case PiTerm (var param, var body) -> buildNew(getName(PiTerm.class), () -> {
        doSerialize(param).sep();
        serializeClosure(body);
      });
      case CoeTerm(var type, var r, var s) -> buildNew(getName(CoeTerm.class), () -> {
        serializeClosure(type).sep();
        doSerialize(r).sep();
        doSerialize(s);
      });
      case ProjTerm(var of, var ix) -> buildNew(getName(ProjTerm.class), () -> {
        doSerialize(of).sep();
        builder.append(ix);
      });
      case PAppTerm(var fun, var arg, var a, var b) -> buildNew(getName(PAppTerm.class), () -> {
        doSerialize(fun).sep();
        doSerialize(arg).sep();
        doSerialize(a).sep();
        doSerialize(b);
      });
      case EqTerm(var A, var a, var b) -> buildNew(getName(EqTerm.class), () -> {
        serializeClosure(A).sep();
        doSerialize(a).sep();
        doSerialize(b);
      });
      case DimTyTerm _ -> builder.append(getName(DimTyTerm.class)).append(".INSTANCE");
      case DimTerm dim -> builder.append(getName(DimTerm.class)).append(".").append(dim.name());
      case SigmaTerm sigmaTerm -> throw new UnsupportedOperationException("TODO");
      case TupTerm tupTerm -> throw new UnsupportedOperationException("TODO");
      case PrimCall primCall -> throw new UnsupportedOperationException("TODO");
      case IntegerTerm integerTerm -> buildNew(CLASS_INTEGER, () -> {
        appendSep(Integer.toString(integerTerm.repr()));
        appendSep(getInstance(getReference(integerTerm.zero())));
        appendSep(getInstance(getReference(integerTerm.suc())));
        doSerialize(integerTerm.type());
      });
      case ListTerm listTerm -> throw new UnsupportedOperationException("TODO");
      default -> throw new IllegalStateException("Unexpected value: " + term.getClass());
    }
    return this;
  }

  // def f (A : Type) : Fn (a : A) -> A
  // (A : Type) : Pi(^0, IdxClosure(^1))
  // (A : Type) : Pi(^0, JitClosure(_ -> ^1))

  private void with(@NotNull String subst, @NotNull Consumer<Term> continuation) {
    var bind = new LocalVar(subst);
    this.binds.put(bind, subst);
    continuation.accept(new FreeTerm(bind));
    this.binds.remove(bind);
  }

  private TermSerializer serializeClosure(@NotNull Closure body) {
    serializeClosure(nameGen.nextName(null), body);
    return this;
  }

  private void serializeClosure(@NotNull String param, @NotNull Closure body) {
    buildNew(CLASS_JITLAMTERM, () -> {
      builder.append(STR."\{param} -> ");
      with(param, t -> doSerialize(body.apply(t)));
    });
  }

  @Override public AyaSerializer<Term> serialize(Term unit) {
    binds.clear();
    var vars = ImmutableSeq.fill(instantiates.size(), i -> new LocalVar(STR."arg\{i}"));
    unit = unit.instantiateTeleVar(vars.view());

    vars.forEachWith(instantiates, binds::put);

    doSerialize(unit);
    return this;
  }
}
