// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.pat;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableLinkedSet;
import kala.collection.mutable.MutableSet;
import kala.control.Option;
import kala.tuple.primitive.IntObjTuple2;
import org.aya.normalize.PatMatcher;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.core.pat.PatToTerm;
import org.aya.syntax.core.term.FreeTerm;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.LocalCtx;
import org.aya.tyck.ExprTycker;
import org.aya.tyck.error.ClausesProblem;
import org.aya.tyck.error.UnifyInfo;
import org.aya.util.error.SourcePos;
import org.aya.util.tyck.pat.PatClass;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

/**
 * YouTrack checks confluence.
 *
 * @author ice1000
 * @see PatClassifier#classify
 */
public record YouTrack(
  @NotNull ImmutableSeq<Param> telescope,
  @NotNull ExprTycker tycker, @NotNull SourcePos pos
) {
  private void unifyClauses(
    Term result,
    PatMatcher prebuiltMatcher,
    IntObjTuple2<Term.Matching> lhsInfo,
    IntObjTuple2<Term.Matching> rhsInfo,
    MutableSet<ClausesProblem.Domination> doms
  ) {
    var ctx = tycker.localCtx().derive();
    var args = new PatToTerm.Binary(ctx).list(
      lhsInfo.component2().patterns(), rhsInfo.component2().patterns());
    domination(ctx, args, lhsInfo.component1(), rhsInfo.component1(), rhsInfo.component2(), doms);
    domination(ctx, args, rhsInfo.component1(), lhsInfo.component1(), lhsInfo.component2(), doms);
    var lhsTerm = prebuiltMatcher.apply(lhsInfo.component2(), args).get();
    var rhsTerm = prebuiltMatcher.apply(rhsInfo.component2(), args).get();
    // // TODO: Currently all holes at this point are in an ErrorTerm
    // if (lhsTerm instanceof ErrorTerm error && error.description() instanceof MetaCall hole) {
    //   hole.ref().conditions.append(Tuple.of(lhsSubst, rhsTerm));
    // }
    // if (rhsTerm instanceof ErrorTerm error && error.description() instanceof MetaCall hole) {
    //   hole.ref().conditions.append(Tuple.of(rhsSubst, lhsTerm));
    // }
    result = result.instantiateTele(args.view());
    tycker.unifyTermReported(lhsTerm, rhsTerm, result, pos, comparison ->
      new ClausesProblem.Confluence(pos, rhsInfo.component1() + 1, lhsInfo.component1() + 1,
        comparison, new UnifyInfo(tycker.state), rhsInfo.component2().sourcePos(), lhsInfo.component2().sourcePos()));
  }

  private void domination(
    LocalCtx ctx, ImmutableSeq<Term> subst,
    int lhsIx, int rhsIx, Term.Matching matching,
    MutableSet<ClausesProblem.Domination> doms
  ) {
    if (subst.allMatch(dom -> dom instanceof FreeTerm(var ref) && ctx.contains(ref)))
      doms.add(new ClausesProblem.Domination(lhsIx + 1, rhsIx + 1, matching.sourcePos()));
  }

  public void check(
    @NotNull ClauseTycker.TyckResult clauses, @NotNull Term type,
    @NotNull ImmutableSeq<PatClass<ImmutableSeq<Term>>> mct
  ) {
    var prebuildMatcher = new PatMatcher(false, UnaryOperator.identity());
    var doms = MutableLinkedSet.<ClausesProblem.Domination>create();
    mct.forEach(results -> {
      var contents = results.cls()
        .flatMapToObj(i -> Option.ofNullable(Pat.Preclause.lift(clauses.clauses().get(i)))
          .map(matching -> IntObjTuple2.of(i, matching)));
      for (int i = 1, size = contents.size(); i < size; i++)
        unifyClauses(type, prebuildMatcher, contents.get(i - 1), contents.get(i), doms);
    });
    doms.forEach(tycker::fail);
  }
}
