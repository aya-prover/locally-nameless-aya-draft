// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.terck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.immutable.ImmutableSet;
import kala.collection.mutable.MutableSet;
import kala.value.MutableValue;
import org.aya.normalize.Normalizer;
import org.aya.syntax.core.def.FnDef;
import org.aya.syntax.core.def.TeleDef;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.Callable;
import org.aya.syntax.core.term.call.ConCall;
import org.aya.syntax.core.term.call.ConCallLike;
import org.aya.syntax.core.term.repr.IntegerTerm;
import org.aya.syntax.core.term.xtt.PAppTerm;
import org.aya.syntax.ref.DefVar;
import org.aya.tyck.TyckState;
import org.aya.tyck.tycker.Stateful;
import org.aya.util.terck.CallGraph;
import org.aya.util.terck.CallMatrix;
import org.aya.util.terck.Relation;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Resolve calls and build call graph of recursive functions,
 * after {@link org.aya.tyck.StmtTycker}.
 *
 * @param targets only search calls to those definitions
 * @author kiva
 */
public record CallResolver(
  @Override @NotNull TyckState state,
  @NotNull FnDef caller,
  @NotNull MutableSet<TeleDef> targets,
  @NotNull MutableValue<Term.Matching> currentClause,
  @NotNull CallGraph<Callable, TeleDef, Param> graph
) implements Stateful, Consumer<Term.Matching> {
  public CallResolver {
    assert caller.body.isRight();
  }
  public CallResolver(
    @NotNull TyckState state, @NotNull FnDef fn,
    @NotNull MutableSet<TeleDef> targets,
    @NotNull CallGraph<Callable, TeleDef, Param> graph
  ) {
    this(state, fn, targets, MutableValue.create(), graph);
  }

  private void resolveCall(@NotNull Callable callable) {
    if (!(callable.ref() instanceof DefVar<?, ?> defVar)) return;
    var callee = (TeleDef) defVar.core;
    if (!targets.contains(callee)) return;
    var matrix = new CallMatrix<>(callable, caller, callee, caller.telescope, callee.telescope());
    fillMatrix(callable, callee, matrix);
    graph.put(matrix);
  }

  private void fillMatrix(@NotNull Callable callable, @NotNull TeleDef callee, CallMatrix<?, TeleDef, Param> matrix) {
    var currentPatterns = currentClause.get();
    assert currentPatterns != null;
    currentPatterns.patterns().forEachWith(caller.telescope, (pat, domParam) ->
      callable.args().forEachWith(callee.telescope(), (term, codParam) -> {
        var relation = compare(term, pat);
        matrix.set(domParam, codParam, relation);
      }));
  }

  /** foetus dependencies */
  private @NotNull Relation compare(@NotNull Term term, @NotNull Pat pat) {
    return switch (pat) {
      case Pat.Con con -> {
        if (term instanceof ConCallLike con2) {
          var ref = con2.ref();
          var conArgs = con2.conArgs();

          if (ref != con.ref() || !conArgs.sizeEquals(con.args())) yield Relation.unk();
          var attempt = compareConArgs(conArgs, con);
          // Reduce arguments and compare again. This may cause performance issues (but not observed yet [2022-11-07]),
          // see https://github.com/agda/agda/issues/2403 for more information.
          if (attempt == Relation.unk()) attempt = compareConArgs(conArgs.map(a -> a.descent(this::whnf)), con);

          yield attempt;
        }

        var subCompare = con.args().view().map(sub -> compare(term, sub));
        var attempt = subCompare.anyMatch(r -> r != Relation.unk()) ? Relation.lt() : Relation.unk();
        if (attempt == Relation.unk()) {
          yield switch (whnf(term)) {
            case ConCall con2 -> compare(con2, con);
            // TODO[h]: do we need a RuleReducer.Con case here? @ice1000
            case IntegerTerm lit -> compare(lit, con);
            // This is related to the predicativity issue mentioned in #907
            case PAppTerm papp -> {
              var head = papp.fun();
              while (head instanceof PAppTerm papp2) head = papp2.fun();
              yield compare(head, con);
            }
            default -> attempt;
          };
        }

        yield attempt;
      }
      case Pat.Bind bind -> {
        if (term instanceof FreeTerm(var ref))
          yield ref == bind.bind() ? Relation.eq() : Relation.unk();
        if (headOf(term) instanceof FreeTerm(var ref))
          yield ref == bind.bind() ? Relation.lt() : Relation.unk();
        yield Relation.unk();
      }
      case Pat.ShapedInt intPat -> switch (term) {
        case IntegerTerm intTerm -> {
          // ice: by well-typedness, we don't need to compareShape
          if (intTerm.recognition().shape() != intPat.recognition().shape()) yield Relation.unk();
          yield Relation.fromCompare(Integer.compare(intTerm.repr(), intPat.repr()));
        }
        case ConCall con -> compare(con, intPat.constructorForm());
        default -> compare(term, intPat.constructorForm());
      };
      default -> Relation.unk();
    };
  }

  private Relation compareConArgs(@NotNull ImmutableSeq<Term> conArgs, @NotNull Pat.Con con) {
    var subCompare = conArgs.zip(con.args(), this::compare);
    return subCompare.foldLeft(Relation.eq(), Relation::mul);
  }

  /** @return the head of application or projection */
  private @NotNull Term headOf(@NotNull Term term) {
    return switch (term) {
      case AppTerm app -> headOf(app.fun());
      case PAppTerm papp -> headOf(papp.fun());
      case ProjTerm proj -> headOf(proj.of());
      // case FieldTerm access -> headOf(access.of());
      default -> term;
    };
  }

  public void check() {
    var clauses = caller.body.getRightValue();
    clauses.forEach(this);
  }

  @Override public void accept(@NotNull Term.Matching matching) {
    this.currentClause.set(matching);
    var vars = Pat.collectBindings(matching.patterns().view()).view().map(Pat.CollectBind::var);
    visitTerm(matching.body().instantiateTeleVar(vars));
    this.currentClause.set(null);
  }

  private void visitTerm(@NotNull Term term) {
    // TODO: Improve error reporting to include the original call
    term = new Normalizer(state, ImmutableSet.from(targets.map(TeleDef::ref))).apply(term);
    if (term instanceof Callable call) resolveCall(call);
    term.descent((_, child) -> {
      visitTerm(child);
      return child;
    });
  }
}
