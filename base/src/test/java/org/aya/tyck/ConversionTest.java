// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Either;
import org.aya.generic.Modifier;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.FnDef;
import org.aya.syntax.core.term.SortTerm;
import org.aya.syntax.core.term.call.FnCall;
import org.aya.syntax.ref.DefVar;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.aya.syntax.core.term.Param.ofExplicit;

public class ConversionTest {
  @Test
  public void test0() {
    var ref = DefVar.<FnDef, TeleDecl.FnDecl>empty("114514");
    // Not even well-typed lol
    var f = new FnDef(ref,
      ImmutableSeq.of(ofExplicit("a", SortTerm.Type0)),
      SortTerm.Type0,
      EnumSet.noneOf(Modifier.class),
      Either.left(SortTerm.Type0)
    );

    var term0 = new FnCall(ref, 0, ImmutableSeq.of(SortTerm.Type0));
    var term1 = new FnCall(ref, 0, ImmutableSeq.of(SortTerm.Type0));
    assertTrue(new MockConversionChecker().compare(term0, term1, null));
    assertTrue(new MockConversionChecker().compare(term0, term1, SortTerm.Type0));
  }
}
