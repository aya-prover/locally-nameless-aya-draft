// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya;

import org.aya.normalize.PrimFactory;
import org.aya.prettier.AyaPrettierOptions;
import org.aya.syntax.ref.LocalCtx;
import org.aya.tyck.TyckState;
import org.aya.tyck.unify.TermComparator;
import org.aya.tyck.unify.Unifier;
import org.aya.util.Ordering;
import org.aya.util.error.SourcePos;
import org.aya.util.reporter.IgnoringReporter;
import org.aya.util.reporter.Reporter;
import org.aya.util.reporter.ThrowingReporter;
import org.jetbrains.annotations.NotNull;

public interface TestUtil {
  @NotNull
  Reporter THROWING = new ThrowingReporter(AyaPrettierOptions.debug());

  static @NotNull TermComparator conversion() {
    return new Unifier(emptyState(), makeLocalCtx(),
      IgnoringReporter.INSTANCE, SourcePos.NONE, Ordering.Eq);
  }

  static @NotNull TyckState emptyState() {
    return new TyckState(new PrimFactory());
  }

  static @NotNull LocalCtx makeLocalCtx() {
    return new LocalCtx();
  }
}
