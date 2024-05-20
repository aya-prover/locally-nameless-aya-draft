// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.cli.compiler;

import org.aya.syntax.compile.JitConCall;
import org.aya.syntax.compile.JitDataCall;
import org.aya.syntax.compile.JitFnCall;
import org.intellij.lang.annotations.Language;

public interface AyaSerializer<T> {
  String STATIC_FIELD_INSTANCE = "INSTANCE";
  /**
   * @see JitConCall#instance()
   * @see JitDataCall#instance()
   * @see JitFnCall#instance()
   */
  String FIELD_INSTANCE = "instance";
  String CLASS_JITCONCALL = JitConCall.class.getSimpleName();
  String CLASS_JITDATACALL = JitDataCall.class.getSimpleName();
  String CLASS_JITFNCALL = JitFnCall.class.getSimpleName();

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
