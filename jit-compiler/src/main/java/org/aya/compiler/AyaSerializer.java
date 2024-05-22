// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Result;
import org.aya.compiler.util.SerializeUtils;
import org.aya.syntax.compile.JitConCall;
import org.aya.syntax.compile.JitDataCall;
import org.aya.syntax.compile.JitFnCall;
import org.aya.syntax.core.term.Term;
import org.aya.util.error.Panic;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static org.aya.compiler.AbstractSerializer.getName;

/**
 * <h1>Serializing</h1>
 * AyaSerializer will serialize some {@link T} to some (java) class or expression, depends on what {@link T} is.
 *
 * <h2>File Structure</h2>
 * Each aya module will be serialized to a java file,
 * each {@link org.aya.syntax.concrete.stmt.decl.Decl} will be serialized to a nested class.
 * The class name should have form {@code AYA_QUALIFIED_NAME}.
 * For example, a module {@code Data::Vec} and a data type definition {@code Vec} in it
 * should have name {@code AYA_Data_Vec} and {@code AYA_Data_Vec_Vec}, respectively.
 * This avoids from the serialized class covering some importing.
 * We can use those importing by qualified name, but that makes the output ugly.
 */
public interface AyaSerializer<T> {
  String STATIC_FIELD_INSTANCE = "INSTANCE";
  /**
   * @see JitConCall#instance()
   * @see JitDataCall#instance()
   * @see JitFnCall#instance()
   */
  String FIELD_INSTANCE = "instance";
  String CLASS_JITCONCALL = getName(JitConCall.class);
  String CLASS_JITDATACALL = getName(JitDataCall.class);
  String CLASS_IMMSEQ = getName(ImmutableSeq.class);
  String CLASS_TERM = getName(Term.class);
  String CLASS_PANIC = getName(Panic.class);

  String CLASS_SERIALIZEUTILS = getName(SerializeUtils.class);
  String CLASS_RESULT = getName(Result.class);
  String CLASS_ARRAYS = getName(Arrays.class);
  String CLASS_BOOLEAN = getName(Boolean.class);

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
