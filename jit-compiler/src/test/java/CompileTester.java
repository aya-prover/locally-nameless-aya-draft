// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.

import com.javax0.sourcebuddy.Compiler;
import com.javax0.sourcebuddy.Fluent;
import kala.collection.immutable.ImmutableSeq;
import org.aya.compiler.AyaSerializer;
import org.aya.compiler.ModuleSerializer;
import org.aya.generic.NameGenerator;
import org.aya.syntax.compile.JitDef;
import org.aya.syntax.core.def.DataDef;
import org.aya.syntax.core.def.FnDef;
import org.aya.syntax.core.def.TyckDef;
import org.aya.syntax.core.repr.AyaShape;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public class CompileTester {
  public final @Language("Java") @NotNull String code;
  private final Fluent.AddSource compiler = Compiler.java();
  private Class<?> output = null;

  public CompileTester(@NotNull String code) { this.code = code; }

  public static CompileTester make(@NotNull ImmutableSeq<TyckDef> defs, @NotNull AyaShape.Factory factory) {
    defs = defs.filter(x -> x instanceof FnDef || x instanceof DataDef);
    var out = new ModuleSerializer(new StringBuilder(), 0, new NameGenerator(), factory)
      .serialize(defs)
      .result();

    var code = STR."""
      package \{AyaSerializer.PACKAGE_BASE};

      \{AyaSerializer.IMPORT_BLOCK}

      public interface baka {
      \{out}
      }
      """;

    return new CompileTester(code);
  }

  public void compile() {
    try {
      output = compiler.from(STR."\{AyaSerializer.PACKAGE_BASE}.baka", code).compile().load().get();
    } catch (ClassNotFoundException | Compiler.CompileException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public <T> @NotNull Class<T> load(String... qualified) {
    try {
      return (Class<T>) output.getClassLoader()
        .loadClass(STR."\{AyaSerializer.PACKAGE_BASE}.\{ImmutableSeq.from(qualified).joinToString("$")}");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public <T extends JitDef> T getInstance(@NotNull Class<T> clazz) {
    try {
      Field field = clazz.getField(AyaSerializer.STATIC_FIELD_INSTANCE);
      field.setAccessible(true);
      return (T) field.get(null);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public <T extends JitDef> T loadInstance(String... qualified) {
    return getInstance(load(qualified));
  }
}
