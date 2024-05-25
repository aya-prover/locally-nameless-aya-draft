// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

import org.aya.syntax.core.repr.AyaShape;
import org.aya.syntax.core.repr.CodeShape;
import org.aya.util.binop.Assoc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CompiledAya {
  @NotNull String[] fileModule();
  @NotNull String[] module();

  @NotNull String name();
  @NotNull Assoc assoc();

  @Nullable AyaShape shape();
  @NotNull CodeShape.GlobalId[] recognition();
}
