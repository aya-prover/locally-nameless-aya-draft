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
import org.aya.syntax.core.pat.Pat;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class PatternSerializer extends AbstractSerializer<ImmutableSeq<PatternSerializer.Matching>> {
  public record Matching(@NotNull ImmutableSeq<Pat> patterns, @NotNull Runnable onSucc) {}

  private static final @NotNull String VARIABLE_RESULT = "result";
  private static final @NotNull String VARIABLE_STATE = "matchState";

  enum State {
    Stuck,
    DontMatch
  }

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

  private static void toSource(@NotNull StringBuilder acc, @NotNull Pat pat) {
    switch (pat) {
      case Pat.Absurd _ -> acc.append("Pat.Absurd.INSTANCE");
      case Pat.Bind bind -> {
        // it is safe to new a LocalVar, this method will be called when meta solving only,
        // but the meta solver will eat all LocalVar so that it will be happy.
        acc.append("""
            new Pat.Bind(new LocalVar("dogfood"), ErrorTerm.DUMMY)""");
      }
      case Pat.Con con -> throw new UnsupportedOperationException();
      case Pat.Meta meta -> Panic.unreachable();
      case Pat.Tuple tuple -> throw new UnsupportedOperationException();
      case Pat.ShapedInt shapedInt -> throw new UnsupportedOperationException();
    }
  }

  private void doSerialize(@NotNull Pat pat, @NotNull String term, @NotNull Runnable continuation) {
    switch (pat) {
      case Pat.Absurd _ -> buildIfElse("Panic.unreachable()", State.Stuck, continuation);
      case Pat.Bind bind -> {
        onMatchBind(term);
        continuation.run();
      }
      case Pat.Con con -> {
        var qualifiedName = getQualified(con.ref());
        term = solveMeta(pat, term, continuation);
        buildIfInstanceElse(term, CLASS_JITCONCALL, State.Stuck, mTerm -> {
          buildIfElse(STR."\{getCallInstance(mTerm)} == \{getInstance(qualifiedName)}",
            State.DontMatch, () -> doSerialize(con.args().view(),
              fromArray(STR."\{mTerm}.conArgs()",
                con.args().size()).view(),
              continuation));
        });
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
   *
   * @return the name of new term that is ready to be solved.
   */
  private @NotNull String solveMeta(@NotNull Pat pat, @NotNull String term, @NotNull Runnable continuation) {
    var tmpName = nameGen.nextName(null);

    buildLocalVar("Term", tmpName, term);
    buildIf(STR."\{term} instanceof MetaPatTerm", () -> {
      appendLine(STR."\{tmpName} = PatMatcher.realSolution((MetaPatTerm) \{term});");
      buildIf(STR."\{tmpName} instanceof MetaPatTerm", () -> {
        continuation.run();
        appendLine("throw new UnsupportedOperationException();");
      });
    });

    return tmpName;
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

  private void buildIfInstanceElse(
    @NotNull String term,
    @NotNull String type,
    @NotNull PatternSerializer.State state,
    @NotNull Consumer<String> continuation
  ) {
    buildIfInstanceElse(term, type, continuation, () -> updateState(-state.ordinal()));
  }


  private void buildIfElse(@NotNull String condition, @NotNull PatternSerializer.State state, @NotNull Runnable continuation) {
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
