package org.aya.syntax.ref;

import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record LocalVar(@NotNull String name, @NotNull SourcePos definition) {
  public LocalVar(@NotNull String name) {
    this(name, SourcePos.NONE);
  }

  public static final @NotNull LocalVar IGNORED = new LocalVar("_", SourcePos.NONE);

  @Override public boolean equals(@Nullable Object o) {
    return this == o;
  }

  @Override public int hashCode() {
    return System.identityHashCode(this);
  }
}
