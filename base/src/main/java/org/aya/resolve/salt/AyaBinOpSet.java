// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.resolve.salt;

import kala.collection.immutable.ImmutableSeq;
import org.aya.resolve.context.Context;
import org.aya.resolve.error.OperatorError;
import org.aya.util.binop.BinOpSet;
import org.aya.util.error.SourcePos;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;

public final class AyaBinOpSet extends BinOpSet {
  public final @NotNull Reporter reporter;
  public AyaBinOpSet(@NotNull Reporter reporter) { this.reporter = reporter; }

  @Override protected void reportSelfBind(@NotNull SourcePos sourcePos) {
    reporter.report(new OperatorError.SelfBind(sourcePos));
    throw new Context.ResolvingInterruptedException();
  }

  @Override protected void reportCyclic(ImmutableSeq<ImmutableSeq<BinOP>> cycles) {
    cycles.forEach(c -> reporter.report(new OperatorError.Circular(c)));
    throw new Context.ResolvingInterruptedException();
  }
}
