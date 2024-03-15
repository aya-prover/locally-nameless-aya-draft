// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.resolve.context;

import org.jetbrains.annotations.NotNull;

public record WithCtx<T>(@NotNull Context ctx, @NotNull T data) {
  public static <T> @NotNull WithCtx<T> of(@NotNull Context ctx, @NotNull T data) {
    return new WithCtx<>(ctx, data);
  }
}
