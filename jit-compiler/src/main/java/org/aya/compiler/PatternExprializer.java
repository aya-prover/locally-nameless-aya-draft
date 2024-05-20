// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableSeq;
import org.aya.generic.NameGenerator;
import org.aya.syntax.core.pat.Pat;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

public class PatternExprializer extends AbstractSerializer<Pat> {
  protected PatternExprializer(@NotNull NameGenerator nameGen) {
    super(new StringBuilder(), 0, nameGen);
  }

  private void serialize(@NotNull ImmutableSeq<Pat> pats) {
    if (pats.isEmpty()) {
      builder.append("ImmutableSeq.empty()");
      return;
    }

    builder.append("ImmutableSeq.of(");

    var it = pats.iterator();
    this.serialize(it.next());

    while (it.hasNext()) {
      builder.append(", ");
      this.serialize(it.next());
    }

    builder.append(")");
  }

  @Override
  public AyaSerializer<Pat> serialize(Pat unit) {
    switch (unit) {
      case Pat.Absurd _ -> builder.append(getInstance(PatternSerializer.CLASS_PAT_ABSURD));
      case Pat.Bind bind -> {
        // it is safe to new a LocalVar, this method will be called when meta solving only,
        // but the meta solver will eat all LocalVar so that it will be happy.
        builder.append(STR."new \{PatternSerializer.CLASS_PAT_BIND}(new LocalVar(\"dogfood\"), ErrorTerm.DUMMY)");
      }
      case Pat.ConLike con -> {
        var instance = PatternSerializer.getQualified(con);

        builder.append(STR."new \{PatternSerializer.CLASS_PAT_JCON}(\{getInstance(instance)}, ");
        serialize(con.args());
        builder.append(")");
      }
      case Pat.Meta meta -> Panic.unreachable();
      case Pat.Tuple tuple -> throw new UnsupportedOperationException();
      case Pat.ShapedInt shapedInt -> throw new UnsupportedOperationException();
    }

    return this;
  }
}
