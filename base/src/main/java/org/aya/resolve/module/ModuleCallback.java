// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.resolve.module;

import kala.collection.immutable.ImmutableSeq;
import org.aya.resolve.ResolveInfo;
import org.aya.syntax.core.def.Def;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ModuleCallback<E extends Exception> {
  // TODO[ice]: why param1 is needed?
  void onModuleTycked(@NotNull ResolveInfo moduleResolve, @NotNull ImmutableSeq<Def> defs)
    throws E;
}
