// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.collection.mutable.MutableMap;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.MetaVar;
import org.jetbrains.annotations.NotNull;

public record TyckState(
  @NotNull MutableMap<MetaVar, Term> solutions
) {
  public TyckState() {
    this(MutableMap.create());
  }
}
