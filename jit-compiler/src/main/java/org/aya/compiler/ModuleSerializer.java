// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableSeq;
import org.aya.generic.NameGenerator;
import org.aya.syntax.core.def.*;
import org.aya.util.IterableUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Serializing a module, note that it may not a file module, so we need not to make importing.
 */
public final class ModuleSerializer extends AbstractSerializer<ImmutableSeq<Def>> {
  public ModuleSerializer(@NotNull StringBuilder builder, int indent, @NotNull NameGenerator nameGen) {
    super(builder, indent, nameGen);
  }

  private void serializeCons(@NotNull DataDef dataDef, @NotNull DataSerializer serializer) {
    var ser = new ConSerializer(serializer.builder, serializer.indent, serializer.nameGen);
    IterableUtil.forEach(dataDef.body, ser::appendLine, ser::serialize);
  }

  private void doSerialize(@NotNull Def unit) {
    switch (unit) {
      case FnDef teleDef -> new FnSerializer(builder, indent, nameGen)
        .serialize(teleDef);
      case DataDef dataDef -> new DataSerializer(builder, indent, nameGen, ser -> serializeCons(dataDef, ser))
        .serialize(dataDef);
      case ConDef conDef -> new ConSerializer(builder, indent, nameGen)
        .serialize(conDef);
      case PrimDef primDef -> throw new UnsupportedOperationException("TODO");
    }
  }

  @Override
  public AyaSerializer<ImmutableSeq<Def>> serialize(ImmutableSeq<Def> unit) {
    IterableUtil.forEach(unit, this::appendLine, this::doSerialize);
    return this;
  }
}