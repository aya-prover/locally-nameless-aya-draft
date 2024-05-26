// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
import com.javax0.sourcebuddy.Compiler;
import com.javax0.sourcebuddy.Fluent;
import kala.collection.immutable.ImmutableSeq;
import org.aya.compiler.AyaSerializer;
import org.aya.compiler.ModuleSerializer;
import org.aya.generic.NameGenerator;
import org.aya.syntax.core.def.DataDef;
import org.aya.syntax.core.def.FnDef;
import org.aya.syntax.core.def.TyckDef;
import org.aya.syntax.core.repr.AyaShape;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

public class CompileTester {
  private final @Language("Java") @NotNull String code;
  private final Fluent.AddSource compiler = Compiler.java();
  private Class<?> output = null;

  public CompileTester(@NotNull String code) { this.code = code; }

  public CompileTester(@NotNull ImmutableSeq<TyckDef> defs, @NotNull AyaShape.Factory factory) {
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

    this(code);
  }

  public void compile() throws ClassNotFoundException, Compiler.CompileException {
    output = compiler.from(code).compile().load().get();
  }

  public <T> Class<T> load(String... path) {

  }
}
