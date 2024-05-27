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
import org.aya.syntax.core.term.repr.ListOps;
import org.aya.syntax.core.term.repr.ListTerm;
import org.aya.syntax.core.term.xtt.*;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

import static org.aya.compiler.AbstractSerializer.*;

/**
 * Build the "constructor form" of {@link Term}, but in Java.
 */
public class TermExprializer extends AbstractExprializer<Term> {
  public static final String CLASS_LAMTERM = getName(LamTerm.class);
  public static final String CLASS_JITLAMTERM = getName(Closure.Jit.class);
  public static final String CLASS_APPTERM = getName(AppTerm.class);
  public static final String CLASS_SORTKIND = getName(SortKind.class);
  public static final String CLASS_INTOPS = getName(IntegerOps.class);
  public static final String CLASS_LISTOPS = getName(ListOps.class);
  public static final String CLASS_INTEGER = getName(IntegerTerm.class);
  public static final String CLASS_LIST = getName(ListTerm.class);
  public static final String CLASS_INT_CONRULE = makeSub(CLASS_INTOPS, getName(IntegerOps.ConRule.class));
  public static final String CLASS_INT_FNRULE = makeSub(CLASS_INTOPS, getName(IntegerOps.FnRule.class));
  public static final String CLASS_LIST_CONRULE = makeSub(CLASS_LISTOPS, getName(ListOps.ConRule.class));
  public static final String CLASS_FNRULE_KIND = makeSub(CLASS_INT_FNRULE, getName(IntegerOps.FnRule.Kind.class));
  public static final String CLASS_RULEREDUCER = getName(RuleReducer.class);
  public static final String CLASS_RULE_CON = makeSub(CLASS_RULEREDUCER, getName(RuleReducer.Con.class));
  public static final String CLASS_RULE_FN = makeSub(CLASS_RULEREDUCER, getName(RuleReducer.Fn.class));

  private final @NotNull ImmutableSeq<String> instantiates;
  private final @NotNull MutableMap<LocalVar, String> binds;

  public TermExprializer(@NotNull NameGenerator nameGen, @NotNull ImmutableSeq<String> instantiates) {
    super(nameGen);
    this.instantiates = instantiates;
    this.binds = MutableMap.create();
  }

  private @NotNull String serializeApplicable(@NotNull Shaped.Applicable<?, ?> applicable) {
    return switch (applicable) {
      case IntegerOps.ConRule conRule -> makeNew(CLASS_INT_CONRULE, getInstance(getReference(conRule.ref())),
        doSerialize(conRule.zero())
      );
      case IntegerOps.FnRule fnRule -> makeNew(CLASS_INT_FNRULE,
        getInstance(getReference(fnRule.ref())),
        makeSub(CLASS_FNRULE_KIND, fnRule.kind().toString())
      );
      case ListOps.ConRule conRule -> makeNew(CLASS_LIST_CONRULE,
        getInstance(getReference(conRule.ref())),
        doSerialize(conRule.empty())
      );
      default -> Panic.unreachable();
    };
  }

  /**
   * This code requires that {@link FnCall} and {@link RuleReducer.Fn}:
   * {@code ulift} is the second parameter, {@code args} is the third parameter
   */
  private @NotNull String buildReducibleCall(
    @NotNull String reducible,
    @NotNull String callName,
    int ulift,
    @NotNull ImmutableSeq<ImmutableSeq<Term>> args
  ) {
    var seredArgs = args.map(x -> x.map(this::doSerialize));
    var seredSeq = seredArgs.map(x -> makeImmutableSeq(CLASS_TERM, x));
    var flatArgs = seredArgs.flatMap(x -> x);

    var callArgs = new String[seredSeq.size() + 2];
    callArgs[0] = reducible;
    callArgs[1] = "0";      // elevate later
    for (var i = 0; i < seredSeq.size(); ++i) {
      callArgs[i + 2] = seredSeq.get(i);
    }

    var elevate = ulift > 0 ? STR.".elevate(\{ulift})" : "";

    return STR."\{reducible}.invoke(\{makeNew(callName, callArgs)}, \{makeImmutableSeq(CLASS_TERM, flatArgs)})\{elevate}";
  }

  @Override
  protected @NotNull String doSerialize(@NotNull Term term) {
    return switch (term) {
      case FreeTerm bind -> {
        // It is possible that we meet bind here,
        // the serializer will instantiate some variable while serializing LamTerm
        var subst = binds.getOrNull(bind.name());
        if (subst == null) {
          throw new Panic(STR."No substitution for \{bind.name()} during serialization");
        }

        yield subst;
      }
      case TyckInternal i -> throw new Panic(i.getClass().toString());
      case AppTerm appTerm -> makeNew(CLASS_APPTERM, appTerm.fun(), appTerm.arg());
      case LocalTerm _ -> throw AyaRuntimeException.runtime(new Panic("LocalTerm"));
      case LamTerm lamTerm -> makeNew(CLASS_LAMTERM, serializeClosure(lamTerm.body()));
      case DataCall(var ref, var ulift, var args) -> makeNew(CLASS_JITDATACALL,
        getInstance(getReference(ref)),
        Integer.toString(ulift),
        serializeToImmutableSeq(CLASS_TERM, args)
      );
      case ConCall(var head, var args) -> makeNew(CLASS_JITCONCALL,
        getInstance(getReference(head.ref())),
        serializeToImmutableSeq(CLASS_TERM, head.ownerArgs()),
        Integer.toString(head.ulift()),
        serializeToImmutableSeq(CLASS_TERM, args)
      );
      case FnCall call -> {
        var ref = switch (call.ref()) {
          case JitFn jit -> getInstance(getReference(jit));
          case FnDef.Delegate def -> getInstance(getCoreReference(def.ref));
        };

        var ulift = call.ulift();
        var args = call.args();
        yield buildReducibleCall(ref, CLASS_JITFNCALL, ulift, ImmutableSeq.of(args));
      }
      case RuleReducer.Con conRuler -> buildReducibleCall(
        serializeApplicable(conRuler.rule()),
        CLASS_RULE_CON, conRuler.ulift(),
        ImmutableSeq.of(conRuler.dataArgs(), conRuler.conArgs())
      );
      case RuleReducer.Fn fnRuler -> buildReducibleCall(
        serializeApplicable(fnRuler.rule()),
        CLASS_RULE_FN, fnRuler.ulift(),
        ImmutableSeq.of(fnRuler.args())
      );
      case SortTerm(var kind, var ulift) -> makeNew(getName(SortTerm.class),
        makeSub(CLASS_SORTKIND, kind.name()),
        Integer.toString(ulift));
      case PiTerm(var param, var body) -> makeNew(getName(PiTerm.class),
        doSerialize(param),
        serializeClosure(body)
      );
      case CoeTerm(var type, var r, var s) -> makeNew(getName(CoeTerm.class),
        serializeClosure(type),
        doSerialize(r),
        doSerialize(s)
      );
      case ProjTerm(var of, var ix) -> makeNew(getName(ProjTerm.class),
        doSerialize(of),
        Integer.toString(ix)
      );
      case PAppTerm(var fun, var arg, var a, var b) -> makeNew(getName(PAppTerm.class),
        fun, arg, a, b
      );
      case EqTerm(var A, var a, var b) -> makeNew(getName(EqTerm.class),
        serializeClosure(A),
        doSerialize(a), doSerialize(b)
      );
      case DimTyTerm _ -> getInstance(getName(DimTyTerm.class));
      case DimTerm dim -> makeSub(getName(DimTerm.class), dim.name());
      case TupTerm(var items) -> makeNew(getName(TupTerm.class),
        serializeToImmutableSeq(CLASS_TERM, items)
      );
      case SigmaTerm sigmaTerm -> throw new UnsupportedOperationException("TODO");
      case PrimCall primCall -> throw new UnsupportedOperationException("TODO");
      case IntegerTerm(var repr, var zero, var suc, var type) -> makeNew(CLASS_INTEGER,
        Integer.toString(repr),
        getInstance(getReference(zero)),
        getInstance(getReference(suc)),
        doSerialize(type)
      );
      case ListTerm(var repr, var nil, var cons, var type) -> makeNew(CLASS_LIST,
        serializeToImmutableSeq(CLASS_TERM, repr),
        getInstance(getReference(nil)),
        getInstance(getReference(cons)),
        doSerialize(type)
      );
    };
  }

  // def f (A : Type) : Fn (a : A) -> A
  // (A : Type) : Pi(^0, IdxClosure(^1))
  // (A : Type) : Pi(^0, JitClosure(_ -> ^1))

  private @NotNull String with(@NotNull String subst, @NotNull Function<Term, String> continuation) {
    var bind = new LocalVar(subst);
    this.binds.put(bind, subst);
    var result = continuation.apply(new FreeTerm(bind));
    this.binds.remove(bind);
    return result;
  }

  private @NotNull String serializeClosure(@NotNull Closure body) {
    return serializeClosure(nameGen.nextName(null), body);
  }

  private @NotNull String serializeClosure(@NotNull String param, @NotNull Closure body) {
    return makeNew(CLASS_JITLAMTERM, STR."\{param} -> \{with(param, t -> doSerialize(body.apply(t)))}");
  }

  @Override public AyaSerializer<Term> serialize(Term unit) {
    binds.clear();
    var vars = ImmutableSeq.fill(instantiates.size(), i -> new LocalVar(STR."arg\{i}"));
    unit = unit.instantiateTeleVar(vars.view());
    vars.forEachWith(instantiates, binds::put);

    return super.serialize(unit);
  }
}
