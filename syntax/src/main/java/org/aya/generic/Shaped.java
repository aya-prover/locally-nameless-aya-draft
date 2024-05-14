// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.generic;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.ConDef;
import org.aya.syntax.core.def.Def;
import org.aya.syntax.core.repr.CodeShape;
import org.aya.syntax.core.repr.ShapeRecognition;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.ref.DefVar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;
import java.util.function.IntUnaryOperator;

/**
 * <h2> What should I do after I creating a new Shape? </h2>
 * <ul>
 *   <li>impl your Shape, see {@link org.aya.syntax.core.term.repr.IntegerTerm},
 *   and do everything you should after you creating a {@link Term}/{@link org.aya.syntax.core.pat.Pat}.</l1>
 *   <li>impl TermComparator, see {@link TermComparator#doCompareUntyped(Term, Term)}</li>
 *   <li>impl PatMatcher, see {@link org.aya.core.pat.PatMatcher#match(Pat, Term)}</li>
 *   <li>impl PatUnifier, see {@link org.aya.core.pat.PatUnify#unify(Pat, Pat)}</li>
 * </ul>
 *
 * @param <T>
 */
public interface Shaped<T> {
  @NotNull Term type();

  sealed interface Inductive<T> extends Shaped<T> {
    @Override @NotNull DataCall type();
    @NotNull ShapeRecognition recognition();
    @NotNull T constructorForm();

    @SuppressWarnings("unchecked") default @NotNull DefVar<ConDef, ?> ctorRef(@NotNull CodeShape.GlobalId id) {
      return (DefVar<ConDef, ?>) recognition().captures().get(id);
    }

    default <O> boolean compareShape(BiPredicate<Term, Term> comparator, @NotNull Inductive<O> other) {
      if (recognition().shape() != other.recognition().shape()) return false;
      if (other.getClass() != getClass()) return false;
      return comparator.test(type(), other.type());
    }
  }

  non-sealed interface Nat<T extends AyaDocile> extends Inductive<T> {
    @NotNull T makeZero(@NotNull ConDef zero);
    @NotNull T makeSuc(@NotNull ConDef suc, @NotNull T t);
    @NotNull T destruct(int repr);
    int repr();

    /** Untyped: compare the internal representation only */
    default <O extends AyaDocile> boolean compareUntyped(@NotNull Shaped.Nat<O> other) {
      return repr() == other.repr();
    }

    default @Override @NotNull T constructorForm() {
      int repr = repr();
      var zero = ctorRef(CodeShape.GlobalId.ZERO);
      var suc = ctorRef(CodeShape.GlobalId.SUC);
      if (repr == 0) return makeZero(zero.core);
      return makeSuc(suc.core, destruct(repr - 1));
    }

    @NotNull Shaped.Nat<T> map(@NotNull IntUnaryOperator f);
  }

  non-sealed interface List<T extends AyaDocile> extends Inductive<T> {
    @NotNull ImmutableSeq<T> repr();
    @NotNull T makeNil(@NotNull ConDef nil, @NotNull Term type);
    @NotNull T makeCons(@NotNull ConDef cons, @NotNull Term type, T x, T xs);
    @NotNull T destruct(@NotNull ImmutableSeq<T> repr);

    /**
     * Comparing two List
     *
     * @param other      another list
     * @param comparator a comparator that should compare the subterms between two List.
     * @return true if they match (a term matches a pat or two terms are equivalent,
     * which depends on the type parameters {@link T} and {@link O}), false if otherwise.
     */
    default <O extends AyaDocile> boolean compareUntyped(@NotNull Shaped.List<O> other, @NotNull BiPredicate<T, O> comparator) {
      var lhsRepr = repr();
      var rhsRepr = other.repr();
      // the size should equal.
      return lhsRepr.sizeEquals(rhsRepr)
        && lhsRepr.allMatchWith(rhsRepr, comparator);
    }

    @Override default @NotNull T constructorForm() {
      var nil = ctorRef(CodeShape.GlobalId.NIL).core;
      var cons = ctorRef(CodeShape.GlobalId.CONS).core;
      var dataArg = type().args().getFirst(); // Check?
      var elements = repr();
      if (elements.isEmpty()) return makeNil(nil, dataArg);
      return makeCons(cons, dataArg, elements.getFirst(),
        destruct(elements.drop(1)));
    }
  }

  /**
   * Something Shaped which is applicable, like
   * {@link org.aya.syntax.core.def.FnDef}, {@link ConDef}, and probably {@link org.aya.syntax.core.def.DataDef}.
   * See also <code>RuleReducer</code>.
   */
  interface Applicable<T extends AyaDocile, Core extends Def, Concrete extends TeleDecl<?>> extends Shaped<T> {
    /**
     * The underlying ref
     */
    @NotNull DefVar<Core, Concrete> ref();

    /**
     * Applying arguments.
     *
     * @param args arguments
     * @return null if failed
     */
    @Nullable T apply(@NotNull ImmutableSeq<T> args);
  }
}
