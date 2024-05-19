// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.cli.compiler;

import org.jetbrains.annotations.NotNull;

// TODO
public abstract class ClassTargetSerializer<T> implements AyaSerializer<T> {
  protected final @NotNull StringBuilder builder = new StringBuilder();
  protected final @NotNull String superClassName;
  protected final @NotNull String className;
  protected int indent = 0;

  protected ClassTargetSerializer(@NotNull String className, @NotNull String superClassName) {
    this.superClassName = superClassName;
    this.className = className;
  }

  protected void classDef(@NotNull Runnable block) {
    classBegin();
    indent ++;
    classInstance();
    block.run();
    indent --;
    classEnd();
  }

  protected void fillIndent() {
    if (indent == 0) return;

    builder.append("  ".repeat(indent));
  }

  protected void classInstance() {
    fillIndent();
    builder.append(STR."public static final \{className} INSTANCE = new \{className}();\n");
  }

  protected void classBegin() {
    fillIndent();
    builder.append(STR."public final class \{className} extends \{superClassName} {\n");
  }

  protected void classEnd() {
    fillIndent();
    builder.append("}\n");
  }
}
