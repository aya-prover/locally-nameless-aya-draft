// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya;

import kala.collection.mutable.MutableMap;
import org.aya.prettier.AyaPrettierOptions;
import org.aya.syntax.ref.LocalCtx;
import org.aya.util.reporter.Reporter;
import org.aya.util.reporter.ThrowingReporter;
import org.jetbrains.annotations.NotNull;

public interface TestUtil {
  @NotNull
  Reporter THROWING = new ThrowingReporter(AyaPrettierOptions.debug());

  static @NotNull LocalCtx makeLocalCtx() {
    return new LocalCtx(MutableMap.create(), null);
  }
}
