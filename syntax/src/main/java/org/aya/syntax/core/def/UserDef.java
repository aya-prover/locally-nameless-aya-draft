// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.def;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.Term;
import org.aya.util.reporter.Problem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Definitions by user.
 *
 * @author ice1000
 */
public sealed abstract class UserDef<Ret extends Term>
  extends TopLevelDef<Ret> permits FnDef, DataDef {
  /**
   * In case of counterexamples, this field will be assigned.
   */
  public @Nullable ImmutableSeq<Problem> problems;

  protected UserDef(@NotNull ImmutableSeq<Param> telescope, @NotNull Ret result) {
    super(telescope, result);
  }
}
