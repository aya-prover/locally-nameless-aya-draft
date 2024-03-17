// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.ref;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.concrete.stmt.QualifiedID;
import org.jetbrains.annotations.NotNull;

public record ModulePath(@NotNull ImmutableSeq<String> module) {
  public boolean isInModule(@NotNull ModulePath other) {
    var moduleName = other.module;
    if (module.sizeLessThan(moduleName.size())) return false;
    return module.sliceView(0, moduleName.size()).sameElements(moduleName);
  }

  public boolean sameElements(ModulePath other) {
    return module.sameElements(other.module);
  }

  public @NotNull ModulePath derive(@NotNull String modName) {
    return new ModulePath(module.appended(modName));
  }

  public @NotNull ModulePath derive(@NotNull ImmutableSeq<String> modName) {
    return new ModulePath(module.concat(modName));
  }

  @Override
  public String toString() {
    return QualifiedID.join(module);
  }
}
