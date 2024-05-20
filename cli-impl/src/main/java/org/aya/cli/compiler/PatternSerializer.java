// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.cli.compiler;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.immutable.primitive.ImmutableIntSeq;
import kala.tuple.Tuple;
import kala.value.primitive.MutableIntValue;
import org.aya.syntax.core.pat.Pat;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

public final class PatternSerializer extends AbstractSerializer<ImmutableSeq<PatternSerializer.Matching>> {
  public record Matching(@NotNull ImmutableSeq<Pat> patterns, @NotNull Runnable onSucc) { }

  private static final @NotNull String VARIABLE_RESULT = "result";
  private static final @NotNull String VARIABLE_STATE = "failState";

  enum State {
    Success,
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
    @NotNull String argName,
    @NotNull Runnable onStuck,
    @NotNull Runnable onDontMatch
  ) {
    super(builder, indent);
    this.argName = argName;
    this.onStuck = onStuck;
    this.onDontMatch = onDontMatch;
  }

  private void doSerialize(@NotNull Pat pat, @NotNull String term, @NotNull Runnable continuation) {
    switch (pat) {
      case Pat.Absurd _ -> buildIf("Panic.unreachable()", State.Success, continuation);
      case Pat.Bind bind -> {
        onMatchBind(term);
        continuation.run();
      }
      case Pat.Con con -> {
        // TODO: store Pat form in case MetaPatTerm
        var qualifiedName = con.ref().toString();    // TODO: SerState
        buildIf(STR."\{term} instanceof \{CLASS_JITCONCALL}", State.Stuck, () -> {
          buildIf(STR."((\{CLASS_JITCONCALL}) \{term}).\{FIELD_INSTANCE}() == \{qualifiedName}.\{STATIC_FIELD_INSTANCE}",
            State.DontMatch, () -> {
            doSerialize(con.args().view(),
              fromArray(STR."((\{CLASS_JITCONCALL}) \{term}).conArgs()",
                con.args().size()).view(),
              continuation);
          });
        });
      }
      case Pat.Meta _ -> Panic.unreachable();
      case Pat.ShapedInt shapedInt -> throw new UnsupportedOperationException("TODO");    // TODO
      case Pat.Tuple tuple -> {
        buildIf(STR."\{term} instanceof TupleTerm", State.Stuck, () -> {
          throw new UnsupportedOperationException("TODO");
        });
      }
    }
  }

  /**
   * @apiNote {@code pats.sizeEquals(terms)}
   */
  private void doSerialize(@NotNull SeqView<Pat> pats, @NotNull SeqView<String> terms, @NotNull Runnable continuation) {
    if (pats.isEmpty()) return;
    var pat = pats.getFirst();
    var term = terms.getFirst();
    doSerialize(pat, term, () -> doSerialize(pats.drop(1), terms.drop(1), continuation));
  }

  private void buildIf(@NotNull String condition, @NotNull PatternSerializer.State state, @NotNull Runnable continuation) {
    buildIf(condition, continuation, () -> {
      appendLine(STR."failState = \{state.ordinal()};");
    });
  }

  private void onMatchBind(@NotNull String term) {
    appendLine(STR."\{VARIABLE_RESULT}[\{bindCount++}] = \{term}");
  }

  private @NotNull ImmutableSeq<String> fromArray(@NotNull String term, int size) {
    return ImmutableSeq.fill(size, idx -> STR."\{term}[\{idx}]");
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

    appendLine(STR."Term[] \{VARIABLE_RESULT} = new Term[\{maxBindSize}];");
    appendLine(STR."int \{VARIABLE_STATE} = 0;");

    for (var clause : unit) {
      doSerialize(clause.patterns().view(), fromArray(argName, clause.patterns.size()).view(), clause.onSucc);
      // the user should return a value and exit this function
      appendLine(STR."assert \{VARIABLE_STATE} != 0;");
    }

    // if the execution arrive here, that means no clause is matched

    buildSwitch(VARIABLE_STATE, ImmutableSeq.of(
      Tuple.of("1", onStuck),
      Tuple.of("2", onDontMatch)
    ));
  }
}
