// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.concrete.stmt;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Option;
import kala.value.LazyValue;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.concrete.Pattern;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.TeleDef;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.AnyVar;
import org.aya.syntax.ref.DefVar;
import org.aya.util.error.PosedConsumer;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface StmtExprVisitor extends Consumer<Stmt>, PosedConsumer<Expr> {
  default void visitModuleDecl(@NotNull SourcePos pos, @NotNull ModuleName path) { }
  default void visitModuleRef(@NotNull SourcePos pos, @NotNull ModuleName path) { }
  default void visitVar(
    @NotNull SourcePos pos, @NotNull AnyVar path,
    @NotNull LazyValue<@Nullable Term> type
  ) { }
  default void visitVarRef(
    @NotNull SourcePos pos, @NotNull AnyVar path,
    @NotNull LazyValue<@Nullable Term> type
  ) { visitVar(pos, path, type); }
  default void visitVarDecl(
    @NotNull SourcePos pos, @NotNull AnyVar path,
    @NotNull LazyValue<@Nullable Term> type
  ) { visitVar(pos, path, type); }

  @SuppressWarnings("unchecked") default @Nullable Term varType(@Nullable AnyVar var) {
    if (var instanceof DefVar<?, ?> defVar && defVar.core instanceof TeleDef)
      return TeleDef.defType((DefVar<? extends TeleDef, ? extends TeleDecl<?>>) defVar);
    return null;
  }

  default @NotNull LazyValue<@Nullable Term> lazyType(@Nullable AnyVar var) {
    return LazyValue.of(() -> varType(var));
  }

  /** @implNote Should conceptually only be used outside of these visitors, where types are all ignored. */
  @NotNull LazyValue<@Nullable Term> noType = LazyValue.ofValue(null);
  default void visit(@NotNull BindBlock bb) {
    var t = Option.ofNullable(bb.resolvedTighters().get()).getOrElse(ImmutableSeq::empty);
    var l = Option.ofNullable(bb.resolvedLoosers().get()).getOrElse(ImmutableSeq::empty);
    t.forEachWith(bb.tighters(), (tt, b) -> visitVarRef(b.sourcePos(), tt, lazyType(tt)));
    l.forEachWith(bb.loosers(), (ll, b) -> visitVarRef(b.sourcePos(), ll, lazyType(ll)));
  }

  @MustBeInvokedByOverriders
  default void visitVars(@NotNull Stmt stmt) {
    switch (stmt) {
      case Generalize g -> g.variables.forEach(v -> visitVarDecl(v.sourcePos, v, noType));
      case Command.Module m -> visitModuleDecl(m.sourcePos(), new ModuleName.Qualified(m.name()));
      case Command.Import i -> visitModuleRef(i.sourcePos(), i.path().asName());
      case Command.Open o when o.fromSugar() -> { }  // handled in `case Decl` or `case Command.Import`
      case Command.Open o -> {
        visitModuleRef(o.sourcePos(), o.path());
        // https://github.com/aya-prover/aya-dev/issues/721
        o.useHide().list().forEach(v -> visit(v.asBind()));
      }
      case Decl decl -> {
        visit(decl.bindBlock());
        if (decl instanceof TeleDecl<?> teleDecl) {
          visitVarDecl(decl.sourcePos(), decl.ref(), lazyType(decl.ref()));
          teleDecl.telescope.forEach(p -> visitVarDecl(p.sourcePos(), p.ref(), withTermType(p)));
        }
      }
    }
    ;
  }

  default void accept(@NotNull Stmt stmt) {
    switch (stmt) {
      case Decl decl -> {
        if (decl instanceof TeleDecl<?> telescopic) visitTelescopic(telescopic);
        switch (decl) {
          case TeleDecl.DataDecl data -> data.body.forEach(this);
          // case ClassDecl clazz -> clazz.members.forEach(this);
          case TeleDecl.FnDecl fn -> fn.body.forEach(this,
            cl -> cl.forEach(this, (pos, pat) -> apply(new WithPos<>(pos, pat))));
          case TeleDecl.DataCon con -> con.patterns.forEach(cl -> apply(cl.term()));
          // case TeleDecl.ClassMember field -> field.body = field.body.map(this);
          case TeleDecl.PrimDecl _ -> { }
        }
      }
      case Command command -> {
        switch (command) {
          case Command.Module module -> module.contents().forEach(this);
          case Command.Import _, Command.Open _ -> { }
        }
      }
      case Generalize generalize -> accept(generalize.type);
    }
  }

  void apply(WithPos<Pattern> pat);

  default void visitTelescopic(@NotNull TeleDecl<?> telescopic) {
    telescopic.telescope.forEach(param -> param.forEach(this));
    if (telescopic.result != null) accept(telescopic.result);
  }
  default @NotNull LazyValue<@Nullable Term> withTermType(@NotNull Expr.WithTerm term) {
    return LazyValue.of(term::coreType);
  }
}
