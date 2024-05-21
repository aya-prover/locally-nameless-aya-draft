// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableSeq;
import kala.range.primitive.IntRange;
import org.aya.generic.NameGenerator;
import org.aya.syntax.compile.JitCon;
import org.aya.syntax.core.def.TeleDef;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.Term;
import org.jetbrains.annotations.NotNull;

public abstract class JitTeleSerializer<T extends TeleDef> extends AbstractSerializer<T> {
  public static final String CLASS_JITCON = getQualified(JitCon.class);

  protected final @NotNull String superClass;

  protected JitTeleSerializer(
    @NotNull StringBuilder builder,
    int indent,
    @NotNull NameGenerator nameGen,
    @NotNull String superClass
  ) {
    super(builder, indent, nameGen);
    this.superClass = superClass;
  }

  protected void buildFramework(T unit, @NotNull Runnable continuation) {
    var className = getClassName(unit);
    buildClass(className, superClass, () -> {
      buildInstance(className);
      appendLine();     // make code more pretty
      // empty return type for constructor
      buildMethod(className, ImmutableSeq.empty(), "/*constructor*/", false, () -> buildConstructor(unit));
      appendLine();
      var iTerm = "i";
      var teleArgsTerm = "teleArgs";
      buildMethod("telescope", ImmutableSeq.of(
        new JitParam("i", "int"),
        new JitParam("teleArgs", STR."\{CLASS_TERM}...")
      ), CLASS_TERM, true, () -> buildTelescope(unit, iTerm, teleArgsTerm));
      appendLine();
      buildMethod("result", ImmutableSeq.of(
        new JitParam("teleArgs", STR."\{CLASS_TERM}...")
      ), CLASS_TERM, true, () -> buildResult(unit, teleArgsTerm));
      appendLine();
      continuation.run();
    });
  }

  protected abstract String getClassName(T unit);

  /**
   * @see org.aya.syntax.compile.JitTele
   */
  protected abstract void buildConstructor(T unit);

  protected void buildConstructor(T def, @NotNull ImmutableSeq<String> ext) {
    var tele = def.telescope();
    var size = tele.size();
    var licit = tele.view().map(Param::explicit).map(Object::toString);
    var names = tele.view().map(Param::name).map(x -> STR."\"\{x}\"");

    buildSuperCall(ImmutableSeq.of(
      Integer.toString(size),
      arrayFrom("boolean", licit.toImmutableSeq()),
      arrayFrom("String", names.toImmutableArray())
    ).appendedAll(ext));
  }

  /**
   * @see org.aya.syntax.compile.JitTele#telescope(int, Term...)
   */
  protected void buildTelescope(T unit, @NotNull String iTerm, @NotNull String teleArgsTerm) {
    @NotNull ImmutableSeq<Param> tele = unit.telescope();
    buildSwitch(iTerm, IntRange.closedOpen(0, tele.size()).collect(ImmutableSeq.factory()), kase -> {
      var serializer = new TermSerializer(this.nameGen, fromArray(teleArgsTerm, kase));
      buildReturn(serializer.serialize(tele.get(kase).type()).result());
    }, () -> buildPanic(null));
  }

  /**
   * @see org.aya.syntax.compile.JitTele#result(Term...)
   */
  protected void buildResult(T unit, @NotNull String teleArgsTerm) {
    buildReturn(
      new TermSerializer(nameGen, fromArray(teleArgsTerm, unit.telescope().size()))
        .serialize(unit.result())
        .result()
    );
  }

  public void buildSuperCall(@NotNull ImmutableSeq<String> args) {
    appendLine(STR."super(\{args.joinToString(", ")});");
  }
}