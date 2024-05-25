// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

import kala.collection.immutable.ImmutableArray;
import org.aya.syntax.core.def.AnyDef;
import org.aya.syntax.ref.ModulePath;
import org.aya.util.binop.Assoc;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

/**
 * A well-typed compiled definition
 *
 * @implNote every definition should be annotated by {@link CompiledAya}
 */
public abstract sealed class JitDef extends JitTele implements AnyDef permits JitCon, JitData, JitFn {
  private CompiledAya metadata;
  protected JitDef(int telescopeSize, boolean[] telescopeLicit, String[] telescopeNames) {
    super(telescopeSize, telescopeLicit, telescopeNames);
  }

  public @NotNull CompiledAya metadata() {
    if (metadata == null) metadata = getClass().getAnnotation(CompiledAya.class);
    if (metadata == null) throw new Panic(STR."No @CompiledAya on \{getClass().getName()}");
    return metadata;
  }

  @Override public @NotNull ModulePath fileModule() { return ModulePath.of(metadata().fileModule()); }

  @Override public @NotNull ModulePath module() {
    return new ModulePath(ImmutableArray.Unsafe.wrap(metadata().module()));
  }

  @Override public @NotNull String name() { return metadata().name(); }
  @Override public @NotNull Assoc assoc() { return metadata().assoc(); }
}
