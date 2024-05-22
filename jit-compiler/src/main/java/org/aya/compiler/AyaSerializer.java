// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Result;
import org.aya.syntax.compile.JitConCall;
import org.aya.syntax.compile.JitDataCall;
import org.aya.syntax.compile.JitFnCall;
import org.aya.syntax.core.term.Term;
import org.aya.util.error.Panic;
import org.intellij.lang.annotations.Language;

import java.util.Arrays;

import static org.aya.compiler.AbstractSerializer.getQualified;

public interface AyaSerializer<T> {
  String STATIC_FIELD_INSTANCE = "INSTANCE";
  /**
   * @see JitConCall#instance()
   * @see JitDataCall#instance()
   * @see JitFnCall#instance()
   */
  String FIELD_INSTANCE = "instance";
  String CLASS_JITCONCALL = getQualified(JitConCall.class);
  String CLASS_JITDATACALL = getQualified(JitDataCall.class);
  String CLASS_JITFNCALL = getQualified(JitFnCall.class);
  String CLASS_IMMSEQ = getQualified(ImmutableSeq.class);
  String CLASS_TERM = getQualified(Term.class);
  String CLASS_PANIC = getQualified(Panic.class);

  String CLASS_RESULT = getQualified(Result.class);
  String CLASS_ARRAYS = getQualified(Arrays.class);
  String CLASS_BOOLEAN = getQualified(Boolean.class);

  @Language("Java") String IMPORT_BLOCK = """
    import org.aya.compiler.util.*;
    import org.aya.syntax.compile.*;
    import org.aya.syntax.core.*;
    import org.aya.syntax.core.term.*;
    import org.aya.syntax.core.term.repr.*;
    import org.aya.syntax.core.term.call.*;
    import org.aya.syntax.core.term.xtt.*;
    import org.aya.util.error.Panic;
    """;

  /**
   * Serialize the given {@param unit} to java source code,
   * the source code can be a class declaration or a expression, depends on the type of unit.
   */
  AyaSerializer<T> serialize(T unit);
  String result();
}
