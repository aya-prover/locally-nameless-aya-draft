// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.repr;

import kala.collection.immutable.ImmutableMap;
import org.aya.syntax.core.def.ConDef;
import org.aya.syntax.core.def.ConDefLike;
import org.aya.syntax.ref.DefVar;
import org.jetbrains.annotations.NotNull;

public record ShapeRecognition(
  @NotNull AyaShape shape,
  @NotNull ImmutableMap<CodeShape.GlobalId, DefVar<?, ?>> captures
) {
  @SuppressWarnings("unchecked") public @NotNull ConDefLike
  getCon(@NotNull CodeShape.GlobalId id) {
    var ref = this.captures().get(id);
    assert ref.core instanceof ConDef : "Sanity check";
    return new ConDef.Delegate((DefVar<ConDef, ?>) ref);
  }
}
