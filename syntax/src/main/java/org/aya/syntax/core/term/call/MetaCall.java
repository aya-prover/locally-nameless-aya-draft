// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.call;

import kala.collection.immutable.ImmutableSeq;
import kala.function.IndexedFunction;
import org.aya.syntax.core.term.AppTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.MetaVar;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;

/**
 * @param pos error report of this MetaCall will be associated with this position.
 * @param args can grow!! See {@link AppTerm#make()}
 */
public record MetaCall(
  @Override @NotNull MetaVar ref,
  @NotNull SourcePos pos,
  @Override @NotNull ImmutableSeq<Term> args
) implements Callable {
  public @NotNull Term update(@NotNull ImmutableSeq<Term> args) {
    return new MetaCall(ref, pos, args);
  }

  @Override public @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f) {
    return update(Callable.descent(args, f));
  }
}
