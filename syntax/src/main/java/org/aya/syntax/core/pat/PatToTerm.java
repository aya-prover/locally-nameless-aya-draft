// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.pat;

import org.aya.syntax.core.term.FreeTerm;
import org.aya.syntax.core.term.MetaPatTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.TupTerm;
import org.aya.syntax.core.term.call.ConCall;
import org.aya.syntax.core.term.repr.IntegerTerm;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

public final class PatToTerm {
  public static @NotNull Term visit(@NotNull Pat pat) {
    return switch (pat) {
      case Pat.Absurd _ -> Panic.unreachable();
      case Pat.Bind bind -> new FreeTerm(bind.bind());
      case Pat.Con(var conRef, var args, var recog, var data) ->
        new ConCall(data.ref(), conRef, data.args(), 0, args.map(PatToTerm::visit));
      case Pat.Tuple tuple -> new TupTerm(tuple.elements().map(PatToTerm::visit));
      case Pat.Meta meta -> new MetaPatTerm(meta);
      case Pat.ShapedInt(var i, var recog, var data) -> new IntegerTerm(i, recog, data);
    };
  }
}
