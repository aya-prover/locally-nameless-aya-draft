// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.cli.compiler;

import kala.value.primitive.MutableIntValue;
import org.aya.syntax.core.pat.Pat;
import org.jetbrains.annotations.NotNull;

public final class PatternSerializer implements AyaSerializer<Pat> {
  private final @NotNull StringBuilder builder = new StringBuilder();
  private int bindCount = 0;
  private int indent = 0;

  @Override public @NotNull String serialize(Pat unit) {
    assert builder.isEmpty();

    var maxBindCount = MutableIntValue.create();
    unit.consumeBindings((_, _) -> maxBindCount.increment());

    fillIndent();
    builder.append(STR."Term[] binds = new Term[\{maxBindCount}];\n");
  }

  private void fillIndent() {
    if (indent == 0) return;
    builder.append("  ".repeat(indent));
  }


  private void onSerialize(@NotNull Pat unit) {
    switch (unit) {
      case Pat.Absurd absurd -> {
        builder.append("Panic.unreachable();");
      }
      case Pat.Bind bind -> { }
      case Pat.Con con -> { }
      case Pat.JitBind jitBind -> { }
      case Pat.Meta meta -> { }
      case Pat.ShapedInt shapedInt -> { }
      case Pat.Tuple tuple -> { }
    }
  }
}
