// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.pat;

import org.aya.syntax.core.term.FreeTerm;
import org.aya.syntax.core.term.MetaPatTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.TupTerm;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

public final class PatToTerm {
  public static @NotNull Term visit(@NotNull Pat pat) {
    return switch (pat) {
      case Pat.Absurd absurd -> throw new Panic("unreachable");
      case Pat.Bind bind -> new FreeTerm(bind.bind());
      case Pat.Con con -> throw new UnsupportedOperationException("TODO");
      case Pat.Tuple tuple -> new TupTerm(tuple.elements().map(PatToTerm::visit));
      case Pat.Meta meta -> new MetaPatTerm(meta);
    };
  }
}
