// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.repr;

import org.jetbrains.annotations.NotNull;

/**
 * @author kiva
 */
sealed public interface ParamShape {
  enum Any implements ParamShape {
    INSTANCE;
  }

  record Impl(
    @NotNull CodeShape.LocalId name,
    @NotNull TermShape type
  ) implements ParamShape, CodeShape.Moment {
  }

  static @NotNull ParamShape ty(@NotNull TermShape type) {
    return named(CodeShape.LocalId.IGNORED, type);
  }

  static @NotNull ParamShape named(@NotNull CodeShape.LocalId name, @NotNull TermShape type) {
    return new Impl(name, type);
  }

  static @NotNull ParamShape implicit(@NotNull TermShape type) {
    return new Impl(CodeShape.LocalId.IGNORED, type);
  }

  static @NotNull ParamShape anyLicit(@NotNull CodeShape.LocalId name, @NotNull TermShape type) {
    return new Impl(name, type);
  }

  static @NotNull ParamShape anyLicit(@NotNull TermShape type) {
    return anyLicit(CodeShape.LocalId.IGNORED, type);
  }

  static @NotNull ParamShape anyEx() {
    return ty(TermShape.Any.INSTANCE);
  }

  static @NotNull ParamShape anyIm() {
    return implicit(TermShape.Any.INSTANCE);
  }

  // anyLicit(TermShape.Any) would be equivalent to ParamShape.Any
}
