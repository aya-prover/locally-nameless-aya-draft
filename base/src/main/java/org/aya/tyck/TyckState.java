// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableMap;
import org.aya.generic.AyaDocile;
import org.aya.normalize.PrimFactory;
import org.aya.prettier.FindUsage;
import org.aya.pretty.doc.Doc;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.LocalCtx;
import org.aya.syntax.ref.MetaVar;
import org.aya.tyck.unify.Unifier;
import org.aya.util.Ordering;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.aya.util.prettier.PrettierOptions;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

public record TyckState(
  @NotNull MutableList<Eqn> eqns,
  @NotNull MutableList<WithPos<MetaVar>> activeMetas,
  @NotNull MutableMap<MetaVar, Term> solutions,
  @NotNull PrimFactory factory
) {
  public TyckState(@NotNull PrimFactory factory) {
    this(MutableList.create(), MutableList.create(), MutableMap.create(), factory);
  }
  public void solve(MetaVar meta, Term candidate) {
    solutions.put(meta, candidate);
  }

  /**
   * @param trying whether to solve in a yasashi manner.
   */
  public void solveEqn(
    @NotNull Reporter reporter,
    @NotNull Eqn eqn, boolean trying
  ) {
    new Unifier(this, eqn.localCtx, reporter, eqn.pos, eqn.cmp, trying).checkEqn(eqn);
  }

  /** @return true if <code>this.eqns</code> and <code>this.activeMetas</code> are mutated. */
  public boolean simplify(@NotNull Reporter reporter) {
    var removingMetas = MutableList.<WithPos<MetaVar>>create();
    for (var activeMeta : activeMetas) {
      var v = activeMeta.data();
      if (solutions.containsKey(v)) {
        eqns.retainIf(eqn -> {
          if (FindUsage.Meta.applyAsInt(eqn.lhs, v) + FindUsage.Meta.applyAsInt(eqn.rhs, v) > 0) {
            solveEqn(reporter, eqn, true);
            return false;
          } else return true;
        });
        removingMetas.append(activeMeta);
      }
    }
    activeMetas.removeIf(removingMetas::contains);
    return removingMetas.isNotEmpty();
  }

  public void addEqn(Eqn eqn) {
    // TODO: maintain the invariants
    eqns.append(eqn);
  }

  public record Eqn(
    @NotNull Term lhs, @NotNull Term rhs,
    @NotNull Ordering cmp, @NotNull SourcePos pos,
    @NotNull LocalCtx localCtx
  ) implements AyaDocile {
    public @NotNull Doc toDoc(@NotNull PrettierOptions options) {
      return Doc.stickySep(lhs.toDoc(options), Doc.symbol(cmp.symbol), rhs.toDoc(options));
    }
  }
}
