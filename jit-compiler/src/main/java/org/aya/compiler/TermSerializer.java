// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableArray;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import org.aya.generic.NameGenerator;
import org.aya.generic.SortKind;
import org.aya.syntax.compile.*;
import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.ConCall;
import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.core.term.call.FnCall;
import org.aya.syntax.core.term.marker.TyckInternal;
import org.aya.syntax.core.term.xtt.CoeTerm;
import org.aya.syntax.core.term.xtt.PAppTerm;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

/**
 * Build the "constructor form" of {@link Term}, but in Java.
 */
public class TermSerializer extends AbstractSerializer<Term> {
  public static final String CLASS_LAMTERM = LamTerm.class.getName();
  public static final String CLASS_APPTERM = AppTerm.class.getName();
  public static final String CLASS_SORTTERM = SortTerm.class.getName();
  public static final String CLASS_SORTKIND = SortKind.class.getName();

  // telescope order, it can grow, i.e. lambda
  private final @NotNull MutableList<String> instantiates;

  public TermSerializer(@NotNull NameGenerator nameGen, @NotNull ImmutableSeq<String> instantiates) {
    this(new StringBuilder(), nameGen, instantiates);
  }

  public TermSerializer(@NotNull StringBuilder builder, @NotNull NameGenerator nameGen, @NotNull ImmutableSeq<String> instantiates) {
    super(builder, 0, nameGen);
    this.instantiates = MutableList.from(instantiates);
  }

  private void buildNew(@NotNull String qualifiedClassName, int level, @NotNull ImmutableSeq<Term> terms) {
    doSerialize(STR."new \{qualifiedClassName}(", ")", level, terms);
  }

  private void buildNew(@NotNull String qualifiedClassName, @NotNull Runnable continuation) {
    builder.append(STR."new \{qualifiedClassName}(");
    continuation.run();
    builder.append(")");
  }

  private void sep() {
    builder.append(", ");
  }

  private void buildArray(@NotNull String typeName, int level, @NotNull ImmutableSeq<Term> terms) {
    doSerialize(STR."new \{typeName}[] { ", " }", level, terms);
  }

  private void doSerialize(@NotNull String prefix, @NotNull String suffix, int level, @NotNull ImmutableSeq<Term> terms) {
    builder.append(prefix);

    if (terms.isNotEmpty()) {
      var it = terms.iterator();
      doSerialize(level, it.next());

      while (it.hasNext()) {
        sep();
        doSerialize(level, it.next());
      }
    }

    builder.append(suffix);
  }

  private void doSerialize(int level, @NotNull Term term) {
    switch (term) {
      case TyckInternal _ -> Panic.unreachable();
      case AppTerm appTerm -> buildNew(CLASS_APPTERM, level, ImmutableSeq.of(
        appTerm.fun(), appTerm.arg()
      ));
      case LocalTerm(var idx) -> {
        var subst = AyaRuntimeException.onRuntime(() -> instantiates.get(instantiates.size() - idx - 1));
        builder.append(subst);
      }
      // NbE!!!!
      case LamTerm lamTerm -> with(nameGen.nextName(null),
        () -> doSerialize(level + 1, lamTerm.body()));
      case JitLamTerm jLambda ->
        // TODO: instantiate with special LocalVar, and add them to {instantiates},
        // replace those LocalVar/FreeTerm while serializing `jLambda.apply(that LocalVar)`
        buildNew(CLASS_LAMTERM, level, ImmutableSeq.of(jLambda.toLam().body()));
      case DataCall(var ref, var ulift, var args) -> buildNew(CLASS_JITDATACALL, () -> {
        builder.append(getInstance(getQualified(ref))); sep();
        builder.append(ulift); sep();
        buildArray(CLASS_TERM, level, args);
      });
      case JitDataCall(var ref, var ulift, var args) -> buildNew(CLASS_JITDATACALL, () -> {
        builder.append(getInstance(getQualified(ref))); sep();
        builder.append(ulift); sep();
        buildArray(CLASS_TERM, level, ImmutableArray.Unsafe.wrap(args));
      });
      case ConCall(var head, var args) -> buildNew(CLASS_JITCONCALL, () -> {
        builder.append(getInstance(getQualified(head.ref()))); sep();
        builder.append(head.ulift()); sep();
        buildArray(CLASS_TERM, level, head.ownerArgs()); sep();
        buildArray(CLASS_TERM, level, args);
      });
      case JitConCall(var instance, var ulift, var ownerArgs, var conArgs) -> buildNew(CLASS_JITCONCALL, () -> {
        builder.append(getInstance(getQualified(instance))); sep();
        builder.append(ulift); sep();
        buildArray(CLASS_TERM, level, ImmutableArray.Unsafe.wrap(ownerArgs)); sep();
        buildArray(CLASS_TERM, level, ImmutableArray.Unsafe.wrap(conArgs));
      });
      case FnCall(var ref, var ulift, var args) -> buildNew(CLASS_JITFNCALL, () -> {
        builder.append(getInstance(getQualified(ref))); sep();
        builder.append(ulift); sep();
        buildArray(CLASS_TERM, level, args);
      });
      case JitFnCall(var ref, var ulift, var args) -> {
        builder.append(STR."\{getQualified(ref)}.invoke(");
        buildArray(CLASS_TERM, level, ImmutableArray.Unsafe.wrap(args));
        builder.append(")");
        if (ulift > 0) builder.append(STR.".elevate(\{ulift})");
      }
      case SortTerm(var kind, var ulift) -> buildNew(CLASS_SORTTERM, () -> {
        builder.append(STR."\{CLASS_SORTKIND}.\{kind.name()}"); sep();
        builder.append(ulift);
      });
      case CoeTerm coeTerm -> { }
      case ProjTerm projTerm -> { }
      case PAppTerm pAppTerm -> { }
      default -> throw new UnsupportedOperationException("TODO");
    }
  }

  private void with(@NotNull String subst, @NotNull Runnable continuation) {
    instantiates.append(subst);
    continuation.run();
    instantiates.removeLast();
  }

  @Override public AyaSerializer<Term> serialize(Term unit) {
    doSerialize(0, unit);
    return this;
  }
}
