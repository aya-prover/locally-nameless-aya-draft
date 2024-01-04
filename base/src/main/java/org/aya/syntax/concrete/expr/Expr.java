package org.aya.syntax.concrete.expr;

import kala.collection.immutable.ImmutableSeq;
import kala.value.MutableValue;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.BinOpElem;
import org.aya.util.error.SourceNode;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

public sealed interface Expr {
  @NotNull Expr descent(@NotNull UnaryOperator<@NotNull Expr> f);

  record Node(@NotNull SourcePos sourcePos, @NotNull Expr expr) implements SourceNode {
    public @NotNull Node map(@NotNull UnaryOperator<Expr> mapper) {
      var result = mapper.apply(expr);
      return result == expr ? this : new Node(sourcePos, result);
    }
  }

  record Param(
    @NotNull SourcePos sourcePos,
    @NotNull LocalVar ref,
    @NotNull Node type,
    boolean explicit
    // @ForLSP MutableValue<Result> theCore
  ) {
    public Param(@NotNull SourcePos sourcePos, @NotNull LocalVar var, boolean explicit) {
      this(sourcePos, var, new Node(sourcePos, new Hole(false, null)), explicit);
    }

    public @NotNull Param update(@NotNull Node type) {
      return type == type() ? this : new Param(sourcePos, ref, type, explicit);
    }

    public @NotNull Param descent(@NotNull UnaryOperator<Expr> f) {
      return update(type.map(f));
    }
  }

  /**
   * @param explicit whether the hole is a type-directed programming goal or
   *                 a to-be-solved by tycking hole.
   * @author ice1000
   */
  record Hole(
    boolean explicit,
    @Nullable Expr filling,
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

  record Lam(@NotNull Param param, @NotNull Node body) implements Expr {
    public @NotNull Lam update(@NotNull Param param, @NotNull Node body) {
      return param == param() && body == body() ? this : new Lam(param, body);
    }

    @Override public @NotNull Lam descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return update(param.descent(f), body.map(f));
    }
  }

  record Tuple(
    @NotNull ImmutableSeq<@NotNull Node> items
  ) implements Expr {
    public @NotNull Expr.Tuple update(@NotNull ImmutableSeq<@NotNull Node> items) {
      return items.sameElements(items(), true) ? this : new Tuple(items);
    }

    @Override public @NotNull Expr.Tuple descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return update(items.map(x -> x.map(f)));
    }
  }

  record App(@NotNull Node function, @NotNull NamedArg argument) implements Expr {
    public @NotNull App update(@NotNull Node function, @NotNull NamedArg argument) {
      return function == function() && argument == argument() ? this : new App(function, argument);
    }

    @Override public @NotNull App descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return update(function.map(f), argument.descent(f));
    }
  }

  record NamedArg(
    @NotNull SourcePos sourcePos,
    @Override boolean explicit,
    @Nullable String name,
    @NotNull Node arg
  ) implements BinOpElem<Expr> {
    @Override
    public @NotNull Expr term() {
      return arg.expr();
    }

    public @NotNull NamedArg update(@NotNull Node expr) {
      return expr == arg ? this : new NamedArg(sourcePos, explicit, name, expr);
    }

    public @NotNull NamedArg descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return update(arg.map(f));
    }
  }

  record Pi(
    @NotNull Param param,
    @NotNull Node last
  ) implements Expr {
    public @NotNull Pi update(@NotNull Param param, @NotNull Node last) {
      return param == param() && last == last() ? this : new Pi(param, last);
    }

    @Override public @NotNull Pi descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return update(param.descent(f), last.map(f));
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
}
