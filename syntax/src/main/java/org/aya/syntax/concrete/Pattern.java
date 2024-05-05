// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.concrete;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Option;
import kala.value.MutableValue;
import org.aya.generic.AyaDocile;
import org.aya.pretty.doc.Doc;
import org.aya.syntax.concrete.stmt.QualifiedID;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.AnyVar;
import org.aya.syntax.ref.DefVar;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.Arg;
import org.aya.util.ForLSP;
import org.aya.util.PosedUnaryOperator;
import org.aya.util.error.SourceNode;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.aya.util.prettier.PrettierOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Patterns in the concrete syntax.
 *
 * @author kiva, ice1000, HoshinoTented
 */
public sealed interface Pattern extends AyaDocile {
  interface Salt {
  }

  @NotNull Pattern descent(@NotNull PosedUnaryOperator<@NotNull Pattern> f);

  @Override
  default @NotNull Doc toDoc(@NotNull PrettierOptions options) {
    throw new UnsupportedOperationException("TODO");
  }

  record Tuple(
    @NotNull ImmutableSeq<WithPos<Pattern>> patterns
  ) implements Pattern {
    public @NotNull Tuple update(@NotNull ImmutableSeq<WithPos<Pattern>> patterns) {
      return patterns.sameElements(patterns(), true) ? this : new Tuple(patterns);
    }

    @Override public @NotNull Tuple descent(@NotNull PosedUnaryOperator<@NotNull Pattern> f) {
      return update(patterns.map(a -> a.descent(f)));
    }
  }

  record Number(int number) implements Pattern {
    @Override public @NotNull Number descent(@NotNull PosedUnaryOperator<@NotNull Pattern> f) {
      return this;
    }
  }

  enum Absurd implements Pattern {
    INSTANCE;

    @Override
    public @NotNull Pattern descent(@NotNull PosedUnaryOperator<@NotNull Pattern> f) {
      return this;
    }
  }

  enum CalmFace implements Pattern {
    INSTANCE;

    @Override
    public @NotNull Pattern descent(@NotNull PosedUnaryOperator<@NotNull Pattern> f) {
      return this;
    }
  }

  /**
   * @param type used in the LSP server
   */
  record Bind(
    @NotNull LocalVar bind,
    @ForLSP @NotNull MutableValue<@Nullable Term> type
  ) implements Pattern {
    public Bind(@NotNull LocalVar bind) {
      this(bind, MutableValue.create());
    }
    @Override public @NotNull Bind descent(@NotNull PosedUnaryOperator<@NotNull Pattern> f) {
      return this;
    }
  }

  // TODO: QualifiedRef here

  record Ctor(
    @NotNull WithPos<@NotNull DefVar<?, ?>> resolved,
    @NotNull ImmutableSeq<Arg<WithPos<Pattern>>> params
  ) implements Pattern {
    public Ctor(@NotNull SourcePos pos, @NotNull DefVar<?, ?> maybe) {
      this(new WithPos<>(pos, maybe), ImmutableSeq.empty());
    }

    public @NotNull Ctor update(@NotNull ImmutableSeq<Arg<WithPos<Pattern>>> params) {
      return params.sameElements(params(), true) ? this : new Ctor(resolved, params);
    }

    @Override public @NotNull Ctor descent(@NotNull PosedUnaryOperator<@NotNull Pattern> f) {
      return update(params.map(a -> a.descent(x -> x.descent(f))));
    }
  }

  record BinOpSeq(
    @NotNull ImmutableSeq<Arg<WithPos<Pattern>>> seq
  ) implements Pattern, Salt {
    public @NotNull BinOpSeq update(@NotNull ImmutableSeq<Arg<WithPos<Pattern>>> seq) {
      return seq.sameElements(seq(), true) ? this : new BinOpSeq(seq);
    }

    @Override public @NotNull BinOpSeq descent(@NotNull PosedUnaryOperator<@NotNull Pattern> f) {
      return update(seq.map(a -> a.descent(x -> x.descent(f))));
    }
  }

  /**
   * Represent a {@code (Pattern) as bind} pattern
   */
  record As(
    @NotNull WithPos<Pattern> pattern,
    @NotNull LocalVar as,
    @ForLSP @NotNull MutableValue<@Nullable Term> type
  ) implements Pattern {
    public static Arg<Pattern> wrap(@NotNull Arg<WithPos<Pattern>> pattern, @NotNull LocalVar var) {
      return new Arg<>(new As(pattern.term(), var, MutableValue.create()), pattern.explicit());
    }

    public @NotNull As update(@NotNull WithPos<Pattern> pattern) {
      return pattern == pattern() ? this : new As(pattern, as, type);
    }

    @Override public @NotNull As descent(@NotNull PosedUnaryOperator<@NotNull Pattern> f) {
      return update(pattern.descent(f));
    }
  }

  /**
   * @param qualifiedID a qualified QualifiedID ({@code isUnqualified == false})
   */
  record QualifiedRef(
    @NotNull QualifiedID qualifiedID,
    @Nullable WithPos<Expr> userType,
    @ForLSP @NotNull MutableValue<@Nullable Term> type
  ) implements Pattern, Salt {
    public QualifiedRef(@NotNull QualifiedID qualifiedID) {
      this(qualifiedID, null, MutableValue.create());
    }

    @Override public @NotNull QualifiedRef descent(@NotNull PosedUnaryOperator<@NotNull Pattern> f) {
      return this;
    }
  }

  /** Sugared List Pattern */
  record List(
    @NotNull ImmutableSeq<WithPos<Pattern>> elements
  ) implements Pattern {
    public @NotNull List update(@NotNull ImmutableSeq<WithPos<Pattern>> elements) {
      return elements.sameElements(elements(), true) ? this : new List(elements);
    }

    @Override public @NotNull List descent(@NotNull PosedUnaryOperator<@NotNull Pattern> f) {
      return update(elements.map(x -> x.descent(f)));
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

    // public @NotNull Clause descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
    //   return update(patterns, expr.map(x -> x.descent(f)));
    // }

    public @NotNull Clause descent(@NotNull PosedUnaryOperator<@NotNull Expr> f, @NotNull PosedUnaryOperator<@NotNull Pattern> g) {
      return update(patterns.map(p -> p.descent(x -> x.descent(g))), expr.map(x -> x.descent(f)));
    }

    @Override public @NotNull SourcePos sourcePos() {
      return sourcePos;
    }
  }
}
