// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.def;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * Top-level definitions.
 *
 * @author ice1000
 */
public sealed abstract class TopLevelDef<Ret extends Term> implements TeleDef permits DataDef, FnDef {
  public final @NotNull ImmutableSeq<Param> telescope;
  public final @NotNull Ret result;

  protected TopLevelDef(
    @NotNull ImmutableSeq<Param> telescope,
    @NotNull Ret result
  ) {
    this.telescope = telescope;
    this.result = result;
  }

  @Override public @NotNull ImmutableSeq<Param> telescope() {
    return telescope;
  }

  @Override public @NotNull Ret result() {
    return result;
  }
}
