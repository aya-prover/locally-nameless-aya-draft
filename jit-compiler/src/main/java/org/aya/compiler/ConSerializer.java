// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableSeq;
import org.aya.generic.NameGenerator;
import org.aya.syntax.compile.JitCon;
import org.aya.syntax.core.def.ConDef;
import org.jetbrains.annotations.NotNull;

public final class ConSerializer extends JitTeleSerializer<ConDef> {
  public ConSerializer(@NotNull StringBuilder builder, int indent, @NotNull NameGenerator nameGen) {
    super(builder, indent, nameGen, JitCon.class.getName());
  }

  @Override protected String getClassName(ConDef unit) { return javify(unit.ref.name()); }
  @Override protected void buildConstructor(ConDef unit) {
    buildConstructor(unit, ImmutableSeq.of(getInstance(getQualified(unit.dataRef))));
  }

  private void buildIsAvailable(ConDef unit, @NotNull String argsTerm) {
    var ser = new PatternSerializer(this.builder, this.indent, this.nameGen, argsTerm,
      () -> buildReturn(STR."\{CLASS_RESULT}.err(true)"),
      () -> buildReturn(STR."\{CLASS_RESULT}.err(false)"));

    ser.serialize(ImmutableSeq.of(new PatternSerializer.Matching(unit.pats,
      () -> buildReturn(STR."\{CLASS_RESULT}.ok(\{CLASS_IMMSEQ}.from(\{PatternSerializer.VARIABLE_RESULT}))"))));
  }

  @Override public AyaSerializer<ConDef> serialize(ConDef unit) {
    buildFramework(unit, () -> {
      buildMethod("isAvailable", ImmutableSeq.of(new JitParam("args", STR."\{CLASS_TERM}[]")),
        STR."\{CLASS_RESULT}<\{CLASS_IMMSEQ}<\{CLASS_TERM}>, \{CLASS_BOOLEAN}>", true, () -> {
          buildIsAvailable(unit, "args");
        });
    });

    return this;
  }
}
