// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.generic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thread-unsafe name generator
 */
public class NameGenerator {
  private int id = 0;

  public int nextId() {
    return id++;
  }

  public @NotNull String nextName(@Nullable String typeName) {
    return (typeName == null ? "" : Constants.ANONYMOUS_PREFIX + typeName)
      + Constants.ANONYMOUS_PREFIX + nextId();
  }
}
