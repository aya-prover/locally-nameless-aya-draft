// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.concrete.stmt.decl;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import org.aya.generic.Modifier;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.concrete.Pattern;
import org.aya.syntax.concrete.stmt.BindBlock;
import org.aya.syntax.core.def.*;
import org.aya.syntax.ref.DefVar;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.Arg;
import org.aya.util.error.PosedConsumer;
import org.aya.util.error.PosedUnaryOperator;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Concrete telescopic definition, corresponding to {@link TeleDef}.
 *
 * @author re-xyr
 * @see Decl
 */
public sealed abstract class TeleDecl implements Decl {
  public @Nullable WithPos<Expr> result;
  // will change after resolve
  public @NotNull ImmutableSeq<Expr.Param> telescope;
  public @Nullable Signature signature;
  public @NotNull DeclInfo info;
  public boolean isExample;

  protected TeleDecl(
    @NotNull DeclInfo info, @NotNull ImmutableSeq<Expr.Param> telescope,
    @Nullable WithPos<Expr> result
  ) {
    this.info = info;
    this.result = result;
    this.telescope = telescope;
  }

  public void modifyResult(@NotNull PosedUnaryOperator<Expr> f) {
    if (result != null) result = result.descent(f);
  }

  @Override public void descentInPlace(@NotNull PosedUnaryOperator<Expr> f, @NotNull PosedUnaryOperator<Pattern> p) {
    telescope = telescope.map(param -> param.descent(f));
    modifyResult(f);
  }

  @Contract(pure = true) public abstract @NotNull DefVar<? extends TeleDef, ? extends TeleDecl> ref();
  @Override public @NotNull DeclInfo info() { return info; }
  public SeqView<LocalVar> teleVars() { return telescope.view().map(Expr.Param::ref); }

  /**
   * @implNote {@link TeleDecl#signature} is always null.
   */
  public static final class DataCon extends TeleDecl {
    public final @NotNull DefVar<ConDef, DataCon> ref;
    public DefVar<DataDef, DataDecl> dataRef;
    public @NotNull ImmutableSeq<Arg<WithPos<Pattern>>> patterns;
    public final boolean coerce;

    public DataCon(
      @NotNull DeclInfo info,
      @NotNull String name,
      @NotNull ImmutableSeq<Arg<WithPos<Pattern>>> patterns,
      @NotNull ImmutableSeq<Expr.Param> telescope,
      boolean coerce, @Nullable WithPos<Expr> result
    ) {
      super(info, telescope, result);
      this.patterns = patterns;
      this.coerce = coerce;
      this.ref = DefVar.concrete(this, name);
      this.telescope = telescope;
    }

    @Override
    public void descentInPlace(@NotNull PosedUnaryOperator<Expr> f, @NotNull PosedUnaryOperator<Pattern> p) {
      super.descentInPlace(f, p);
      // descent patterns
      patterns = patterns.map(x -> x.descent(wp -> wp.descent(p)));
    }
    @Override public @NotNull DefVar<ConDef, DataCon> ref() { return ref; }
  }

  /**
   * Concrete data definition
   *
   * @author kiva
   * @see DataDef
   */
  public static final class DataDecl extends TeleDecl {
    public final @NotNull DefVar<DataDef, DataDecl> ref;
    public final @NotNull ImmutableSeq<DataCon> body;
    /** Yet type-checked constructors */
    public final @NotNull MutableList<@NotNull ConDef> checkedBody = MutableList.create();

    public DataDecl(
      @NotNull DeclInfo info,
      @NotNull String name,
      @NotNull ImmutableSeq<Expr.Param> telescope,
      @Nullable WithPos<Expr> result,
      @NotNull ImmutableSeq<DataCon> body
    ) {
      super(info, telescope, result);
      this.body = body;
      this.ref = DefVar.concrete(this, name);
      body.forEach(con -> con.dataRef = ref);
    }

    @Override public void descentInPlace(@NotNull PosedUnaryOperator<Expr> f, @NotNull PosedUnaryOperator<Pattern> p) {
      super.descentInPlace(f, p);
      body.forEach(con -> con.descentInPlace(f, p));
    }

    @Override public @NotNull DefVar<DataDef, DataDecl> ref() { return ref; }
  }

  public sealed interface FnBody {
    FnBody map(@NotNull PosedUnaryOperator<Expr> f, @NotNull UnaryOperator<Pattern.Clause> g);
    void forEach(@NotNull PosedConsumer<Expr> f, @NotNull Consumer<Pattern.Clause> g);
  }

  public record ExprBody(@NotNull WithPos<Expr> expr) implements FnBody {
    @Override public ExprBody map(@NotNull PosedUnaryOperator<Expr> f, @NotNull UnaryOperator<Pattern.Clause> g) {
      return new ExprBody(expr.descent(f));
    }
    @Override public void forEach(@NotNull PosedConsumer<Expr> f, @NotNull Consumer<Pattern.Clause> g) {
      f.accept(expr);
    }
  }

  public record BlockBody(
    @NotNull ImmutableSeq<Pattern.Clause> clauses,
    @NotNull ImmutableSeq<WithPos<LocalVar>> elims
  ) implements FnBody {
    public BlockBody {

    }

    @Override public BlockBody map(@NotNull PosedUnaryOperator<Expr> f, @NotNull UnaryOperator<Pattern.Clause> g) {
      return new BlockBody(clauses.map(g), elims);
    }
    @Override public void forEach(@NotNull PosedConsumer<Expr> f, @NotNull Consumer<Pattern.Clause> g) {
      clauses.forEach(g);
    }
  }

  /**
   * Concrete function definition
   *
   * @author re-xyr
   * @see FnDef
   */
  public static final class FnDecl extends TeleDecl {
    public final @NotNull EnumSet<Modifier> modifiers;
    public final @NotNull DefVar<FnDef, FnDecl> ref;
    public @NotNull FnBody body;

    public FnDecl(
      @NotNull DeclInfo info,
      @NotNull EnumSet<Modifier> modifiers,
      @NotNull String name,
      @NotNull ImmutableSeq<Expr.Param> telescope,
      @Nullable WithPos<Expr> result,
      @NotNull FnBody body
    ) {
      super(info, telescope, result);

      this.modifiers = modifiers;
      this.ref = DefVar.concrete(this, name);
      this.body = body;
    }

    @Override public @NotNull DefVar<FnDef, FnDecl> ref() { return ref; }

    @Override
    public void descentInPlace(@NotNull PosedUnaryOperator<Expr> f, @NotNull PosedUnaryOperator<Pattern> p) {
      super.descentInPlace(f, p);
      body = body.map(f, cls -> cls.descent(f, p));
    }
  }

  /**
   * @implSpec the result field of {@link PrimDecl} might be {@link Expr.Error},
   * which means it's unspecified in the concrete syntax.
   * @see PrimDef
   */
  public static final class PrimDecl extends TeleDecl {
    public final @NotNull DefVar<PrimDef, PrimDecl> ref;

    public PrimDecl(
      @NotNull SourcePos sourcePos, @NotNull SourcePos entireSourcePos,
      @NotNull String name,
      @NotNull ImmutableSeq<Expr.Param> telescope,
      @Nullable WithPos<Expr> result
    ) {
      super(new DeclInfo(Accessibility.Public, sourcePos, entireSourcePos, null, BindBlock.EMPTY), telescope, result);
      ref = DefVar.concrete(this, name);
    }

    @Override public @NotNull DefVar<PrimDef, PrimDecl> ref() { return ref; }
  }
}
