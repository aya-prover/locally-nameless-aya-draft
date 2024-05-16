// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.pat;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.core.def.ConDef;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.SigmaTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.TupTerm;
import org.aya.syntax.core.term.call.DataCall;
import org.aya.tyck.TyckState;
import org.aya.tyck.error.ClausesProblem;
import org.aya.tyck.tycker.Problematic;
import org.aya.tyck.tycker.Stateful;
import org.aya.util.error.SourcePos;
import org.aya.util.reporter.Reporter;
import org.aya.util.tyck.pat.ClassifierUtil;
import org.aya.util.tyck.pat.Indexed;
import org.aya.util.tyck.pat.PatClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record PatClassifier<T extends Stateful & Problematic>(
  @NotNull T delegate, @NotNull SourcePos pos
) implements ClassifierUtil<ImmutableSeq<Term>, Term, Param, Pat>, Stateful, Problematic {
  @Override public Param subst(ImmutableSeq<Term> subst, Param param) {
    return param.instTele(subst.view());
  }
  @Override public @NotNull TyckState state() { return delegate.state(); }
  @Override public @NotNull Reporter reporter() { return delegate.reporter(); }
  @Override public Pat normalize(Pat pat) { return pat.inline((_, _) -> { }); }
  @Override public ImmutableSeq<Term> add(ImmutableSeq<Term> subst, Term term) {
    return subst.appended(term);
  }
  @Override public @NotNull ImmutableSeq<PatClass<Term>> classify1(
    @NotNull ImmutableSeq<Term> subst, @NotNull Param param,
    @NotNull ImmutableSeq<Indexed<Pat>> clauses,
    int fuel
  ) {
    var whnfTy = whnf(param.type());
    switch (whnfTy) {
      // Note that we cannot have ill-typed patterns such as constructor patterns under sigma,
      // since patterns here are already well-typed
      case SigmaTerm(var params) -> {
        // The type is sigma type, and do we have any non-catchall patterns?
        // In case we do,
        if (clauses.anyMatch(i -> i.pat() instanceof Pat.Tuple)) {
          var namedParams = params.mapIndexed((i, p) ->
            new Param(String.valueOf(i), p, true));
          // ^ the licit shall not matter
          var matches = clauses.mapIndexedNotNull((i, subPat) ->
            switch (subPat.pat()) {
              case Pat.Tuple tuple -> new Indexed<>(tuple.elements().view(), i);
              case Pat.Bind _ -> new Indexed<>(namedParams.view().map(Param::toFreshPat), i);
              default -> null;
            });
          var classes = classifyN(subst, namedParams.view(), matches, fuel);
          return classes.map(args -> new PatClass<>(new TupTerm(args.term()), args.cls()));
        }
      }
      default -> { }
    }
    return ImmutableSeq.of(new PatClass<>(param.type(), Indexed.indices(clauses)));
  }

  private @Nullable SeqView<Param>
  conTele(@NotNull ImmutableSeq<? extends Indexed<?>> clauses, DataCall dataCall, ConDef con) {
    var conTele = con.selfTele.view();
    // Check if this constructor is available by doing the obvious thing
    var matchy = PatternTycker.isConAvailable(dataCall, con, state());
    // If not, check the reason why: it may fail negatively or positively
    if (matchy.isErr()) {
      // Index unification fails negatively
      if (matchy.getErr()) {
        // If clauses is empty, we continue splitting to see
        // if we can ensure that the other cases are impossible, it would be fine.
        if (clauses.isNotEmpty() &&
          // If clauses has catch-all pattern(s), it would also be fine.
          clauses.noneMatch(seq -> seq.pat() instanceof Pat.Bind)
        ) {
          fail(new ClausesProblem.UnsureCase(pos, con, dataCall));
          return null;
        }
      } else return null;
      // ^ If fails positively, this would be an impossible case
    } else conTele = conTele.map(param -> param.instTele(matchy.get().view()));
    // Java wants a final local variable, let's alias it
    return conTele;
  }
}
