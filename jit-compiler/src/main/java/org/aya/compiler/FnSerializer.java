// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Either;
import org.aya.generic.Modifier;
import org.aya.generic.NameGenerator;
import org.aya.syntax.compile.JitFn;
import org.aya.syntax.core.def.FnDef;
import org.jetbrains.annotations.NotNull;

public final class FnSerializer extends JitTeleSerializer<FnDef> {
  public FnSerializer(@NotNull StringBuilder builder, int indent, @NotNull NameGenerator nameGen) {
    super(builder, indent, nameGen, JitFn.class);
  }

  public FnSerializer(@NotNull AbstractSerializer<?> other) {
    super(other, JitFn.class);
  }

  @Override protected void buildConstructor(FnDef unit) {
    super.buildConstructor(unit, ImmutableSeq.empty());
  }

  private void buildInvoke(FnDef unit, @NotNull String onStuckTerm, @NotNull String argsTerm) {
    if (unit.is(Modifier.Opaque)) {
      buildReturn(onStuckTerm);
      return;
    }
    switch (unit.body()) {
      case Either.Left(var expr) -> buildReturn(serializeTermUnderTele(expr, argsTerm, unit.telescope().size()));
      case Either.Right(var clauses) -> {
        var names = buildGenLocalVarsFromSeq(CLASS_TERM, argsTerm, unit.telescope().size());
        appendLine();

        var ser = new PatternSerializer(this.builder, this.indent, this.nameGen, names, false,
          s -> s.buildReturn(onStuckTerm), s -> s.buildReturn(onStuckTerm));
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
