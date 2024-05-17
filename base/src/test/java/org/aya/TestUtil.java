// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya;

import kala.collection.immutable.ImmutableSeq;
import org.aya.normalize.Normalizer;
import org.aya.normalize.PrimFactory;
import org.aya.prettier.AyaPrettierOptions;
import org.aya.syntax.core.def.FnDef;
import org.aya.syntax.core.repr.AyaShape;
import org.aya.syntax.core.term.call.FnCall;
import org.aya.syntax.ref.LocalCtx;
import org.aya.tyck.TyckState;
import org.aya.tyck.ctx.LocalLet;
import org.aya.util.reporter.Reporter;
import org.aya.util.reporter.ThrowingReporter;
import org.jetbrains.annotations.NotNull;

public interface TestUtil {
  @NotNull Reporter THROWING = new ThrowingReporter(AyaPrettierOptions.debug());

  static @NotNull TyckState emptyState() {
    return new TyckState(new AyaShape.Factory(), new PrimFactory());
  }
  static @NotNull Normalizer sillyNormalizer() {
    return new Normalizer(emptyState());
  }
  static @NotNull FnCall emptyCall(FnDef fn) {
    return new FnCall(fn.ref, 0, ImmutableSeq.empty());
  }
  static @NotNull LocalCtx makeLocalCtx() {
    return new LocalCtx();
  }
  static @NotNull LocalLet makeLocalSubst() {
    return new LocalLet();
  }
}
