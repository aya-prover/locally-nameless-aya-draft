// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.cli.compiler;

import kala.collection.immutable.ImmutableSeq;
import kala.tuple.Tuple2;
import org.aya.generic.NameGenerator;
import org.aya.syntax.ref.DefVar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public abstract class AbstractSerializer<T> implements AyaSerializer<T> {
  public record Param(@NotNull String name, @NotNull String type) { }

  protected final @NotNull StringBuilder builder;
  protected int indent;
  protected final @NotNull NameGenerator nameGen;

  protected AbstractSerializer(@NotNull StringBuilder builder, int indent, @NotNull NameGenerator nameGen) {
    this.builder = builder;
    this.indent = indent;
    this.nameGen = nameGen;
  }

  protected AbstractSerializer(@NotNull AbstractSerializer<?> other) {
    this(other.builder, other.indent, other.nameGen);
  }

  @Override
  public String result() {
    return builder.toString();
  }

  public void fillIndent() {
    if (indent == 0) return;
    builder.append("  ".repeat(indent));
  }

  public void runInside(@NotNull Runnable runnable) {
    indent++;
    runnable.run();
    indent--;
  }

  public void buildLocalVar(@NotNull String type, @NotNull String name, @Nullable String initial) {
    var update = initial == null ? "" : STR." = \{initial}";
    appendLine(STR."\{type} \{name}\{update};");
  }

  public void buildUpdate(@NotNull String lhs, @NotNull String rhs) {
    appendLine(STR."\{lhs} = \{rhs};");
  }

  public void buildIf(@NotNull String condition, @NotNull Runnable onSucc) {
    buildIfElse(condition, onSucc, null);
  }

  public void buildIfElse(@NotNull String condition, @NotNull Runnable onSucc, @Nullable Runnable onFailed) {
    appendLine(STR."if (\{condition}) {");
    runInside(onSucc);
    if (onFailed == null) {
      appendLine("}");
    } else {
      appendLine("} else {");
      runInside(onFailed);
      appendLine("}");
    }
  }

  /**
   * Generate java code that check whether {@param term} is an instance of {@param type}
   *
   * @param onSucc the argument is a local variable that has type {@param type} and identical equal to {@param term};
   */
  public void buildIfInstanceElse(
    @NotNull String term,
    @NotNull String type,
    @NotNull Consumer<String> onSucc,
    @Nullable Runnable onFailed
  ) {
    String name = nameGen.nextName(null);
    buildIfElse(STR."\{term} instanceof \{type} \{name}",
      () -> onSucc.accept(name),
      onFailed);
  }

  public void buildGoto(@NotNull Runnable continuation) {
    appendLine("do {");
    runInside(continuation);
    appendLine("} while (false);");
  }

  public void buildBreak() {
    appendLine("break;");
  }

  public void buildClass(@NotNull String className, @NotNull String superClass, @NotNull Runnable continuation) {
    appendLine(STR."class \{className} extends \{superClass} {");
    runInside(continuation);
    appendLine("}");
  }

  public void buildInstance(@NotNull String className) {
    appendLine(STR."public static final \{className} INSTANCE = new \{className}();");
  }

  public @NotNull ImmutableSeq<String> fromArray(@NotNull String term, int size) {
    return ImmutableSeq.fill(size, idx -> STR."\{term}[\{idx}]");
  }

  public void appendLine(@NotNull String string) {
    fillIndent();
    builder.append(string);
    builder.append('\n');
  }

  public void appendLine() {
    builder.append('\n');
  }

  public void buildSwitch(
    @NotNull String term,
    @NotNull ImmutableSeq<Tuple2<String, Runnable>> cases
  ) {
    buildSwitch(term, cases, () -> {
      appendLine("return Panic.unreachable();");
    });
  }

  public void buildSwitch(
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

  public void buildMethod(
    @NotNull String name,
    @NotNull ImmutableSeq<Param> params,
    @NotNull String returnType,
    @NotNull Runnable continuation,
    boolean override
  ) {
    if (override) {
      appendLine("@Override");
    }

    var paramStr = params.joinToString(", ", param -> STR."\{param.type()} \{param.name()}");
    appendLine(STR."public \{returnType} \{name}(\{paramStr}) {");
    runInside(continuation);
    appendLine("}");
  }

  protected static @NotNull String copyOf(@NotNull String arrayTerm, int length) {
    return STR."Arrays.copyOf(\{arrayTerm}, \{length})";
  }

  protected static @NotNull String getQualified(@NotNull DefVar<?, ?> ref) {
    return Objects.requireNonNull(ref.module).module().joinToString(".");
  }

  protected static @NotNull String getInstance(@NotNull String defName) {
    return STR."\{defName}.\{STATIC_FIELD_INSTANCE}";
  }

  protected static @NotNull String getCallInstance(@NotNull String term) {
    return STR."\{term}.\{FIELD_INSTANCE}()";
  }
}
