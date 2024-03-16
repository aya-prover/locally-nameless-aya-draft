// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.util;

import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

@FunctionalInterface
public interface PosedUnaryOperator<T> extends BiFunction<SourcePos, T, T> {
  default @NotNull T apply(@NotNull WithPos<T> a) {
    return apply(a.sourcePos(), a.data());
  }

  default @NotNull T forceApply(@NotNull T a) {
    return apply(SourcePos.NONE, a);
  }
}
