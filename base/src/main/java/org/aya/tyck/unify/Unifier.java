// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.unify;

import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableList;
import org.aya.syntax.core.term.FreeTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.MetaCall;
import org.aya.syntax.ref.LocalCtx;
import org.aya.syntax.ref.LocalVar;
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
    if (overlap.isNotEmpty()) {
      // TODO: find usages of the overlapping variables in the rhs
    }
    // Now we are sure that the variables in overlap are all unused.

    var candidate = inverted.view().foldRight(rhs, (var, wip) -> wip.bind(var));

    // TODO: scope check: if any FreeTerm is in candidate, report error

    // TODO: synthesize the type in case it's not provided
    return type;
  }
}
