// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.concrete;

import kala.collection.immutable.ImmutableSeq;
import kala.value.MutableValue;
import org.aya.generic.SortKind;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.BinOpElem;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

public sealed interface Expr {
  @NotNull Expr descent(@NotNull UnaryOperator<@NotNull Expr> f);

  record Param(
    @NotNull SourcePos sourcePos,
    @NotNull LocalVar ref,
    @NotNull WithPos<Expr> type,
    boolean explicit
    // @ForLSP MutableValue<Result> theCore
  ) {
    public Param(@NotNull SourcePos sourcePos, @NotNull LocalVar var, boolean explicit) {
      this(sourcePos, var, new WithPos<>(sourcePos, new Hole(false, null)), explicit);
    }

    public @NotNull Param update(@NotNull WithPos<Expr> type) {
      return type == type() ? this : new Param(sourcePos, ref, type, explicit);
    }

    public @NotNull Param descent(@NotNull UnaryOperator<Expr> f) {
      return update(type.descent(f));
    }
  }

  /**
   * @param explicit whether the hole is a type-directed programming goal or
   *                 a to-be-solved by tycking hole.
   * @author ice1000
   */
  record Hole(
    boolean explicit,
    @Nullable Expr filling, // TODO
    MutableValue<ImmutableSeq<LocalVar>> accessibleLocal
  ) implements Expr {
    public Hole(boolean explicit, @Nullable Expr filling) {
      this(explicit, filling, MutableValue.create());
    }

    public @NotNull Hole update(@Nullable Expr filling) {
      return filling == filling() ? this : new Hole(explicit, filling);
    }

    @Override public @NotNull Hole descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return update(filling == null ? null : f.apply(filling));
    }
  }

  record Unresolved(
    @NotNull String name    // TODO: QualifiedID
  ) implements Expr {
    @Override public @NotNull Unresolved descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return this;
    }
  }

  record Ref(@NotNull LocalVar var) implements Expr {
    @Override
    public @NotNull Expr descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return this;
    }
  }

  record Lam(@NotNull Param param, @NotNull WithPos<Expr> body) implements Expr {
    public @NotNull Lam update(@NotNull Param param, @NotNull WithPos<Expr> body) {
      return param == param() && body == body() ? this : new Lam(param, body);
    }

    @Override public @NotNull Lam descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return update(param.descent(f), body.descent(f));
    }
  }

  record Tuple(
    @NotNull ImmutableSeq<@NotNull WithPos<Expr>> items
  ) implements Expr {
    public @NotNull Expr.Tuple update(@NotNull ImmutableSeq<@NotNull WithPos<Expr>> items) {
      return items.sameElements(items(), true) ? this : new Tuple(items);
    }

    @Override public @NotNull Expr.Tuple descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return update(items.map(x -> x.descent(f)));
    }
  }

  record App(@NotNull WithPos<Expr> function, @NotNull NamedArg argument) implements Expr {
    public @NotNull App update(@NotNull WithPos<Expr> function, @NotNull NamedArg argument) {
      return function == function() && argument == argument() ? this : new App(function, argument);
    }

    @Override public @NotNull App descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return update(function.descent(f), argument.descent(f));
    }
  }

  record NamedArg(
    @NotNull SourcePos sourcePos,
    @Override boolean explicit,
    @Nullable String name,
    @NotNull WithPos<Expr> arg
  ) implements BinOpElem<Expr> {
    @Override
    public @NotNull Expr term() {
      return arg.data();
    }

    public @NotNull NamedArg update(@NotNull WithPos<Expr> expr) {
      return expr == arg ? this : new NamedArg(sourcePos, explicit, name, expr);
    }

    public @NotNull NamedArg descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return update(arg.descent(f));
    }
  }

  record Pi(
    @NotNull Param param,
    @NotNull WithPos<Expr> last
  ) implements Expr {
    public @NotNull Pi update(@NotNull Param param, @NotNull WithPos<Expr> last) {
      return param == param() && last == last() ? this : new Pi(param, last);
    }

    @Override public @NotNull Pi descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return update(param.descent(f), last.descent(f));
    }
  }

  record Sigma(
    @NotNull ImmutableSeq<@NotNull Param> params
  ) implements Expr {
    public @NotNull Sigma update(@NotNull ImmutableSeq<@NotNull Param> params) {
      return params.sameElements(params(), true) ? this : new Sigma(params);
    }

    @Override public @NotNull Sigma descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return update(params.map(param -> param.descent(f)));
    }
  }

  record RawSort(@NotNull SourcePos sourcePos, @NotNull SortKind kind) implements Expr {
    @Override public @NotNull RawSort descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return this;
    }
  }

  sealed interface Sort extends Expr {
    int lift();

    SortKind kind();

    @Override default @NotNull Sort descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return this;
    }
  }

  record Type(@Override int lift) implements Sort {
    @Override public SortKind kind() {
      return SortKind.Type;
    }
  }

  record Set(@Override int lift) implements Sort {
    @Override public SortKind kind() {
      return SortKind.Set;
    }
  }

  enum ISet implements Sort {
    INSTANCE;

    @Override public int lift() {
      return 0;
    }

    @Override public SortKind kind() {
      return SortKind.ISet;
    }
  }

  record LitInt(int integer) implements Expr {
    @Override public @NotNull LitInt descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return this;
    }
  }

  record LitString(@NotNull String string) implements Expr {
    @Override public @NotNull LitString descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return this;
    }
  }

  record BinOpSeq(
    @NotNull ImmutableSeq<NamedArg> seq
  ) implements Expr {
    public @NotNull BinOpSeq update(@NotNull ImmutableSeq<NamedArg> seq) {
      return seq.sameElements(seq(), true) ? this : new BinOpSeq(seq);
    }

    @Override public @NotNull BinOpSeq descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return update(seq.map(arg -> arg.descent(f)));
    }
  }
}
