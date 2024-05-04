// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.ref;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record MetaVar(
  @Override @NotNull String name
) implements AnyVar {
  @Override public boolean equals(@Nullable Object o) {return this == o;}
  @Override public int hashCode() {return System.identityHashCode(this);}
}
