// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.pat;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.Term;
import org.aya.tyck.TyckState;
import org.aya.tyck.tycker.Stateful;
import org.aya.util.Arg;
import org.aya.util.tyck.pat.ClassifierUtil;
import org.aya.util.tyck.pat.Indexed;
import org.aya.util.tyck.pat.PatClass;
import org.jetbrains.annotations.NotNull;

public record PatClassifier(@NotNull Stateful delegate) implements
  ClassifierUtil<ImmutableSeq<Term>, Term, Param, Pat>, Stateful {
  @Override public Param subst(ImmutableSeq<Term> terms, Param param) {
    return param.instTele(terms.view());
  }
  @Override public @NotNull TyckState state() { return delegate.state(); }
  @Override public Pat normalize(Pat pat) { return pat.inline((_, _) -> { }); }
  @Override public ImmutableSeq<Term> add(ImmutableSeq<Term> terms, Term term) {
    return terms.appended(term);
  }
  @Override public @NotNull ImmutableSeq<PatClass<Arg<Term>>> classify1(
    @NotNull ImmutableSeq<Term> terms, @NotNull Param param,
    @NotNull ImmutableSeq<Indexed<Pat>> clauses,
    int fuel
  ) {
    var whnfTy = whnf(param.type());
    final var explicit = param.explicit();
    switch (whnfTy) {
      default -> { }
    }
    return ImmutableSeq.of(new PatClass<>(param.toArg(), Indexed.indices(clauses)));
  }
}
