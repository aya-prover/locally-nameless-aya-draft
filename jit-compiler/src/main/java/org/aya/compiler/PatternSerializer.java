// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.immutable.primitive.ImmutableIntSeq;
import kala.range.primitive.IntRange;
import kala.value.primitive.MutableIntValue;
import org.aya.generic.NameGenerator;
import org.aya.normalize.PatMatcher;
import org.aya.normalize.PatMatcher.State;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.core.term.MetaPatTerm;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class PatternSerializer extends AbstractSerializer<ImmutableSeq<PatternSerializer.Matching>> {
  public record Matching(@NotNull ImmutableSeq<Pat> patterns, @NotNull Consumer<PatternSerializer> onSucc) { }

  public static final @NotNull String VARIABLE_RESULT = "result";
  public static final @NotNull String VARIABLE_STATE = "matchState";
  public static final @NotNull String VARIABLE_META_STATE = "metaState";

  static final @NotNull String CLASS_METAPATTERM = MetaPatTerm.class.getName();
  static final @NotNull String CLASS_PATMATCHER = PatMatcher.class.getName();
  // TODO: they are inner class, which contains '$'
  static final @NotNull String CLASS_PAT_ABSURD = Pat.Absurd.class.getName();
  static final @NotNull String CLASS_PAT_BIND = Pat.Bind.class.getName();
  static final @NotNull String CLASS_PAT_JCON = Pat.JCon.class.getName();

  private final @NotNull String argName;
  private final @NotNull Consumer<PatternSerializer> onStuck;
  private final @NotNull Consumer<PatternSerializer> onMismatch;
  private int bindCount = 0;
  // TODO: impl inferMeta = false
  private final boolean inferMeta = true;

  public PatternSerializer(
    @NotNull StringBuilder builder,
    int indent,
    @NotNull NameGenerator nameGen,
    @NotNull String argName,
    @NotNull Consumer<PatternSerializer> onStuck,
    @NotNull Consumer<PatternSerializer> onMismatch
  ) {
    super(builder, indent, nameGen);
    this.argName = argName;
    this.onStuck = onStuck;
    this.onMismatch = onMismatch;
  }

  /// region Serializing

  private void doSerialize(@NotNull Pat pat, @NotNull String term, @NotNull Runnable continuation) {
    switch (pat) {
      case Pat.Absurd _ -> buildIfElse("Panic.unreachable()", State.Stuck, continuation);
      case Pat.Bind bind -> {
        onMatchBind(term);
        continuation.run();
      }
      case Pat.ConLike con -> {
        var qualifiedName = getQualified(con);
        solveMeta(pat, term, State.Mismatch, (realTerm, mCon) -> {
          buildIfInstanceElse(realTerm, CLASS_JITCONCALL, State.Stuck, mTerm -> {
            buildIfElse(STR."\{getCallInstance(mTerm)} == \{getInstance(qualifiedName)}",
              State.Mismatch, () -> doSerialize(con.args().view(),
                fromArray(STR."\{mTerm}.conArgs()",
                  con.args().size()).view(),
                mCon));
          });
        }, continuation);
      }
      case Pat.Meta _ -> Panic.unreachable();
      case Pat.ShapedInt shapedInt -> throw new UnsupportedOperationException("TODO");    // TODO
      case Pat.Tuple tuple -> {
        buildIfElse(STR."\{term} instanceof TupleTerm", State.Stuck, () -> {
          throw new UnsupportedOperationException("TODO");
        });
      }
    }
  }

  /**
   * Generate meta solving code
   */
  private void solveMeta(
    @NotNull Pat pat,
    @NotNull String term,
    @NotNull State stateOnFail,
    @NotNull BiConsumer<String, Runnable> matchContinuation,
    @NotNull Runnable continuation
  ) {
    var tmpName = nameGen.nextName(null);
    buildUpdate(VARIABLE_META_STATE, "false");
    buildLocalVar("Term", tmpName, term);
    buildIfInstanceElse(term, CLASS_METAPATTERM, metaTerm -> {
      buildUpdate(tmpName, STR."\{CLASS_PATMATCHER}.realSolution(\{metaTerm})");
      // if the solution is still a meta, we solve it
      // this is a heavy work
      buildIfInstanceElse(tmpName, CLASS_METAPATTERM, stillMetaTerm -> {
        // TODO: we may store all Pattern in somewhere and refer them by something like `.conArgs().get(114514)`
        var exprializer = new PatternExprializer(nameGen);
        exprializer.serialize(pat);
        var doSolveMetaResult = STR."\{CLASS_PATMATCHER}.doSolveMeta(\{exprializer.result()}, \{stillMetaTerm}.meta())";
        appendLine(STR."\{CLASS_SERIALIZEUTILS}.copyTo(\{VARIABLE_RESULT}, \{doSolveMetaResult}, \{bindCount});");
        buildUpdate(VARIABLE_META_STATE, "true");
        // at this moment, the matching is complete,
        // but we still need to generate the code for normal matching
        // and it will increase bindCount
      }, null);

    }, null);

    buildIf(STR."! \{VARIABLE_META_STATE}", () -> matchContinuation.accept(tmpName, () -> {
      buildUpdate(VARIABLE_META_STATE, "true");
    }));

    buildIfElse(VARIABLE_META_STATE, stateOnFail, continuation);
  }

  /**
   * @apiNote {@code pats.sizeEquals(terms)}
   */
  private void doSerialize(@NotNull SeqView<Pat> pats, @NotNull SeqView<String> terms, @NotNull Runnable continuation) {
    if (pats.isEmpty()) {
      continuation.run();
      return;
    }

    var pat = pats.getFirst();
    var term = terms.getFirst();
    doSerialize(pat, term, () -> doSerialize(pats.drop(1), terms.drop(1), continuation));
  }

  /// endregion Serializing

  /// region Java Source Code Generate API

  private void buildIfInstanceElse(
    @NotNull String term,
    @NotNull String type,
    @NotNull State state,
    @NotNull Consumer<String> continuation
  ) {
    buildIfInstanceElse(term, type, continuation, () -> updateState(-state.ordinal()));
  }

  private void buildIfElse(@NotNull String condition, @NotNull State state, @NotNull Runnable continuation) {
    buildIfElse(condition, continuation, () -> updateState(-state.ordinal()));
  }

  private void updateState(int state) {
    buildUpdate(VARIABLE_STATE, Integer.toString(state));
  }

  private void onMatchBind(@NotNull String term) {
    buildUpdate(STR."\{VARIABLE_RESULT}[\{bindCount++}]", term);
  }

  private int bindAmount(@NotNull Pat pat) {
    var acc = MutableIntValue.create();
    pat.consumeBindings((_, _) -> acc.increment());
    return acc.get();
  }

  static @NotNull String getQualified(@NotNull Pat.ConLike conLike) {
    return switch (conLike) {
      case Pat.Con con -> getQualified(con.ref());
      case Pat.JCon jCon -> getQualified(jCon.ref());
    };
  }

  /// endregion Java Source Code Generate API

  @Override public AyaSerializer<ImmutableSeq<Matching>> serialize(@NotNull ImmutableSeq<Matching> unit) {
    var bindSize = unit.mapToInt(ImmutableIntSeq.factory(),
      x -> x.patterns.view().foldLeft(0, (acc, p) -> acc + bindAmount(p)));
    int maxBindSize = bindSize.max();

    // TODO: fix hard code Term
    buildLocalVar("Term[]", VARIABLE_RESULT, STR."new Term[\{maxBindSize}]");
    buildLocalVar("int", VARIABLE_STATE, "0");
    buildLocalVar("boolean", VARIABLE_META_STATE, "false");

    buildGoto(() -> unit.forEachIndexed((idx, clause) -> {
      var jumpCode = idx + 1;
      doSerialize(
        clause.patterns().view(),
        fromArray(argName, clause.patterns.size()).view(),
        () -> updateState(jumpCode));

      buildIf(STR."\{VARIABLE_STATE} > 0", this::buildBreak);
    }));

    // -1 ..= unit.size()
    buildSwitch(VARIABLE_STATE, IntRange.closed(-1, unit.size()).collect(ImmutableSeq.factory()), state -> {
      switch (state) {
        case -1:
          onMismatch.accept(this);
          break;
        case 0:
          onStuck.accept(this);
          break;
        default:
          assert state > 0;
          unit.get(state - 1).onSucc.accept(this);
          break;
      }
    }, () -> buildPanic(null));

    return this;
  }
}