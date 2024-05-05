// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.ref;

import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.MetaCall;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * if the <code>args</code> of the {@link MetaCall} is larger than ctxSize,
 * then in case there is {@link OfType}, we will need to type check the argument
 * and check the solution against the iterated <strong>codomain</strong> instead of the type itself.
 *
 * @param ctxSize size of the original context.
 * @see MetaCall
 */
public record MetaVar(
  @Override @NotNull String name,
  @NotNull SourcePos pos,
  int ctxSize, @NotNull Requirement req
) implements AnyVar {
  @Override public boolean equals(@Nullable Object o) {return this == o;}

  @Override public int hashCode() {return System.identityHashCode(this);}

  public sealed interface Requirement {}
  public enum Misc implements Requirement {
    Whatever,
    IsType,
  }
  public record OfType(@NotNull Term type) implements Requirement {}
}
