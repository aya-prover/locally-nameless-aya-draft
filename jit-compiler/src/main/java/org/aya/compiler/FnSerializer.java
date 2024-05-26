// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Either;
import org.aya.generic.NameGenerator;
import org.aya.syntax.compile.JitFn;
import org.aya.syntax.core.def.FnDef;
import org.jetbrains.annotations.NotNull;

public final class FnSerializer extends JitTeleSerializer<FnDef> {
  public FnSerializer(@NotNull StringBuilder builder, int indent, @NotNull NameGenerator nameGen) {
    super(builder, indent, nameGen, JitFn.class);
  }

  @Override protected void buildConstructor(FnDef unit) {
    super.buildConstructor(unit, ImmutableSeq.empty());
  }

  private void buildInvoke(FnDef unit, @NotNull String onStuck, @NotNull String argsTerm) {
    switch (unit.body) {
      case Either.Left(var expr) -> buildReturn(serializeTermUnderTele(expr, argsTerm, unit.telescope().size()));
      case Either.Right(var clauses) -> {
        var ser = new PatternSerializer(this.builder, this.indent, this.nameGen, argsTerm, false,
          s -> s.buildReturn(onStuck), s -> s.buildReturn(onStuck));
        ser.serialize(clauses.map(matching -> new PatternSerializer.Matching(
            matching.patterns(),
            (s, bindSize) -> s.buildReturn(serializeTermUnderTele(matching.body(), PatternSerializer.VARIABLE_RESULT, bindSize))
        )));
      }
    }
  }

  @Override public AyaSerializer<FnDef> serialize(FnDef unit) {
    var argsTerm = "args";
    var onStuckTerm = "onStuck";
    var params = ImmutableSeq.of(
      new JitParam(onStuckTerm, CLASS_TERM),
      new JitParam(argsTerm, TYPE_TERMSEQ)
    );

    buildFramework(unit, () -> buildMethod("invoke", params, CLASS_TERM, true,
      () -> buildInvoke(unit, onStuckTerm, argsTerm)));

    return this;
  }
}
