// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.unify;

import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableList;
import org.aya.prettier.FindUsage;
import org.aya.syntax.core.term.Formation;
import org.aya.syntax.core.term.FreeTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.MetaCall;
import org.aya.syntax.ref.LocalCtx;
import org.aya.syntax.ref.LocalVar;
import org.aya.syntax.ref.MetaVar;
import org.aya.tyck.TyckState;
import org.aya.tyck.error.HoleProblem;
import org.aya.util.Ordering;
import org.aya.util.error.SourcePos;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Unifier extends TermComparator {
  public Unifier(@NotNull TyckState state, @NotNull LocalCtx ctx, @NotNull Reporter reporter, @NotNull SourcePos pos, @NotNull Ordering cmp) {
    super(state, ctx, reporter, pos, cmp);
  }

  @Override protected @Nullable Term doSolveMeta(@NotNull MetaCall meta, @NotNull Term rhs, @Nullable Term type) {
    // Assumption: rhs is in whnf
    var spine = meta.args();
    var inverted = MutableArrayList.<LocalVar>create(spine.size());
    var overlap = MutableList.<LocalVar>create();
    var returnType = type;
    var needUnify = true;
    // TODO: the code below is incomplete
    switch (meta.ref().req()) {
      case MetaVar.Misc misc -> {
        switch (misc) {
          case Whatever -> needUnify = false;
          case IsType -> {
            switch (rhs) {
              case Formation _ -> {}
              case MetaCall rMeta -> {
                // TODO: rMeta.req().isType(synthesizer)
              }
              default -> {
                // TODO: synthesize and set the returnType to the result of synthesis
              }
            }
            needUnify = false;
          }
        }
      }
      case MetaVar.OfType(var target) -> {
        if (type != null && !compare(type, target, null)) {
          reportIllTyped(meta, rhs);
          return null;
        }
        // TODO: freezeHoles
        returnType = target;
      }
    }
    // TODO: type check the rhs according to the meta's info, need double checker
    for (var arg : spine) {
      // TODO: apply uneta
      if (whnf(arg) instanceof FreeTerm(var var)) {
        inverted.append(var);
        if (inverted.contains(var)) overlap.append(var);
      } else {
        reporter.report(new HoleProblem.BadSpineError(meta));
        return null;
      }
    }

    // In this case, the solution may not be unique (see #608),
    // so we may delay its resolution to the end of the tycking when we disallow vague unification.
    if (overlap.anyMatch(var -> FindUsage.Free.applyAsInt(rhs, var) > 0)) {
      // TODO: addEqn
      return returnType;
    }
    // Now we are sure that the variables in overlap are all unused.

    var candidate = inverted.view().foldRight(rhs, (var, wip) ->
      // We know already that overlapping terms are unused yet, so optimize it a little bit
      overlap.contains(var) ? wip : wip.bind(var));

    if (FindUsage.AnyFree.applyAsInt(rhs) > 0) {
      reporter.report(new HoleProblem.BadlyScopedError(meta, rhs, inverted));
      return null;
    }

    if (FindUsage.Meta.applyAsInt(candidate, meta.ref()) > 0) {
      reporter.report(new HoleProblem.RecursionError(meta, candidate));
      return null;
    }
    state.solve(meta.ref(), candidate);

    // TODO: synthesize the type in case it's not provided
    return type;
  }

  private void reportIllTyped(@NotNull MetaCall meta, @NotNull Term rhs) {
    reporter.report(new HoleProblem.IllTypedError(meta, state, rhs));
  }
}
