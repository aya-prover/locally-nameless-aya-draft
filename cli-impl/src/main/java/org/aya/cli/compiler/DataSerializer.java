// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.cli.compiler;

import org.aya.syntax.core.def.DataDef;
import org.jetbrains.annotations.NotNull;

public final class DataSerializer implements AyaSerializer<DataDef>  {
  private final @NotNull StringBuilder source = new StringBuilder();

  @Override public String serialize(DataDef unit) {
    assert source.isEmpty();

    return "";
  }
}
