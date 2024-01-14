package org.aya.syntax.core.term;

import kala.collection.immutable.ImmutableSeq;
import kala.function.IndexedFunction;
import org.aya.generic.AyaDocile;
import org.aya.pretty.doc.Doc;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.Arg;
import org.aya.util.error.SourcePos;
import org.aya.util.prettier.PrettierOptions;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public sealed interface Term extends Serializable, AyaDocile
  permits AppTerm, Callable, Formation, FreeTerm, LocalTerm, StableWHNF {
  @Override
  default @NotNull Doc toDoc(@NotNull PrettierOptions options) {
    throw new UnsupportedOperationException("TODO");
  }

  @ApiStatus.Internal
  default @NotNull Term bindAt(@NotNull LocalVar var, int depth) {
    return descent((i, t) -> t.bindAt(var, depth + i));
  }

  /**
   * Corresponds to <emph>abstract</emph> operator in [MM 2004].
   * However, <code>abstract</code> is a keyword in Java, so we can't
   * use it as a method name.
   * <pre>
   * abstract :: Name → Expr → Scope
   * </pre>
   *
   * @see #instantiate
   */
  default @NotNull Term bind(@NotNull LocalVar var) {
    return bindAt(var, 0);
  }

  @ApiStatus.Internal
  default @NotNull Term replace(int index, @NotNull Term arg) {
    return descent((i, t) -> t.replace(index + i, arg));
  }

  /**
   * Corresponds to <emph>instantiate</emph> operator in [MM 2004].
   * Could be called <code>apply</code> similar to Mini-TT.
   */
  default @NotNull Term instantiate(Term arg) {
    return replace(0, arg);
  }

  /**
   * @implNote implements {@link Term#bindAt} and {@link Term#replace} if this term is a leaf node.
   */
  @NotNull Term descent(@NotNull IndexedFunction<Term, Term> f);

  record Matching(
    @NotNull SourcePos sourcePos,
    @NotNull ImmutableSeq<Arg<Pat>> patterns,
    @NotNull Term body
  ) {
    public @NotNull Matching update(@NotNull ImmutableSeq<Arg<Pat>> patterns, @NotNull Term body) {
      return body == body() && patterns.sameElements(patterns(), true) ? this
        : new Matching(sourcePos, patterns, body);
    }

    public @NotNull Matching descent(@NotNull UnaryOperator<Term> f, @NotNull UnaryOperator<Pat> g) {
      return update(patterns.map(p -> p.descent(g)), f.apply(body));
    }

    public void descentConsume(@NotNull Consumer<Term> f, @NotNull Consumer<Pat> g) {
      patterns.forEach(a -> a.descentConsume(g));
      f.accept(body);
    }
  }
}
