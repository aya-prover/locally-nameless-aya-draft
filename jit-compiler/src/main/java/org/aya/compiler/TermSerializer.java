// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableArray;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import org.aya.generic.NameGenerator;
import org.aya.generic.SortKind;
import org.aya.syntax.compile.*;
import org.aya.syntax.core.Closure;
import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.ConCall;
import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.core.term.call.FnCall;
import org.aya.syntax.core.term.call.PrimCall;
import org.aya.syntax.core.term.marker.TyckInternal;
import org.aya.syntax.core.term.repr.IntegerTerm;
import org.aya.syntax.core.term.xtt.*;
import org.aya.syntax.ref.LocalVar;
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
  public static final String CLASS_SORTTERM = getName(SortTerm.class);
  public static final String CLASS_SORTKIND = getName(SortKind.class);
  public static final String CLASS_PI = getName(PiTerm.class);

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

  private void doSerialize(@NotNull String prefix, @NotNull String suffix, @NotNull ImmutableSeq<Term> terms) {
    builder.append(prefix);

    // TODO: the code here is duplicated with somewhere, unify them
    if (terms.isNotEmpty()) {
      var it = terms.iterator();
      doSerialize(it.next());

      while (it.hasNext()) {
        sep();
        doSerialize(it.next());
      }
    }

    builder.append(suffix);
  }

  private void doSerialize(@NotNull Term term) {
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
      case LocalTerm(var idx) -> {
        var subst = AyaRuntimeException.onRuntime(() -> instantiates.get(instantiates.size() - idx - 1));
        builder.append(subst);
      }
      case LamTerm lamTerm -> {
        // TODO: instantiate with special LocalVar, and add them to {instantiates},
        // replace those LocalVar/FreeTerm while serializing `jLambda.apply(that LocalVar)`
        buildNew(CLASS_LAMTERM, () -> {
          serializeClosure(lamTerm.body());
        });
      }
      case DataCall(var ref, var ulift, var args) -> buildNew(CLASS_JITDATACALL, () -> {
        builder.append(getInstance(getQualified(ref))); sep();
        builder.append(ulift); sep();
        buildArray(CLASS_TERM, args);
      });
      case JitDataCall(var ref, var ulift, var args) -> buildNew(CLASS_JITDATACALL, () -> {
        builder.append(getInstance(getQualified(ref))); sep();
        builder.append(ulift); sep();
        buildArray(CLASS_TERM, ImmutableArray.Unsafe.wrap(args));
      });
      case ConCall(var head, var args) -> buildNew(CLASS_JITCONCALL, () -> {
        builder.append(getInstance(getQualified(head.ref()))); sep();
        builder.append(head.ulift()); sep();
        buildArray(CLASS_TERM, head.ownerArgs());
        sep();
        buildArray(CLASS_TERM, args);
      });
      case JitConCall(var instance, var ulift, var ownerArgs, var conArgs) -> buildNew(CLASS_JITCONCALL, () -> {
        builder.append(getInstance(getQualified(instance))); sep();
        builder.append(ulift); sep();
        buildArray(CLASS_TERM, ImmutableArray.Unsafe.wrap(ownerArgs));
        sep();
        buildArray(CLASS_TERM, ImmutableArray.Unsafe.wrap(conArgs));
      });
      case FnCall(var ref, var ulift, var args) -> {
        // the function we referred should be serialized together, so we can invoke it directly
        // TODO: need interface
        builder.append(STR."\{getInstance(getQualified(ref))}.invoke(");
        buildArray(CLASS_TERM, args);
        builder.append(")");
        if (ulift > 0) builder.append(STR.".elevate(\{ulift})");
      }
      case JitFnCall(var ref, var ulift, var args) -> {
        builder.append(STR."\{getInstance(getQualified(ref))}.invoke(");
        buildArray(CLASS_TERM, ImmutableArray.Unsafe.wrap(args));
        builder.append(")");
        if (ulift > 0) builder.append(STR.".elevate(\{ulift})");
      }
      case SortTerm(var kind, var ulift) -> buildNew(CLASS_SORTTERM, () -> {
        builder.append(STR."\{CLASS_SORTKIND}.\{kind.name()}"); sep();
        builder.append(ulift);
      });
      case PiTerm piTerm -> {
        buildNew(CLASS_PI, () -> {
          doSerialize(piTerm.param());
          sep();
          serializeClosure(piTerm.body());
        });
      }
      case CoeTerm _ -> throw new UnsupportedOperationException("TODO");
      case ProjTerm _ -> throw new UnsupportedOperationException("TODO");
      case PAppTerm _ -> throw new UnsupportedOperationException("TODO");
      case SigmaTerm sigmaTerm -> throw new UnsupportedOperationException("TODO");
      case TupTerm tupTerm -> throw new UnsupportedOperationException("TODO");
      case PrimCall primCall -> throw new UnsupportedOperationException("TODO");
      case IntegerTerm integerTerm -> throw new UnsupportedOperationException("TODO");
      case DimTerm dimTerm -> throw new UnsupportedOperationException("TODO");
      case DimTyTerm dimTyTerm -> throw new UnsupportedOperationException("TODO");
      case EqTerm eqTerm -> throw new UnsupportedOperationException("TODO");
    }
  }

  private void with(@NotNull String subst, @NotNull Consumer<Term> continuation) {
    var bind = new LocalVar(subst);
    this.binds.put(bind, subst);
    continuation.accept(new FreeTerm(bind));
    this.binds.remove(bind);
  }

  private void serializeClosure(@NotNull Closure body) {
    serializeClosure(nameGen.nextName(null), body);
  }

  private void serializeClosure(@NotNull String param, @NotNull Closure body) {
    buildNew(CLASS_JITLAMTERM, () -> {
      builder.append(STR."\{param} -> ");
      with(param, t -> doSerialize(body.apply(t)));
    });
  }

  @Override public AyaSerializer<Term> serialize(Term unit) {
    doSerialize(unit);
    return this;
  }
}
