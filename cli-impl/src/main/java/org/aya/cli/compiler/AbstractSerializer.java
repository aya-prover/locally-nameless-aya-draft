// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.cli.compiler;

import kala.collection.immutable.ImmutableSeq;
import kala.tuple.Tuple2;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSerializer<T> implements AyaSerializer<T> {
  protected final @NotNull StringBuilder builder;
  protected int indent;

  protected AbstractSerializer(@NotNull StringBuilder builder, int indent) {
    this.builder = builder;
    this.indent = indent;
  }

  protected AbstractSerializer(@NotNull AbstractSerializer<?> other) {
    this(other.builder, other.indent);
  }

  @Override
  public String result() {
    return builder.toString();
  }

  protected void fillIndent() {
    if (indent == 0) return;
    builder.append("  ".repeat(indent));
  }

  protected void runInside(@NotNull Runnable runnable) {
    indent++;
    runnable.run();
    indent--;
  }

  protected void buildIf(@NotNull String condition, @NotNull Runnable onSucc, @NotNull Runnable onFailed) {
    appendLine(STR."if (\{condition}) {");
    runInside(onSucc);
    appendLine("} else {");
    runInside(onFailed);
    appendLine("}");
  }

  protected void buildClass(@NotNull String className, @NotNull String superClass, @NotNull Runnable continuation) {
    appendLine(STR."class \{className} extends \{superClass} {");
    runInside(continuation);
    appendLine("}");
  }

  protected void buildMethod(
    @NotNull String methodName,
    @NotNull String returnType,
    @NotNull ImmutableSeq<Tuple2<String, String>> telescope,
    @NotNull Runnable continuation
  ) {
    var teleStr = telescope.joinToString(", ", (pair) -> STR."\{pair.component1()} \{pair.component2()}");
    appendLine(STR."public \{returnType} \{methodName} (\{teleStr}) {");
    runInside(continuation);
    appendLine("}");
  }

  public void appendLine(@NotNull String string) {
    fillIndent();
    builder.append(string);
    builder.append('\n');
  }

  protected void buildSwitch(
    @NotNull String term,
    @NotNull ImmutableSeq<Tuple2<String, Runnable>> cases
  ) {
    buildSwitch(term, cases, () -> {
      appendLine("return Panic.unreachable();");
    });
  }

  protected void buildSwitch(
    @NotNull String term,
    @NotNull ImmutableSeq<Tuple2<String, Runnable>> cases,
    @NotNull Runnable defaultCase) {
    appendLine(STR."switch (\{term}) {");
    runInside(() -> {
      for (var kase : cases) {
        appendLine(STR."case \{kase.component1()}:");
        // the continuation should return
        runInside(kase.component2());
      }

      appendLine(STR."default:");
      runInside(defaultCase);
    });
    appendLine(STR."}");
  }
}
