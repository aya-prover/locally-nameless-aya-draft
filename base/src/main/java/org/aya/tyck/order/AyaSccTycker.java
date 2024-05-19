// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.order;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableMap;
import kala.collection.mutable.MutableSet;
import org.aya.generic.InterruptException;
import org.aya.generic.TyckOrder;
import org.aya.generic.TyckUnit;
import org.aya.resolve.ResolveInfo;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.Def;
import org.aya.tyck.StmtTycker;
import org.aya.tyck.tycker.Problematic;
import org.aya.util.error.SourceNode;
import org.aya.util.reporter.CollectingReporter;
import org.aya.util.reporter.CountingReporter;
import org.aya.util.reporter.Reporter;
import org.aya.util.terck.MutableGraph;
import org.aya.util.tyck.SCCTycker;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

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
    try {
      if (scc.isEmpty()) return ImmutableSeq.empty();
      if (scc.sizeEquals(1)) checkUnit(scc.getFirst());
      else checkMutual(scc);
      return ImmutableSeq.empty();
    } catch (SCCTyckingFailed failed) {
      reporter.clear();
      return failed.what;
    }
  }

  private void checkMutual(@NotNull ImmutableSeq<TyckOrder> scc) {
    var unit = scc.view().map(TyckOrder::unit)
      .distinct()
      .sorted(Comparator.comparing(SourceNode::sourcePos))
      .toImmutableSeq();
    // the flattened dependency graph (FDG) lose information about header order, in other words,
    // FDG treats all order as body order, so it allows all kinds of mutual recursion to be generated.
    // To detect circular dependency in signatures which we forbid, we have to apply the old way,
    // that is, what we did before https://github.com/aya-prover/aya-dev/pull/326
    if (unit.sizeEquals(1)) checkUnit(new TyckOrder.Body(unit.getFirst()));
    else {
      unit.forEach(u -> check(new TyckOrder.Head(u)));
      unit.forEach(u -> check(new TyckOrder.Body(u)));
      // terck(scc.view());
    }
  }

  private void check(@NotNull TyckOrder tyckOrder) {
    switch (tyckOrder) {
      case TyckOrder.Head head -> checkHeader(tyckOrder, head.unit());
      case TyckOrder.Body body -> checkBody(tyckOrder, body.unit());
    }
  }

  private void checkUnit(@NotNull TyckOrder order) {
    // if (order instanceof TyckOrder.Body(TeleDecl.FnDecl fn) && fn.body instanceof TeleDecl.ExprBody(var expr)) {
    //   checkSimpleFn(order, fn, expr);
    // } else {
    check(order);
    // if (order instanceof TyckOrder.Body body)
    //   terck(SeqView.of(body));
    // }
  }

  private void checkHeader(@NotNull TyckOrder order, @NotNull TyckUnit stmt) {
    if (stmt instanceof TeleDecl<?> decl) tycker.checkHeader(decl);
    if (reporter.anyError()) throw new SCCTyckingFailed(ImmutableSeq.of(order));
  }

  private void checkBody(@NotNull TyckOrder order, @NotNull TyckUnit stmt) {
    if (stmt instanceof Decl decl) tycker.check(decl);
    if (reporter.anyError()) throw new SCCTyckingFailed(ImmutableSeq.of(order));
  }

  private <T> boolean
  hasSuc(@NotNull MutableGraph<T> G, @NotNull MutableSet<T> book, @NotNull T vertex, @NotNull T suc) {
    if (book.contains(vertex)) return false;
    book.add(vertex);
    for (var test : G.suc(vertex)) {
      if (test.equals(suc)) return true;
      if (hasSuc(G, book, test, suc)) return true;
    }
    return false;
  }

  private <T> boolean selfReferencing(@NotNull MutableGraph<T> graph, @NotNull T unit) {
    return hasSuc(graph, MutableSet.create(), unit, unit);
  }

  public static class SCCTyckingFailed extends InterruptException {
    public final @NotNull ImmutableSeq<TyckOrder> what;
    public SCCTyckingFailed(@NotNull ImmutableSeq<TyckOrder> what) { this.what = what; }
    @Override public InterruptStage stage() { return InterruptStage.Tycking; }
  }
}
