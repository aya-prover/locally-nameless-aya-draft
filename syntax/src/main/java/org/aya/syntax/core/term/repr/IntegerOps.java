// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.term.repr;

import kala.collection.immutable.ImmutableSeq;
import org.aya.generic.stmt.Shaped;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.core.def.ConDef;
import org.aya.syntax.core.def.FnDef;
import org.aya.syntax.core.def.Def;
import org.aya.syntax.core.repr.ShapeRecognition;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.ref.DefVar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * IntegerOps acts like a DefVar with special reduce rule. So it is not a {@link Term}.
 *
 * @see org.aya.syntax.core.term.call.RuleReducer
 */
public sealed interface IntegerOps<Core extends Def, Concrete extends Decl>
  extends Shaped.Applicable<Term, Core, Concrete> {
  @Override default @NotNull Term type() {
    var core = ref().core;
    assert core != null;
    return Def.defType(ref());
  }

  record ConRule(
    @Override @NotNull DefVar<ConDef, Decl.DataCon> ref,
    @Override @NotNull ShapeRecognition paramRecognition,
    @Override @NotNull DataCall paramType
  ) implements IntegerOps<ConDef, Decl.DataCon> {
    @Override public @Nullable Term apply(@NotNull ImmutableSeq<Term> args) {
      // zero
      if (args.isEmpty()) return new IntegerTerm(0, paramRecognition, paramType);

      // suc
      assert args.sizeEquals(1);
      var arg = args.get(0);
      if (arg instanceof IntegerTerm intTerm) return intTerm.map(x -> x + 1);
      return null;
    }
  }

  record FnRule(
    @Override @NotNull DefVar<FnDef, Decl.FnDecl> ref,
    @NotNull Kind kind
  ) implements IntegerOps<FnDef, Decl.FnDecl> {
    public enum Kind implements Serializable {
      Add, SubTrunc
    }

    @Override public @Nullable Term apply(@NotNull ImmutableSeq<Term> args) {
      return switch (kind) {
        case Add -> {
          assert args.sizeEquals(2);
          if (args.get(0) instanceof IntegerTerm ita && args.get(1) instanceof IntegerTerm itb) {
            yield ita.map(x -> x + itb.repr());
          }

          yield null;
        }
        case SubTrunc -> {
          assert args.sizeEquals(2);
          if (args.get(0) instanceof IntegerTerm ita && args.get(1) instanceof IntegerTerm itb) {
            yield ita.map(x -> Math.max(x - itb.repr(), 0));
          }

          yield null;
        }
      };
    }
  }
}
