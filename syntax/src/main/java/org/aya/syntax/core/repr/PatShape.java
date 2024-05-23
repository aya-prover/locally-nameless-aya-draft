// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.repr;

import kala.collection.immutable.ImmutableSeq;
import org.jetbrains.annotations.NotNull;

/**
 * @author kiva
 */
public sealed interface PatShape {
  enum Basic implements PatShape { Any, Bind }
  sealed interface ConLike extends PatShape permits Con, ShapedCon {
    @NotNull ImmutableSeq<PatShape> innerPats();
  }

  record Con(@NotNull ImmutableSeq<PatShape> innerPats) implements ConLike { }

  /**
   * Expecting a certain ctor, {@link ShapeMatcher} will crash
   * if the {@param dataId} is invalid (such as undefined or not a DataShaped thing)
   *
   * @param dataId a reference to a {@link CodeShape.DataShape}d term
   * @param conId  the {@link CodeShape.GlobalId} to the ctor
   */
  record ShapedCon(
    @NotNull CodeShape.MomentId dataId, @NotNull CodeShape.GlobalId conId,
    @NotNull ImmutableSeq<PatShape> innerPats
  ) implements ConLike {
    public static @NotNull PatShape.ShapedCon of(@NotNull CodeShape.MomentId name, @NotNull CodeShape.GlobalId id) {
      return new ShapedCon(name, id, ImmutableSeq.empty());
    }
  }
}
