// Copyright (c) 2020-2023 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.concrete;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Option;
import org.aya.syntax.ref.AnyVar;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.Arg;
import org.aya.util.error.SourceNode;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

/**
 * Patterns in the concrete syntax.
 *
 * @author kiva, ice1000, HoshinoTented
 */
public sealed interface Pattern {
  @NotNull Pattern descent(@NotNull UnaryOperator<@NotNull Pattern> f);

  record Tuple(
    @NotNull ImmutableSeq<Arg<WithPos<Pattern>>> patterns
  ) implements Pattern {
    public @NotNull Tuple update(@NotNull ImmutableSeq<Arg<WithPos<Pattern>>> patterns) {
      return patterns.sameElements(patterns(), true) ? this : new Tuple(patterns);
    }

    @Override public @NotNull Tuple descent(@NotNull UnaryOperator<@NotNull Pattern> f) {
      return update(patterns.map(a -> a.descent(x -> x.descent(f))));
    }
  }

  record Number(int number) implements Pattern {
    @Override public @NotNull Number descent(@NotNull UnaryOperator<@NotNull Pattern> f) {
      return this;
    }
  }

  enum Absurd implements Pattern {
    INSTANCE;

    @Override
    public @NotNull Pattern descent(@NotNull UnaryOperator<@NotNull Pattern> f) {
      return this;
    }
  }

  record CalmFace(@Override @NotNull SourcePos sourcePos) implements Pattern {
    @Override public @NotNull CalmFace descent(@NotNull UnaryOperator<@NotNull Pattern> f) {
      return this;
    }
  }

  /**
   * @param userType only generated when a typed lambda is pushed into the patterns
   * @param type     used in the LSP server
   */
  record Bind(
    @NotNull LocalVar bind
    // @Nullable Expr userType
    // @ForLSP @NotNull MutableValue<@Nullable Term> type
  ) implements Pattern {
    @Override public @NotNull Bind descent(@NotNull UnaryOperator<@NotNull Pattern> f) {
      return this;
    }
  }

  // TODO: QualifiedRef here

  record Ctor(
    @NotNull WithPos<@NotNull AnyVar> resolved,
    @NotNull ImmutableSeq<Arg<WithPos<Pattern>>> params
  ) implements Pattern {
    public Ctor(@NotNull WithPos<Pattern.Bind> bind, @NotNull AnyVar maybe) {
      this(new WithPos<>(bind.sourcePos(), maybe), ImmutableSeq.empty());
    }

    // public Ctor(@NotNull Pattern.QualifiedRef qref, @NotNull AnyVar maybe) {
    //   this(qref.sourcePos(), new WithPos<>(qref.sourcePos(), maybe), ImmutableSeq.empty());
    // }

    public @NotNull Ctor update(@NotNull ImmutableSeq<Arg<WithPos<Pattern>>> params) {
      return params.sameElements(params(), true) ? this : new Ctor(resolved, params);
    }

    @Override public @NotNull Ctor descent(@NotNull UnaryOperator<@NotNull Pattern> f) {
      return update(params.map(a -> a.descent(x -> x.descent(f))));
    }
  }

  record BinOpSeq(
    @NotNull ImmutableSeq<Arg<WithPos<Pattern>>> seq
  ) implements Pattern {
    public @NotNull BinOpSeq update(@NotNull ImmutableSeq<Arg<WithPos<Pattern>>> seq) {
      return seq.sameElements(seq(), true) ? this : new BinOpSeq(seq);
    }

    @Override public @NotNull BinOpSeq descent(@NotNull UnaryOperator<@NotNull Pattern> f) {
      return update(seq.map(a -> a.descent(x -> x.descent(f))));
    }
  }

  /**
   * Represent a {@code (Pattern) as bind} pattern
   */
  record As(
    @NotNull WithPos<Pattern> pattern,
    @NotNull LocalVar as
    // @ForLSP @NotNull MutableValue<@Nullable Term> type
  ) implements Pattern {
    public static Arg<Pattern> wrap(@NotNull Arg<WithPos<Pattern>> pattern, @NotNull LocalVar var) {
      return new Arg<>(new As(pattern.term(), var), pattern.explicit());
    }

    public @NotNull As update(@NotNull WithPos<Pattern> pattern) {
      return pattern == pattern() ? this : new As(pattern, as);
    }

    @Override public @NotNull As descent(@NotNull UnaryOperator<@NotNull Pattern> f) {
      return update(pattern.descent(f));
    }
  }

  /**
   * @author kiva, ice1000
   */
  final class Clause implements SourceNode {
    public final @NotNull SourcePos sourcePos;
    public final @NotNull ImmutableSeq<Arg<WithPos<Pattern>>> patterns;
    public final @NotNull Option<WithPos<Expr>> expr;
    public boolean hasError = false;

    public Clause(@NotNull SourcePos sourcePos, @NotNull ImmutableSeq<Arg<WithPos<Pattern>>> patterns, @NotNull Option<WithPos<Expr>> expr) {
      this.sourcePos = sourcePos;
      this.patterns = patterns;
      this.expr = expr;
    }

    public @NotNull Clause update(@NotNull ImmutableSeq<Arg<WithPos<Pattern>>> pats, @NotNull Option<WithPos<Expr>> body) {
      return body.sameElements(expr, true) && pats.sameElements(patterns, true) ? this
        : new Clause(sourcePos, pats, body);
    }

    public @NotNull Clause descent(@NotNull UnaryOperator<@NotNull Expr> f) {
      return update(patterns, expr.map(x -> x.descent(f)));
    }

    public @NotNull Clause descent(@NotNull UnaryOperator<@NotNull Expr> f, @NotNull UnaryOperator<@NotNull Pattern> g) {
      return update(patterns.map(p -> p.descent(x -> x.descent(g))), expr.map(x -> x.descent(f)));
    }

    @Override public @NotNull SourcePos sourcePos() {
      return sourcePos;
    }
  }
}
