// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.

import kala.collection.immutable.ImmutableSeq;
import org.aya.compiler.PatternSerializer;
import org.aya.generic.NameGenerator;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.ConDef;
import org.aya.syntax.core.def.DataDef;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.core.term.ErrorTerm;
import org.aya.syntax.core.term.SortTerm;
import org.aya.syntax.ref.DefVar;
import org.aya.syntax.ref.LocalVar;
import org.aya.syntax.ref.ModulePath;
import org.junit.jupiter.api.Test;

public class CompileTest {
  @Test public void test0() {
    DefVar<ConDef, TeleDecl.DataCon> S = DefVar.empty("S");
    S.module = ModulePath.of("Data", "Nat", "Nat");

    var cls0 = ImmutableSeq.<Pat>of(
      new Pat.Bind(new LocalVar("A"), ErrorTerm.DUMMY),
      new Pat.Con(S, ImmutableSeq.of(new Pat.Bind(new LocalVar("n"), ErrorTerm.DUMMY)), null, null)
    );

    var builder = new StringBuilder();
    var ser = new PatternSerializer(builder, 0, new NameGenerator(), "args",
      () -> builder.append("System.out.println(\"Hello, world!\");\n"),
      () -> builder.append("System.out.println(\"Unhello, world!\");\n"));

    ser.serialize(ImmutableSeq.of(new PatternSerializer.Matching(cls0,
      () -> builder.append("System.out.println(\"Good, world!\");\n"))));

    System.out.println(ser.result());
  }

  @Test public void test1() {
    var Vec = DefVar.<DataDef, TeleDecl.DataDecl>empty("Vec");
    Vec.module = ModulePath.of("Data", "Vec");

    var VecDef = new DataDef(Vec, ImmutableSeq.empty(), SortTerm.Type0, ImmutableSeq.empty());
  }
}
