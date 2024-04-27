// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import org.aya.TestUtil;
import org.aya.syntax.core.term.Term;
import org.aya.tyck.unify.TermComparator;
import org.aya.util.Ordering;
import org.aya.util.error.SourcePos;
import org.aya.util.reporter.IgnoringReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MockConversionChecker extends TermComparator {
  public MockConversionChecker() {
    super(
      new TyckState(),
      TestUtil.makeLocalCtx(),
      IgnoringReporter.INSTANCE,
      SourcePos.NONE,
      Ordering.Eq);
  }

  @Override protected @Nullable Term doSolveMeta(@NotNull Term meta, @NotNull Term rhs, @Nullable Term type) {
    return null;
  }
}
