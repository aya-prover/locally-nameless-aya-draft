// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.order;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableMap;
import org.aya.generic.InterruptException;
import org.aya.generic.TyckOrder;
import org.aya.resolve.ResolveInfo;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.Def;
import org.aya.tyck.StmtTycker;
import org.aya.tyck.tycker.Problematic;
import org.aya.util.reporter.CollectingReporter;
import org.aya.util.reporter.CountingReporter;
import org.aya.util.reporter.Reporter;
import org.aya.util.tyck.SCCTycker;
import org.jetbrains.annotations.NotNull;

/**
 * Tyck statements in SCC.
 *
 * @author kiva
 * @see org.aya.tyck.ExprTycker
 */
public record AyaSccTycker(
  @NotNull StmtTycker tycker,
  @NotNull CountingReporter reporter,
  @NotNull ResolveInfo resolveInfo,
  @NotNull MutableList<@NotNull Def> wellTyped,
  @NotNull MutableMap<TeleDecl<?>, CollectingReporter> sampleReporters
) implements SCCTycker<TyckOrder, AyaSccTycker.SCCTyckingFailed>, Problematic {
  public static @NotNull AyaSccTycker create(ResolveInfo resolveInfo, @NotNull Reporter outReporter) {
    var counting = CountingReporter.delegate(outReporter);
    var stmt = new StmtTycker(counting, resolveInfo.shapeFactory(), resolveInfo.primFactory());
    return new AyaSccTycker(stmt, counting, resolveInfo, MutableList.create(), MutableMap.create());
  }

  @Override public @NotNull ImmutableSeq<TyckOrder>
  tyckSCC(@NotNull ImmutableSeq<TyckOrder> scc) throws SCCTyckingFailed {
    return null;
  }

  public static class SCCTyckingFailed extends InterruptException {
    public final @NotNull ImmutableSeq<TyckOrder> what;
    public SCCTyckingFailed(@NotNull ImmutableSeq<TyckOrder> what) { this.what = what; }
    @Override public InterruptStage stage() { return InterruptStage.Tycking; }
  }
}
