// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.normalize;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import kala.control.Result;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.core.pat.PatToTerm;
import org.aya.syntax.core.term.MetaPatTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.TupTerm;
import org.aya.syntax.core.term.call.ConCall;
import org.aya.syntax.core.term.call.ConCallLike;
import org.aya.syntax.core.term.repr.IntegerTerm;
import org.aya.tyck.pat.BindEater;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

/**
 * @param inferMeta whether infer the PatMetaTerm
 */
public record PatMatcher(boolean inferMeta, @NotNull UnaryOperator<Term> pre) {
  private static class Failure extends Throwable {
    public final boolean reason;
    private Failure(boolean reason) {
      super(null, null, false, false);
      this.reason = reason;
    }
  }

  /**
   * Match {@param term} against to {@param pat}
   *
   * @return a substitution of corresponding bindings of {@param pat} if success
   */
  private @NotNull ImmutableSeq<Term> match(@NotNull Pat pat, @NotNull Term term) throws Failure {
    return switch (pat) {
      // We stuck on absurd patterns, as if this is reached, the term must have an empty type,
      // which we should be expecting to refute, not to compute on it.
      case Pat.Absurd _ -> throw new Failure(true);
      case Pat.Bind _, Pat.JitBind _ -> ImmutableSeq.of(term);
      case Pat.Con con -> switch (pre.apply(term)) {
        case ConCallLike kon -> {
          if (con.ref() != kon.ref()) throw new Failure(false);
          yield matchMany(con.args(), kon.conArgs());
          // ^ arguments for data should not be matched
        }
        case MetaPatTerm metaPatTerm -> solve(pat, metaPatTerm);
        default -> throw new Failure(true);
      };
      case Pat.Tuple tuple -> switch (pre.apply(term)) {
        case TupTerm tup -> matchMany(tuple.elements(), tup.items());
        case MetaPatTerm metaPatTerm -> solve(pat, metaPatTerm);
        default -> throw new Failure(true);
      };
      // You can't match with a tycking pattern!
      case Pat.Meta meta -> throw new Panic("Illegal pattern: Pat.Meta");
      case Pat.ShapedInt lit -> switch (pre.apply(term)) {
         case IntegerTerm litTerm -> {
           if (!lit.compareUntyped(litTerm)) throw new Failure(false);
           yield ImmutableSeq.empty();
         }
         case ConCall con -> match(lit.constructorForm(), con);
         // we only need to handle matching both literals, otherwise we just rematch it
         // with constructor form to reuse the code as much as possible (like solving MetaPats).
         case Term t -> match(lit.constructorForm(), t);
       };
    };
  }

  /**
   * @return a substitution of corresponding bindings of {@param pats} if success,
   * true if stuck (like trying to split a non-constructor),
   * false if it does not match (like trying to match zero with suc).
   * @apiNote The binding order is the same as {@link Pat#consumeBindings(java.util.function.BiConsumer)}
   */
  public @NotNull Result<ImmutableSeq<Term>, Boolean> apply(
    @NotNull ImmutableSeq<Pat> pats,
    @NotNull ImmutableSeq<Term> terms
  ) {
    try {
      return Result.ok(matchMany(pats, terms));
    } catch (Failure e) {
      return Result.err(e.reason);
    }
  }

  public @NotNull Result<Term, Boolean> apply(
    @NotNull Term.Matching matching,
    @NotNull ImmutableSeq<Term> terms
  ) {
    try {
      return Result.ok(matching.body().instantiateTele(matchMany(matching.patterns(), terms).view()));
    } catch (Failure e) {
      return Result.err(e.reason);
    }
  }

  /**
   * @see #match(Pat, Term)
   */
  private @NotNull ImmutableSeq<Term> matchMany(
    @NotNull ImmutableSeq<Pat> pats,
    @NotNull ImmutableSeq<Term> terms
  ) throws Failure {
    assert pats.sizeEquals(terms) : "List size mismatch 😱";

    var subst = MutableList.<Term>create();
    pats.forEachWithChecked(terms, (pat, term) -> subst.appendAll(match(pat, term)));
    return subst.toImmutableSeq();
  }

  private @NotNull ImmutableSeq<Term> solve(@NotNull Pat pat, @NotNull MetaPatTerm term) throws Failure {
    var meta = term.meta();
    return meta.mapChecked(p -> match(pat, PatToTerm.visit(p)), () -> {
      if (!inferMeta) throw new Failure(true);

      // No solution, set the current pattern as solution,
      // also replace the bindings in pat as sub-meta,
      // so that we can solve this meta more.

      var eater = new BindEater(MutableList.create());
      var boroboroPat = eater.apply(pat);   // It looks boroboro, there are holes on it.
      meta.solution().set(boroboroPat);

      return eater.mouth().toImmutableSeq();
    });
  }
}
