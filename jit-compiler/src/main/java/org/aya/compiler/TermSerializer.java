// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import org.aya.generic.NameGenerator;
import org.aya.generic.term.SortKind;
import org.aya.syntax.core.Closure;
import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.ConCall;
import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.core.term.call.FnCall;
import org.aya.syntax.core.term.call.PrimCall;
import org.aya.syntax.core.term.marker.TyckInternal;
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
public class TermSerializer extends AbstractSerializer<Term> {
  public static final String CLASS_LAMTERM = getName(LamTerm.class);
  public static final String CLASS_JITLAMTERM = getName(Closure.Jit.class);
  public static final String CLASS_APPTERM = getName(AppTerm.class);
  public static final String CLASS_SORTKIND = getName(SortKind.class);

  private final @NotNull ImmutableSeq<String> instantiates;
  private final @NotNull MutableMap<LocalVar, String> binds;

  public TermSerializer(@NotNull NameGenerator nameGen, @NotNull ImmutableSeq<String> instantiates) {
    this(new StringBuilder(), nameGen, instantiates);
  }

  public TermSerializer(@NotNull StringBuilder builder, @NotNull NameGenerator nameGen, @NotNull ImmutableSeq<String> instantiates) {
    super(builder, 0, nameGen);
    this.instantiates = instantiates;
    this.binds = MutableMap.create();
  }

  private void buildNew(@NotNull String qualifiedClassName, @NotNull ImmutableSeq<Term> terms) {
    doSerialize(STR."new \{qualifiedClassName}(", ")", terms);
  }

  private void buildNew(@NotNull String qualifiedClassName, @NotNull Runnable continuation) {
    builder.append(STR."new \{qualifiedClassName}(");
    continuation.run();
    builder.append(")");
  }

  private void sep() {
    builder.append(", ");
  }

  private void buildArray(@NotNull String typeName, @NotNull ImmutableSeq<Term> terms) {
    doSerialize(STR."new \{typeName}[] { ", " }", terms);
  }

  private void buildImmutableSeq(@NotNull String typeName, @NotNull ImmutableSeq<Term> terms) {
    if (terms.isEmpty()) {
      builder.append(STR."\{CLASS_IMMSEQ}.empty()");
    } else {
      doSerialize(STR."\{CLASS_IMMSEQ}.<\{typeName}>of(", ")", terms);
    }
  }

  private void doSerialize(@NotNull String prefix, @NotNull String suffix, @NotNull ImmutableSeq<Term> terms) {
    builder.append(prefix);
    IterableUtil.forEach(terms, this::sep, this::doSerialize);
    builder.append(suffix);
  }

  private TermSerializer doSerialize(@NotNull Term term) {
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
      case LamTerm lamTerm -> buildNew(CLASS_LAMTERM, () -> {
        serializeClosure(lamTerm.body());
      });
      case DataCall(var ref, var ulift, var args) -> buildNew(CLASS_JITDATACALL, () -> {
        builder.append(getInstance(getQualified(ref)));
        sep();
        builder.append(ulift);
        sep();
        buildImmutableSeq(CLASS_TERM, args);
      });
      case JitDataCall(var ref, var ulift, var args) -> buildNew(CLASS_JITDATACALL, () -> {
        builder.append(getInstance(getQualified(ref)));
        sep();
        builder.append(ulift);
        sep();
        buildImmutableSeq(CLASS_TERM, args);
      });
      case ConCall(var head, var args) -> buildNew(CLASS_JITCONCALL, () -> {
        builder.append(getInstance(getQualified(head.ref())));
        sep();
        builder.append(head.ulift());
        sep();
        buildImmutableSeq(CLASS_TERM, head.ownerArgs());
        sep();
        buildImmutableSeq(CLASS_TERM, args);
      });
      case JitConCall(var instance, var ulift, var ownerArgs, var conArgs) -> buildNew(CLASS_JITCONCALL, () -> {
        builder.append(getInstance(getQualified(instance)));
        sep();
        builder.append(ulift);
        sep();
        buildImmutableSeq(CLASS_TERM, ownerArgs);
        sep();
        buildImmutableSeq(CLASS_TERM, conArgs);
      });
      case CallLike.FnCallLike call -> {
        var ref = switch (call) {
          case JitFnCall fnCall -> getInstance(getQualified(fnCall.instance()));
          case FnCall fnCall -> getInstance(getQualified(fnCall.ref()));
        };

        var ulift = call.ulift();
        var args = call.args();

        builder.append(STR."\{ref}.invoke(");
        // serialize JitFnCall in case stuck
        buildNew(CLASS_JITFNCALL, () -> {
          builder.append(ref);
          sep();
          builder.append(ulift);
          sep();
          buildImmutableSeq(CLASS_TERM, args);
        });

        sep();
        buildArray(CLASS_TERM, args);
        builder.append(")");
        if (ulift > 0) builder.append(STR.".elevate(\{ulift})");
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
      case SigmaTerm sigmaTerm -> throw new UnsupportedOperationException("TODO");
      case TupTerm tupTerm -> throw new UnsupportedOperationException("TODO");
      case PrimCall primCall -> throw new UnsupportedOperationException("TODO");
      case IntegerTerm integerTerm -> throw new UnsupportedOperationException("TODO");
      case ListTerm listTerm -> throw new UnsupportedOperationException("TODO");
      case DimTerm dimTerm -> throw new UnsupportedOperationException("TODO");
      case DimTyTerm _ -> throw new UnsupportedOperationException("TODO");
      case EqTerm eqTerm -> throw new UnsupportedOperationException("TODO");
      default -> throw new IllegalStateException("Unexpected value: " + term);
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
