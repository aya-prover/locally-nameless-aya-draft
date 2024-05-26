// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableSeq;
import org.aya.generic.NameGenerator;
import org.aya.syntax.core.def.*;
import org.aya.syntax.core.repr.AyaShape;
import org.aya.util.IterableUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Serializing a module, note that it may not a file module, so we need not to make importing.
 */
public final class ModuleSerializer extends AbstractSerializer<ImmutableSeq<TyckDef>> {
  private final @NotNull AyaShape.Factory shapeFactory;

  public ModuleSerializer(@NotNull StringBuilder builder, int indent, @NotNull NameGenerator nameGen, @NotNull AyaShape.Factory shapeFactory) {
    super(builder, indent, nameGen);
    this.shapeFactory = shapeFactory;
  }

  private void serializeCons(@NotNull DataDef dataDef, @NotNull DataSerializer serializer) {
    var ser = new ConSerializer(serializer.builder, serializer.indent, serializer.nameGen);
    IterableUtil.forEach(dataDef.body, ser::appendLine, ser::serialize);
  }

  private void doSerialize(@NotNull TyckDef unit) {
    switch (unit) {
      case FnDef teleDef -> new FnSerializer(builder, indent, nameGen)
        .serialize(teleDef);
      case DataDef dataDef ->
        new DataSerializer(builder, indent, nameGen, shapeFactory, ser -> serializeCons(dataDef, ser))
        .serialize(dataDef);
      case ConDef conDef -> new ConSerializer(builder, indent, nameGen)
        .serialize(conDef);
      case PrimDef primDef -> throw new UnsupportedOperationException("TODO");
    }
  }

  @Override public AyaSerializer<ImmutableSeq<TyckDef>> serialize(ImmutableSeq<TyckDef> unit) {
    IterableUtil.forEach(unit, this::appendLine, this::doSerialize);
    return this;
  }
}
