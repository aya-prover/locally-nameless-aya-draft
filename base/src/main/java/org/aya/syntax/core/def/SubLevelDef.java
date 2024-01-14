// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.def;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.core.term.Term;
import org.aya.util.Arg;
import org.jetbrains.annotations.NotNull;

/**
 * Coconstructors or constructors, in contrast to {@link TopLevelDef}.
 *
 * @author ice1000
 */
public sealed abstract class SubLevelDef implements TeleDef permits CtorDef {
  public final @NotNull ImmutableSeq<Arg<Term>> selfTele;
  public final @NotNull Term result;
  public final boolean coerce;

  protected SubLevelDef(
    @NotNull ImmutableSeq<Arg<Term>> selfTele,
    @NotNull Term result, boolean coerce
  ) {
    this.selfTele = selfTele;
    this.result = result;
    this.coerce = coerce;
  }

  public @NotNull SeqView<Arg<Term>> fullTelescope() {
    return selfTele.view();
  }

  @Override public @NotNull Term result() {
    return result;
  }
}
