// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableSeq;
import org.aya.generic.NameGenerator;
import org.aya.syntax.compile.JitData;
import org.aya.syntax.core.def.DataDef;
import org.jetbrains.annotations.NotNull;

// You should compile this with its constructors
public final class DataSerializer extends JitTeleSerializer<DataDef> {
  public DataSerializer(
    @NotNull StringBuilder builder,
    int indent,
    @NotNull NameGenerator nameGen
  ) { super(builder, indent, nameGen, JitData.class.getName()); }

  @Override public AyaSerializer<DataDef> serialize(DataDef unit) {
    buildFramework(unit, () -> {
      // TODO: is it better to be synchronized ?
      buildMethod("constructors", ImmutableSeq.empty(), STR."\{CLASS_JITCON}[]", true,
        () -> buildConstructors(unit));
    });

    return this;
  }

  @Override protected void buildConstructor(DataDef unit) {
    buildConstructor(unit, ImmutableSeq.of(Integer.toString(unit.body.size())));
  }

  /**
   * @see JitData#constructors()
   */
  private void buildConstructors(DataDef unit) {
    var cRef = "this.constructors";

    buildIf(isNull(STR."\{cRef}[0]"), () ->
      unit.body.forEachIndexed((idx, con) ->
        buildUpdate(STR."\{cRef}[\{idx}]", getInstance(getQualified(con.ref)))));

    buildReturn(cRef);
  }
}
