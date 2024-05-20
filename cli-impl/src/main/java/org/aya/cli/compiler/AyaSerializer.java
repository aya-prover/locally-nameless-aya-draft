// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.cli.compiler;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

public interface AyaSerializer<T> {
  String STATIC_FIELD_INSTANCE = "INSTANCE";
  /**
   * @see org.aya.syntax.compile.JitConCall#instance()
   * @see org.aya.syntax.compile.JitDataCall#instance()
   * @see org.aya.syntax.compile.JitFnCall#instance()
   */
  String FIELD_INSTANCE = "instance";

  @Language("Java") String IMPORT_BLOCK = """
    import org.aya.syntax.compile.*;
    import org.aya.syntax.core.term.*;
    import org.aya.util.error.Panic;
    """;

  /**
   * Serialize the given {@param unit} to java source code,
   * the source code can be a class declaration or a expression, depends on the type of unit.
   */
  void serialize(T unit);

  String result();
}
