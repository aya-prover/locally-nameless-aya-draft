// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.resolve.visitor;

import org.aya.resolve.context.Context;
import org.aya.syntax.concrete.Pattern;
import org.aya.syntax.concrete.stmt.ModuleName;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.CtorDef;
import org.aya.syntax.ref.AnyVar;
import org.aya.syntax.ref.DefVar;
import org.aya.util.PosedUnaryOperator;
import org.aya.util.error.InternalException;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PatternResolver implements PosedUnaryOperator<Pattern> {
  // DIRTY!!
  private @NotNull Context context;

  public PatternResolver(@NotNull Context context) {
    this.context = context;
  }

  public @NotNull Context context() {
    return context;
  }

  public @NotNull Pattern apply(@NotNull SourcePos pos, @NotNull Pattern pat) {
    return post(pos, pat.descent(this));
  }

  public @NotNull Pattern post(@NotNull SourcePos pos, @NotNull Pattern pat) {
    return switch (pat) {
      case Pattern.Bind bind -> {
        // Check whether this {bind} is a Ctor
        var conMaybe = context.iterate(ctx -> isCon(ctx.getUnqualifiedLocalMaybe(bind.bind().name(), pos)));
        if (conMaybe != null) {
          // It intents to be a ctor!
          // TODO: addReference
          yield new Pattern.Ctor(pos, conMaybe);
        }

        // It is not a Ctor, it is a bind
        context = context.bind(bind.bind());
        yield bind;
      }
      case Pattern.QualifiedRef qref -> {
        var qid = qref.qualifiedID();
        if (!(qid.component() instanceof ModuleName.Qualified mod))
          throw new InternalException("QualifiedRef#qualifiedID should be qualified");
        var conMaybe = context.iterate(ctx -> isCon(ctx.getQualifiedLocalMaybe(mod, qid.name(), pos)));
        if (conMaybe != null) {
          yield new Pattern.Ctor(pos, conMaybe);
        }

        // !! No Such Thing !!
        // TODO report no such symbol
        throw new UnsupportedOperationException("TODO");
      }
      case Pattern.As as -> {
        context = context.bind(as.as());
        yield as;
      }
      default -> pat;
    };
  }

  private static @Nullable DefVar<?, ?> isCon(@Nullable AnyVar myMaybe) {
    if (myMaybe == null) return null;
    if (myMaybe instanceof DefVar<?, ?> def && (
      def.core instanceof CtorDef
        || def.concrete instanceof TeleDecl.DataCtor
      // || def.core instanceof PrimDef
      // || def.concrete instanceof TeleDecl.PrimDecl
    )) return def;

    return null;
  }
}