package org.aya.syntax.core.term;

import kala.function.IndexedFunction;
import kala.function.IndexedIntUnaryOperator;
import org.aya.syntax.ref.LocalVar;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public sealed interface Term extends Serializable
  permits AppTerm, Formation, FreeTerm, LocalTerm, StableWHNF {
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
}
