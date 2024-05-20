// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compile;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.ConDef;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.core.term.ErrorTerm;
import org.aya.syntax.ref.DefVar;
import org.aya.syntax.ref.LocalVar;
import org.aya.syntax.ref.ModulePath;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CompileTest {
  @Test
  public void test0() {
    var o = Nat.Nat$.O.INSTANCE;
    assertNotNull(o.dataType.constructors()[0]);
    assertNotNull(o.dataType.constructors()[1]);

    DefVar<ConDef, TeleDecl.DataCon> S = DefVar.empty("S");
    S.module = ModulePath.of("Data", "Nat", "Nat");

    var cls0 = ImmutableSeq.of(
      new Pat.Bind(new LocalVar("A"), ErrorTerm.DUMMY),
      new Pat.Con(S, ImmutableSeq.of(new Pat.Bind(new LocalVar("n"), ErrorTerm.DUMMY)), null, null)
    );


  }
}
