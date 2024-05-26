// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableSeq;
import org.aya.generic.NameGenerator;
import org.aya.syntax.core.pat.Pat;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

public class PatternExprializer extends AbstractExprializer<Pat> {
  public static final String CLASS_PAT = getName(Pat.class);

  protected PatternExprializer(@NotNull NameGenerator nameGen) {
    super(new StringBuilder(), nameGen);
  }

  private void doSerialize(@NotNull ImmutableSeq<Pat> pats) {
    buildImmutableSeq(CLASS_PAT, pats);
  }

  @Override
  protected @NotNull AbstractSerializer<Pat> doSerialize(@NotNull Pat term) {
    switch (term) {
      case Pat.Absurd _ -> builder.append(getInstance(PatternSerializer.CLASS_PAT_ABSURD));
      // it is safe to new a LocalVar, this method will be called when meta solving only,
      // but the meta solver will eat all LocalVar so that it will be happy.
      case Pat.Bind bind -> builder.append(
        STR."new \{PatternSerializer.CLASS_PAT_BIND}(new LocalVar(\"\{bind.bind().name()}\"), ErrorTerm.DUMMY)");
      case Pat.Con con -> {
        var instance = PatternSerializer.getQualified(con);

        builder.append(STR."new \{PatternSerializer.CLASS_PAT_CON}(\{getInstance(instance)}, ");
        doSerialize(con.args());
        builder.append(")");
      }
      case Pat.Meta _ -> Panic.unreachable();
      case Pat.Tuple tuple -> throw new UnsupportedOperationException();
      case Pat.ShapedInt shapedInt -> throw new UnsupportedOperationException();
    }

    return this;
  }

  @Override
  public AyaSerializer<Pat> serialize(Pat unit) {
    return doSerialize(unit);
  }
}
