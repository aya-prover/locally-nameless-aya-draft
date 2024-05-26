// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

import org.aya.syntax.core.repr.CodeShape;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CompiledAya {
  @NotNull String[] module();
  int fileModuleSize();

  @NotNull String name();
  int assoc();

  int shape();
  @NotNull CodeShape.GlobalId[] recognition();
}
