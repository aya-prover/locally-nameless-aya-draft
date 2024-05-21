// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableSeq;
import org.aya.generic.NameGenerator;
import org.aya.syntax.compile.JitCon;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;

public abstract class JitTeleSerializer<T> extends AbstractSerializer<T> {
  public static final String CLASS_JITCON = getQualified(JitCon.class);

  protected final @NotNull String superClass;

  protected JitTeleSerializer(
    @NotNull StringBuilder builder,
    int indent,
    @NotNull NameGenerator nameGen,
    @NotNull String superClass
  ) {
    super(builder, indent, nameGen);
    this.superClass = superClass;
  }

  protected void buildFramework(T unit, @NotNull Runnable continuation) {
    var className = getClassName(unit);
    buildClass(className, superClass, () -> {
      buildInstance(className);
      appendLine();     // make code more pretty
      // empty return type for constructor
      buildMethod(className, ImmutableSeq.empty(), "/*constructor*/", () -> buildConstructor(unit), false);
      appendLine();
      var iTerm = "i";
      var teleArgsTerm = "teleArgs";
      buildMethod("telescope", ImmutableSeq.of(
        new JitParam("i", "int"),
        new JitParam("teleArgs", "Term...")
      ), "Term", () -> buildTelescope(unit, iTerm, teleArgsTerm), true);
      appendLine();
      buildMethod("result", ImmutableSeq.of(
        new JitParam("teleArgs", "Term...")
      ), "Term", () -> buildResult(unit, teleArgsTerm), true);
      appendLine();
      continuation.run();
    });
  }

  protected abstract String getClassName(T unit);

  /**
   * @see org.aya.syntax.compile.JitTele
   */
  protected abstract void buildConstructor(T unit);

  /**
   * @see org.aya.syntax.compile.JitTele#telescope(int, Term...)
   */
  protected abstract void buildTelescope(T unit, @NotNull String iTerm, @NotNull String teleArgsTerm);

  protected void buildTelescope(@NotNull ImmutableSeq<Param> param, @NotNull String iTerm, @NotNull String teleArgsTerm) {

  }

  /**
   * @see org.aya.syntax.compile.JitTele#result(Term...)
   */
  protected abstract void buildResult(T unit, @NotNull String teleArgsTerm);

  public void buildSuperCall(@NotNull ImmutableSeq<String> args) {
    appendLine(STR."super(\{args.joinToString(", ")});");
  }
}
