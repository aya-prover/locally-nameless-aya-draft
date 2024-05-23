// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.repr;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Either;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * @author kiva
 */
public sealed interface CodeShape {
  /** A capture group, see {@link ConShape} and {@link ShapeMatcher#captures()} */
  sealed interface Moment permits ConShape, DataShape, FnShape, TermShape.ShapeCall {
    @NotNull MomentId name();
  }

  /** Typed capture name, rather than plain strings */
  sealed interface MomentId { }

  enum GlobalId implements MomentId, Serializable {
    ZERO, SUC, NIL, CONS,
  }

  record LocalId(@NotNull String name) implements MomentId {
    public static final @NotNull LocalId DATA = new LocalId("Data");
    public static final @NotNull LocalId FUNC = new LocalId("Func");
    public static final @NotNull LocalId TYPE = new LocalId("Type0");
  }

  record FnShape(
    @NotNull MomentId name,
    @NotNull ImmutableSeq<TermShape> tele,
    @NotNull TermShape result,
    @NotNull Either<TermShape, ImmutableSeq<ClauseShape>> body
  ) implements CodeShape, Moment { }

  record ClauseShape(@NotNull ImmutableSeq<PatShape> pats, @NotNull TermShape body) implements CodeShape {
  }

  record DataShape(
    @NotNull MomentId name,
    @NotNull ImmutableSeq<TermShape> tele,
    @NotNull ImmutableSeq<ConShape> cons
  ) implements CodeShape, Moment { }

  record ConShape(
    @NotNull GlobalId name,
    @NotNull ImmutableSeq<TermShape> tele
  ) implements CodeShape, Moment { }
}
