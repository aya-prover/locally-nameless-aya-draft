// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.

import com.javax0.sourcebuddy.Compiler;
import kala.collection.immutable.ImmutableSeq;
import org.aya.compiler.*;
import org.aya.generic.NameGenerator;
import org.aya.prettier.AyaPrettierOptions;
import org.aya.producer.AyaParserImpl;
import org.aya.resolve.ResolveInfo;
import org.aya.resolve.context.EmptyContext;
import org.aya.resolve.module.DumbModuleLoader;
import org.aya.resolve.module.ModuleCallback;
import org.aya.syntax.compile.JitCon;
import org.aya.syntax.compile.JitConCall;
import org.aya.syntax.compile.JitFn;
import org.aya.syntax.concrete.stmt.decl.DataCon;
import org.aya.syntax.core.Closure;
import org.aya.syntax.core.def.ConDef;
import org.aya.syntax.core.def.DataDef;
import org.aya.syntax.core.def.Def;
import org.aya.syntax.core.def.FnDef;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.core.term.*;
import org.aya.syntax.ref.DefVar;
import org.aya.syntax.ref.LocalVar;
import org.aya.syntax.ref.ModulePath;
import org.aya.util.error.SourceFile;
import org.aya.util.reporter.ThrowingReporter;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public class CompileTest {
  @Test public void test0() {
    DefVar<ConDef, DataCon> S = DefVar.empty("S");
    S.module = ModulePath.of("Data", "Nat", "Nat");

    var cls0 = ImmutableSeq.<Pat>of(
      new Pat.Bind(new LocalVar("A"), ErrorTerm.DUMMY),
      new Pat.Con(S, ImmutableSeq.of(new Pat.Bind(new LocalVar("n"), ErrorTerm.DUMMY)), null, null)
    );

    var builder = new StringBuilder();
    var ser = new PatternSerializer(builder, 0, new NameGenerator(), "args", true,
      s -> s.appendLine("System.out.println(\"Hello, world!\");"),
      s -> s.appendLine("System.out.println(\"Unhello, world!\");"));

    ser.serialize(ImmutableSeq.of(new PatternSerializer.Matching(cls0,
        (s, _) -> s.appendLine("System.out.println(\"Good, world!\");"))));

    System.out.println(ser.result());
  }

  @Test public void test1() {
    var result = tyck("""
      open data Nat | O | S Nat
      open data Vec (n : Nat) Type
      | O, A   => vnil
      | S n, A => vcons A (Vec n A)

      def plus (a b : Nat) : Nat elim a
      | O => b
      | S n => S (plus n b)
      """).filter(x -> x instanceof FnDef || x instanceof DataDef);

    var out = new ModuleSerializer(new StringBuilder(), 1, new NameGenerator())
      .serialize(result)
      .result();

    var code = STR."""
    package AYA;

    \{AyaSerializer.IMPORT_BLOCK}

    @SuppressWarnings({"NullableProblems", "SwitchStatementWithTooFewBranches", "ConstantValue"})
    public interface baka {
    \{out}
    }
    """;

    try {
      var clazz = Compiler.java().from("AYA.baka", code).compile().load().get();
      var loader = clazz.getClassLoader();

      var fieldO = loader.loadClass("AYA.baka$Nat$O").getField("INSTANCE");
      var fieldS = loader.loadClass("AYA.baka$Nat$S").getField("INSTANCE");
      var fieldPlus = loader.loadClass("AYA.baka$plus").getField("INSTANCE");
      fieldO.setAccessible(true);
      fieldS.setAccessible(true);
      fieldPlus.setAccessible(true);
      var O = (JitCon) fieldO.get(null);
      var S = (JitCon) fieldS.get(null);
      var plus = (JitFn) fieldPlus.get(null);
      var zero = new JitConCall(O, 0, ImmutableSeq.empty(), ImmutableSeq.empty());
      var one = new JitConCall(S, 0, ImmutableSeq.empty(), ImmutableSeq.of(zero));
      var two = new JitConCall(S, 0, ImmutableSeq.empty(), ImmutableSeq.of(one));
      var three = new JitConCall(S, 0, ImmutableSeq.empty(), ImmutableSeq.of(two));

      var mResult = plus.invoke(zero, two, three);
      System.out.println(mResult.debuggerOnlyToDoc().debugRender());
    } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException | Compiler.CompileException e) {
      throw new RuntimeException(e);
    }

    // var vec = (DataDef) result.findFirst(def -> def.ref().name().equals("Vec")).get();
    // var out = new DataSerializer(new StringBuilder(), 0, new NameGenerator(), _ -> {}).serialize(vec).result();
    // System.out.println("Vec.java");
    // System.out.println(out);
    //
    // var vnil = (ConDef) result.findFirst(def -> def.ref().name().equals("[]")).get();
    // out = new ConSerializer(new StringBuilder(), 0, new NameGenerator()).serialize(vnil).result();
    // System.out.println("vnil.java");
    // System.out.println(out);
    //
    // var plus = (FnDef) result.findFirst(def -> def.ref().name().equals("plus")).get();
    // out = new FnSerializer(new StringBuilder(), 0, new NameGenerator()).serialize(plus).result();
    // System.out.println("plus.java");
    // System.out.println(out);
  }

  @Test public void serLam() {
    // \ t. (\0. 0 t)
    var lam = new LamTerm(new Closure.Jit(t -> new LamTerm(new Closure.Idx(new AppTerm(new LocalTerm(0), t)))));
    var out = new TermSerializer(new NameGenerator(), ImmutableSeq.empty())
      .serialize(lam)
      .result();

    System.out.println(out);
  }

  private static final @NotNull Path FILE = Path.of("/home/senpai/1919810.aya");
  public static final ThrowingReporter REPORTER = new ThrowingReporter(AyaPrettierOptions.pretty());
  public static @NotNull ImmutableSeq<Def> tyck(@Language("Aya") @NotNull String code) {
    var moduleLoader = new DumbModuleLoader(new EmptyContext(REPORTER, FILE));
    var callback = new ModuleCallback<RuntimeException>() {
      ImmutableSeq<Def> ok;
      @Override public void onModuleTycked(@NotNull ResolveInfo x, @NotNull ImmutableSeq<Def> defs) { ok = defs; }
    };
    moduleLoader.tyckModule(moduleLoader.resolve(new AyaParserImpl(REPORTER).program(
      new SourceFile("<baka>", FILE, code))), callback);
    return callback.ok;
  }
}
