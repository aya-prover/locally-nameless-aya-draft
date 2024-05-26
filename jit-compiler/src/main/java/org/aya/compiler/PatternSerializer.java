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
import org.aya.syntax.compile.JitCon;
import org.aya.syntax.core.def.ConDef;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.core.term.MetaPatTerm;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class PatternSerializer extends AbstractSerializer<ImmutableSeq<PatternSerializer.Matching>> {
  @FunctionalInterface
  public interface SuccessContinuation extends BiConsumer<PatternSerializer, Integer> { }
  public record Matching(@NotNull ImmutableSeq<Pat> patterns, @NotNull SuccessContinuation onSucc) { }

  public static final @NotNull String VARIABLE_RESULT = "result";
  public static final @NotNull String VARIABLE_STATE = "matchState";
  public static final @NotNull String VARIABLE_META_STATE = "metaState";

  static final @NotNull String CLASS_META_PAT = getName(MetaPatTerm.class);
  static final @NotNull String CLASS_PAT_MATCHER = getName(PatMatcher.class);
  // TODO: they are inner class, which contains '$'

  private final @NotNull String argName;
  private final @NotNull Consumer<PatternSerializer> onStuck;
  private final @NotNull Consumer<PatternSerializer> onMismatch;
  private int bindCount = 0;
  private final boolean inferMeta;

  public PatternSerializer(
    @NotNull StringBuilder builder,
    int indent,
    @NotNull NameGenerator nameGen,
    @NotNull String argName,
    boolean inferMeta,
    @NotNull Consumer<PatternSerializer> onStuck,
    @NotNull Consumer<PatternSerializer> onMismatch
  ) {
    super(builder, indent, nameGen);
    this.argName = argName;
    this.inferMeta = inferMeta;
    this.onStuck = onStuck;
    this.onMismatch = onMismatch;
  }

  /// region Serializing

  private void doSerialize(@NotNull Pat pat, @NotNull String term, @NotNull Runnable continuation) {
    switch (pat) {
      case Pat.Absurd _ -> buildIfElse("Panic.unreachable()", State.Stuck, continuation);
      case Pat.Bind _ -> {
        onMatchBind(term);
        continuation.run();
      }
      case Pat.Con con -> {
        var qualifiedName = getQualified(con);
        solveMeta(pat, term, (realTerm, mCon) ->
          // TODO: match IntegerTerm / ListTerm first
          buildIfInstanceElse(realTerm, CLASS_CONCALLLIKE, State.Stuck, mTerm ->
            buildIfElse(STR."\{getCallInstance(mTerm)} == \{getInstance(qualifiedName)}",
              State.Mismatch, () -> doSerialize(con.args().view(),
                fromImmutableSeq(STR."\{mTerm}.conArgs()",
                  con.args().size()).view(),
                mCon))), continuation);
      }
      case Pat.Meta _ -> Panic.unreachable();
      case Pat.ShapedInt shapedInt -> throw new UnsupportedOperationException("TODO");    // TODO
      case Pat.Tuple tuple -> buildIfElse(STR."\{term} instanceof TupleTerm", State.Stuck, () -> {
        throw new UnsupportedOperationException("TODO");
      });
    }
  }

  /**
   * Generate meta solving code
   *
   * @param pat               the pattern currently serialize
   * @param term              the parameter currently matched
   * @param matchContinuation do something if it is not a meta
   * @param continuation      do something if the matching success
   */
  private void solveMeta(
    @NotNull Pat pat,
    @NotNull String term,
    @NotNull BiConsumer<String, Runnable> matchContinuation,
    @NotNull Runnable continuation
  ) {
    if (inferMeta) {
      var tmpName = nameGen.nextName(null);
      buildUpdate(VARIABLE_META_STATE, "false");
      buildLocalVar(CLASS_TERM, tmpName, term);
      buildIfInstanceElse(term, CLASS_META_PAT, metaTerm -> {
        buildUpdate(tmpName, STR."\{CLASS_PAT_MATCHER}.realSolution(\{metaTerm})");
        // if the solution is still a meta, we solve it
        // this is a heavy work
        buildIfInstanceElse(tmpName, CLASS_META_PAT, stillMetaTerm -> {
          // TODO: we may store all Pattern in somewhere and refer them by something like `.conArgs().get(114514)`
          var exprializer = new PatternExprializer(nameGen);
          exprializer.serialize(pat);
          var doSolveMetaResult = STR."\{CLASS_PAT_MATCHER}.doSolveMeta(\{exprializer.result()}, \{stillMetaTerm}.meta())";
          appendLine(STR."\{CLASS_SER_UTILS}.copyTo(\{VARIABLE_RESULT}, \{doSolveMetaResult}, \{bindCount});");
          buildUpdate(VARIABLE_META_STATE, "true");
          // at this moment, the matching is complete,
          // but we still need to generate the code for normal matching
          // and it will increase bindCount
        }, null);

      }, null);

      buildIf(STR."! \{VARIABLE_META_STATE}", () -> matchContinuation.accept(tmpName, () ->
        buildUpdate(VARIABLE_META_STATE, "true")));

      buildIf(VARIABLE_META_STATE, continuation);
      // if failed, the previous matching already sets the matchState
    } else {
      matchContinuation.accept(term, continuation);
    }
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
    appendLine(STR."\{VARIABLE_RESULT}.set(\{bindCount++}, \{term});");
  }

  private int bindAmount(@NotNull Pat pat) {
    var acc = MutableIntValue.create();
    pat.consumeBindings((_, _) -> acc.increment());
    return acc.get();
  }

  static @NotNull String getQualified(@NotNull Pat.Con conLike) {
    return switch (conLike.ref()) {
      case JitCon jit -> getReference(jit);
      case ConDef.Delegate def -> getCoreReference(def.ref);
    };
  }

  /// endregion Java Source Code Generate API

  @Override public AyaSerializer<ImmutableSeq<Matching>> serialize(@NotNull ImmutableSeq<Matching> unit) {
    var bindSize = unit.mapToInt(ImmutableIntSeq.factory(),
      x -> x.patterns.view().foldLeft(0, (acc, p) -> acc + bindAmount(p)));
    int maxBindSize = bindSize.max();

    buildLocalVar(STR."\{CLASS_MUTSEQ}<\{CLASS_TERM}>", VARIABLE_RESULT, STR."\{CLASS_MUTSEQ}.fill(\{maxBindSize}, (\{CLASS_TERM}) null)");
    buildLocalVar("int", VARIABLE_STATE, "0");
    if (inferMeta) buildLocalVar("boolean", VARIABLE_META_STATE, "false");

    buildGoto(() -> unit.forEachIndexed((idx, clause) -> {
      var jumpCode = idx + 1;
      bindCount = 0;
      doSerialize(
        clause.patterns().view(),
        fromImmutableSeq(argName, clause.patterns.size()).view(),
        () -> updateState(jumpCode));

      buildIf(STR."\{VARIABLE_STATE} > 0", this::buildBreak);
    }));

    // -1 ..= unit.size()
    var range = IntRange.closed(-1, unit.size()).collect(ImmutableSeq.factory());
    buildSwitch(VARIABLE_STATE, range, state -> {
      switch (state) {
        case -1 -> onMismatch.accept(this);
        case 0 -> onStuck.accept(this);
        default -> {
          assert state > 0;
          var realIdx = state - 1;
          unit.get(realIdx).onSucc.accept(this, bindSize.get(realIdx));
        }
      }
    }, () -> buildPanic(null));

    return this;
  }
}
