// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.pat;

import kala.collection.immutable.ImmutableSeq;
import org.aya.generic.AyaDocile;
import org.aya.pretty.doc.Doc;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.CtorDef;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.DefVar;
import org.aya.syntax.ref.LocalCtx;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.prettier.PrettierOptions;
import org.jetbrains.annotations.Debug;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

/**
 * Patterns in the core syntax.
 *
 * @author kiva, ice1000, HoshinoTented
 */
@Debug.Renderer(text = "toTerm().toDoc(AyaPrettierOptions.debug()).debugRender()")
public interface Pat extends AyaDocile {
  @NotNull Pat descent(@NotNull UnaryOperator<Pat> patOp, @NotNull UnaryOperator<Term> termOp);

  /**
   * Puts bindings of this {@link Pat} to {@param ctx}
   */
  void storeBindings(@NotNull LocalCtx ctx, @NotNull UnaryOperator<Term> typeMapper);

  @Override
  default @NotNull Doc toDoc(@NotNull PrettierOptions options) {
    throw new UnsupportedOperationException("TODO");
  }
  record Bind(
    @NotNull LocalVar bind,
    @NotNull Term type
  ) implements Pat {
    public @NotNull Bind update(@NotNull Term type) {
      return this.type == type ? this : new Bind(bind, type);
    }

    @Override
    public @NotNull Pat descent(@NotNull UnaryOperator<Pat> patOp, @NotNull UnaryOperator<Term> termOp) {
      return update(termOp.apply(type));
    }

    @Override
    public void storeBindings(@NotNull LocalCtx ctx, @NotNull UnaryOperator<Term> typeMapper) {
      ctx.put(bind, typeMapper.apply(type));
    }
  }

  record Tuple(@NotNull ImmutableSeq<Pat> elements) implements Pat {
    public @NotNull Tuple update(@NotNull ImmutableSeq<Pat> elements) {
      return this.elements.sameElements(elements, true) ? this : new Tuple(elements);
    }

    @Override
    public @NotNull Pat descent(@NotNull UnaryOperator<Pat> patOp, @NotNull UnaryOperator<Term> termOp) {
      return update(elements.map(patOp));
    }

    @Override
    public void storeBindings(@NotNull LocalCtx ctx, @NotNull UnaryOperator<Term> typeMapper) {
      elements.forEach(e -> e.storeBindings(ctx, typeMapper));
    }
  }

  record Ctor(
    @NotNull DefVar<CtorDef, TeleDecl.DataCtor> ref,
    @NotNull ImmutableSeq<Pat> args
  ) implements Pat {

    @Override
    public @NotNull Pat descent(@NotNull UnaryOperator<Pat> patOp, @NotNull UnaryOperator<Term> termOp) {
      return null;
    }

    @Override
    public void storeBindings(@NotNull LocalCtx ctx, @NotNull UnaryOperator<Term> typeMapper) {

    }
  }
}
