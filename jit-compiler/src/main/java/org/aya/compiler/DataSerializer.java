// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import kala.tuple.Tuple;
import kala.tuple.Tuple2;
import org.aya.generic.NameGenerator;
import org.aya.syntax.compile.JitData;
import org.aya.syntax.core.def.DataDef;
import org.aya.syntax.core.term.Param;
import org.jetbrains.annotations.NotNull;

public final class DataSerializer extends JitTeleSerializer<DataDef> {
  public DataSerializer(
    @NotNull StringBuilder builder,
    int indent,
    @NotNull NameGenerator nameGen
  ) {
    super(builder, indent, nameGen, JitData.class.getName());
  }

  @Override
  public AyaSerializer<DataDef> serialize(DataDef unit) {
    return this;
  }

  @Override
  protected void buildConstructor(DataDef unit) {
    var tele = unit.telescope();
    var size = tele.size();
    var licit = tele.view().map(Param::explicit).map(Object::toString);
    var names = tele.view().map(Param::name);

    buildSuperCall(ImmutableSeq.of(
      Integer.toString(size),
      arrayFrom("boolean", licit.toImmutableSeq()),
      arrayFrom("String", names.toImmutableArray()),
      Integer.toString(unit.body.size())
    ));
  }

  @Override
  protected String getClassName(DataDef unit) {
    return unit.ref.name();
  }

  @Override
  protected void buildTelescope(DataDef unit, @NotNull String iTerm, @NotNull String teleArgsTerm) {
    var tele = unit.telescope;
    var jumpTable = MutableList.<Tuple2<String, Runnable>>create();
    tele.forEachIndexed((idx, p) -> {
      jumpTable.append(Tuple.of(
        Integer.toString(idx), () -> {
          var serializer = new TermSerializer(this.builder, this.indent, this.nameGen, fromArray(teleArgsTerm, idx));
          buildReturn(serializer.serialize(tele.get(idx)).result());
        }
      ));
    });

    buildSwitch(iTerm, jumpTable.toImmutableSeq());
  }

  @Override
  protected void buildResult(DataDef unit, @NotNull String teleArgsTerm) {

  }

  private void buildConstructors(DataDef unit) {

  }

  public @NotNull String arrayFrom(@NotNull String type, @NotNull ImmutableSeq<String> elements) {
    var builder = new StringBuilder();
    builder.append("new ");
    builder.append(type);
    builder.append("[] { ");
    elements.joinTo(builder, ", ");
    builder.append(" }");
    return builder.toString();
  }
}
