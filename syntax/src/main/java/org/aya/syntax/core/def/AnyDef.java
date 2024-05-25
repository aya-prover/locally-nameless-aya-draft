// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.def;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.compile.JitDef;
import org.aya.syntax.ref.ModulePath;
import org.aya.util.binop.Assoc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A well-typed generic definition,
 * it can be a core def (like {@link DataDef}) or a compiled def (like {@link org.aya.syntax.compile.JitData}).<br/>
 * We have three "def-chain"s, take {@link ConDef} as an example:
 * <pre>
 *   TyckDef <-----   AnyDef   -----> JitTeleDef
 *      |               |                  |
 *      v               v                  v
 *   ConDef  <----- ConDefLike ----->   JitCon
 * </pre>
 * where the arrows indicate mean ""is superclass of
 */
public sealed interface AnyDef permits JitDef, ConDefLike, DataDefLike, FnDefLike, TyckAnyDef {
  /**
   * Returns which file level module this def lives in.
   */
  @NotNull ModulePath fileModule();

  /**
   * Returns which module this def lives in, have the same prefix as {@link #fileModule()}
   */
  @NotNull ModulePath module();
  @NotNull String name();
  @Nullable Assoc assoc();

  default @NotNull ImmutableSeq<String> qualifiedName() {
    return module().module().appended(name());
  }
}
