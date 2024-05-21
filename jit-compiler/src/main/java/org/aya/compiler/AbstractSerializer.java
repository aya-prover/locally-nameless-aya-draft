// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableSeq;
import org.aya.compiler.util.SerializeUtils;
import org.aya.generic.NameGenerator;
import org.aya.syntax.compile.JitTele;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.DefVar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public abstract class AbstractSerializer<T> implements AyaSerializer<T> {
  public record JitParam(@NotNull String name, @NotNull String type) { }

  public static final @NotNull String CLASS_SERIALIZEUTILS = SerializeUtils.class.getName();

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

  public void buildReturn(@NotNull String retWith) {
    appendLine(STR."return \{retWith};");
  }

  public void buildClass(@NotNull String className, @NotNull String superClass, @NotNull Runnable continuation) {
    appendLine(STR."class \{className} extends \{superClass} {");
    runInside(continuation);
    appendLine("}");
  }

  public void buildInstance(@NotNull String className) {
    appendLine(STR."public static final \{className} INSTANCE = new \{className}();");
  }

  public void buildPanic(@Nullable String message) {
    message = message == null ? "" : STR."\"\{message}\"";
    appendLine(STR."throw new \{CLASS_PANIC}(\{message});");
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

  public <R> void buildSwitch(
    @NotNull String term,
    @NotNull ImmutableSeq<R> cases,
    @NotNull Consumer<R> continuation,
    @NotNull Runnable defaultCase
  ) {
    appendLine(STR."switch (\{term}) {");
    runInside(() -> {
      for (var kase : cases) {
        appendLine(STR."case \{kase}:");
        // the continuation should return
        runInside(() -> continuation.accept(kase));
      }

      appendLine(STR."default:");
      runInside(defaultCase);
    });
    appendLine(STR."}");
  }

  public void buildMethod(
    @NotNull String name,
    @NotNull ImmutableSeq<JitParam> params,
    @NotNull String returnType,
    boolean override,
    @NotNull Runnable continuation
  ) {
    if (override) {
      appendLine("@Override");
    }

    var paramStr = params.joinToString(", ", param -> STR."\{param.type()} \{param.name()}");
    appendLine(STR."public \{returnType} \{name}(\{paramStr}) {");
    runInside(continuation);
    appendLine("}");
  }

  protected @NotNull String arrayFrom(@NotNull String type, @NotNull ImmutableSeq<String> elements) {
    var builder = new StringBuilder();
    builder.append("new ");
    builder.append(type);
    builder.append("[] { ");
    elements.joinTo(builder, ", ");
    builder.append(" }");
    return builder.toString();
  }

  protected @NotNull String serializeTermUnderTele(@NotNull Term term, @NotNull String argsTerm, int size) {
    return new TermSerializer(this.nameGen, fromArray(argsTerm, size))
        .serialize(term).result();
  }

  protected static @NotNull String isNull(@NotNull String term) {
    return STR."\{term} == null";
  }

  protected static @NotNull String copyOf(@NotNull String arrayTerm, int length) {
    return STR."Arrays.copyOf(\{arrayTerm}, \{length})";
  }

  protected static @NotNull String getQualified(@NotNull DefVar<?, ?> ref) {
    return Objects.requireNonNull(ref.module).module().view().appended(javify(ref))
      .joinToString(".");
  }

  // TODO: produce name like "AYA_Data_Vec_Vec" rather than just "Vec", so that they won't conflict with our import
  // then we can make all `CLASS_*` thing become unqualified.
  protected static @NotNull String getQualified(@NotNull JitTele ref) {
    return ref.getClass().getName();
  }

  protected static @NotNull String getInstance(@NotNull String defName) {
    return STR."\{defName}.\{STATIC_FIELD_INSTANCE}";
  }

  protected static @NotNull String getCallInstance(@NotNull String term) {
    return STR."\{term}.\{FIELD_INSTANCE}()";
  }

  /**
   * Turn an aya symbol name to a java symbol name
   */
  public static @NotNull String javify(@NotNull DefVar<?, ?> ayaName) {
    // TODO: impl
    return ayaName.name();
  }

  public static @NotNull String getQualified(@NotNull Class<?> clazz) {
    // TODO: maybe wrong impl
    return clazz.getName().replace('$', '.');
  }
}
