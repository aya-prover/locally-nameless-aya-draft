// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableMap;
import kala.collection.immutable.ImmutableSeq;
import kala.tuple.Tuple;
import org.aya.generic.NameGenerator;
import org.aya.syntax.compile.JitData;
import org.aya.syntax.core.def.DataDef;
import org.aya.syntax.core.def.TyckDef;
import org.aya.syntax.core.repr.CodeShape;
import org.aya.syntax.core.repr.ShapeRecognition;
import org.aya.syntax.ref.DefVar;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

// You should compile this with its constructors
public final class DataSerializer extends JitTeleSerializer<DataDef> {
  private final @NotNull Consumer<DataSerializer> conContinuation;

  /**
   * @param conContinuation should generate constructor inside of this data
   */
  public DataSerializer(
    @NotNull StringBuilder builder,
    int indent,
    @NotNull NameGenerator nameGen,
    @NotNull Consumer<DataSerializer> conContinuation
  ) {
    super(builder, indent, nameGen, JitData.class);
    this.conContinuation = conContinuation;
  }

  @Override public AyaSerializer<DataDef> serialize(DataDef unit) {
    buildFramework(unit, () -> {
      buildMethod("constructors", ImmutableSeq.empty(), STR."\{CLASS_JITCON}[]", true,
        () -> buildConstructors(unit));
      appendLine();
      conContinuation.accept(this);
    });

    return this;
  }

  @Override
  protected @NotNull String buildCapture(DataDef unit, @NotNull ShapeRecognition recog) {
    // The capture is one-to-one
    var flipped = ImmutableMap.from(recog.captures().toImmutableSeq().view()
      .map(x -> Tuple.<DefVar<?, ?>, CodeShape.GlobalId>of(x.component2(), x.component1())));
    var capture = unit.body.map(x -> flipped.get(x.ref).toString());
    return makeHalfArrayFrom(capture);
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
        buildUpdate(STR."\{cRef}[\{idx}]", getInstance(getCoreQualified(con.ref)))));

    buildReturn(cRef);
  }
}
