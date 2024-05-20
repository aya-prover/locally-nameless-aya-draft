// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.cli.compiler;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.immutable.primitive.ImmutableIntSeq;
import kala.collection.mutable.MutableList;
import kala.tuple.Tuple;
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
  public record Matching(@NotNull ImmutableSeq<Pat> patterns, @NotNull Runnable onSucc) { }

  private static final @NotNull String VARIABLE_RESULT = "result";
  private static final @NotNull String VARIABLE_STATE = "matchState";
  private static final @NotNull String VARIABLE_META_STATE = "metaState";

  private static final @NotNull String CLASS_METAPATTERM = MetaPatTerm.class.getName();
  private static final @NotNull String CLASS_PATMATCHER = PatMatcher.class.getName();
  private static final @NotNull String CLASS_PAT_ABSURD = Pat.Absurd.class.getName();
  private static final @NotNull String CLASS_PAT_BIND = Pat.Bind.class.getName();
  private static final @NotNull String CLASS_PAT_JCON = Pat.JCon.class.getName();

  private final @NotNull String argName;
  private final @NotNull Runnable onStuck;
  private final @NotNull Runnable onDontMatch;
  private int bindCount = 0;

  public PatternSerializer(
    @NotNull StringBuilder builder,
    int indent,
    @NotNull NameGenerator nameGen,
    @NotNull String argName,
    @NotNull Runnable onStuck,
    @NotNull Runnable onDontMatch
  ) {
    super(builder, indent, nameGen);
    this.argName = argName;
    this.onStuck = onStuck;
    this.onDontMatch = onDontMatch;
  }

  /// region Exprize

  // TODO: How about another Pattern Serializer?
  private static void toSource(@NotNull StringBuilder acc, @NotNull ImmutableSeq<Pat> pats) {
    if (pats.isEmpty()) {
      acc.append("ImmutableSeq.empty()");
      return;
    }

    acc.append("ImmutableSeq.of(");

    var it = pats.iterator();
    toSource(acc, it.next());

    while (it.hasNext()) {
      acc.append(", ");
      toSource(acc, it.next());
    }

    acc.append(")");
  }

  private static void toSource(@NotNull StringBuilder acc, @NotNull Pat pat) {
    switch (pat) {
      case Pat.Absurd _ -> acc.append(getInstance(CLASS_PAT_ABSURD));
      case Pat.Bind bind -> {
        // it is safe to new a LocalVar, this method will be called when meta solving only,
        // but the meta solver will eat all LocalVar so that it will be happy.
        acc.append(STR."new \{CLASS_PAT_BIND}(new LocalVar(\"dogfood\"), ErrorTerm.DUMMY)");
      }
      case Pat.ConLike con -> {
        var instance = getQualified(con);

        acc.append(STR."new \{CLASS_PAT_JCON}(\{getInstance(instance)}, ");
        toSource(acc, con.args());
        acc.append(")");
      }
      case Pat.Meta meta -> Panic.unreachable();
      case Pat.Tuple tuple -> throw new UnsupportedOperationException();
      case Pat.ShapedInt shapedInt -> throw new UnsupportedOperationException();
    }
  }

  private static @NotNull String toSource(@NotNull Pat pat) {
    var builder = new StringBuilder();
    toSource(builder, pat);
    return builder.toString();
  }

  /// endregion Exprize

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
        var doSolveMetaResult = STR."\{CLASS_PATMATCHER}.doSolveMeta(\{toSource(pat)}, \{stillMetaTerm}.meta())";
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

  private static @NotNull String getQualified(@NotNull Pat.ConLike conLike) {
    return switch (conLike) {
      case Pat.Con con -> getQualified(con.ref());
      case Pat.JCon jCon -> getQualified(jCon.ref());
    };
  }

  /// endregion Java Source Code Generate API

  @Override public void serialize(@NotNull ImmutableSeq<Matching> unit) {
    var bindSize = unit.mapToInt(ImmutableIntSeq.factory(),
      x -> x.patterns.view().foldLeft(0, (acc, p) -> acc + bindAmount(p)));
    int maxBindSize = bindSize.max();
    var jumpTable = MutableList.of(
      Tuple.of("-1", onDontMatch),
      Tuple.of("0", onStuck)
    );

    buildLocalVar("Term[]", VARIABLE_RESULT, STR."new Term[\{maxBindSize}]");
    buildLocalVar("int", VARIABLE_STATE, "0");
    buildLocalVar("boolean", VARIABLE_META_STATE, "false");

    buildGoto(() -> {
      unit.forEachIndexed((idx, clause) -> {
        var jumpCode = idx + 1;
        jumpTable.append(Tuple.of(Integer.toString(jumpCode), clause.onSucc));

        doSerialize(
          clause.patterns().view(),
          fromArray(argName, clause.patterns.size()).view(),
          () -> {
            updateState(jumpCode);
          });

        buildIf(STR."\{VARIABLE_STATE} > 0", this::buildBreak);
      });
    });

    // if the execution arrive here, that means no clause is matched
    buildSwitch(VARIABLE_STATE, jumpTable.toImmutableSeq());
  }
}
