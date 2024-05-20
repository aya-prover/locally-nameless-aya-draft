// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.cli.compiler;

import kala.collection.immutable.ImmutableSeq;
import org.aya.generic.NameGenerator;
import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;

public abstract class JitTeleSerializer<T> extends AbstractSerializer<T> {
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
      buildMethod(className, ImmutableSeq.empty(), "", () -> buildConstructor(unit), false);
      appendLine();
      buildMethod("telescope", ImmutableSeq.of(
        new Param("i", "int"),
        new Param("teleArgs", "Term...")
      ), "Term", () -> buildTelescope(unit), true);
      appendLine();
      buildMethod("result", ImmutableSeq.of(
        new Param("teleArgs", "Term...")
      ), "Term", () -> buildResult(unit), true);
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
  protected abstract void buildTelescope(T unit);

  /**
   * @see org.aya.syntax.compile.JitTele#result(Term...)
   */
  protected abstract void buildResult(T unit);
}
