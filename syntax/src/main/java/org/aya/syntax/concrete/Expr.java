// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.concrete;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.control.Either;
import kala.value.MutableValue;
import org.aya.generic.AyaDocile;
import org.aya.generic.Nested;
import org.aya.generic.ParamLike;
import org.aya.generic.SortKind;
import org.aya.prettier.BasePrettier;
import org.aya.prettier.ConcretePrettier;
import org.aya.pretty.doc.Doc;
import org.aya.syntax.concrete.stmt.QualifiedID;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.AnyVar;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.BinOpElem;
import org.aya.util.ForLSP;
import org.aya.util.PosedUnaryOperator;
import org.aya.util.error.SourceNode;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.aya.util.prettier.PrettierOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public sealed interface Expr extends AyaDocile {
  @NotNull Expr descent(@NotNull PosedUnaryOperator<@NotNull Expr> f);
  @ForLSP
  sealed interface WithTerm {
    @NotNull MutableValue<Term> theCoreType();
    default @Nullable Term coreType() { return theCoreType().get(); }
  }

  /** Yes, please */
  sealed interface Sugar { }

  @Override
  default @NotNull Doc toDoc(@NotNull PrettierOptions options) {
    return new ConcretePrettier(options).term(BasePrettier.Outer.Free, this);
  }

  record Param(
    @Override @NotNull SourcePos sourcePos,
    @NotNull LocalVar ref,
    @NotNull WithPos<Expr> typeExpr,
    boolean explicit,
    @ForLSP MutableValue<Term> theCoreType
  ) implements SourceNode, AyaDocile, ParamLike<Expr>, WithTerm {
    @Override public @NotNull ParamLike<Expr> map(@NotNull UnaryOperator<Expr> mapper) {
      return new Param(sourcePos, ref, typeExpr.map(mapper), explicit, theCoreType);
    }

    @Override public @NotNull Expr type() { return typeExpr.data(); }

    public Param(@NotNull SourcePos sourcePos, @NotNull LocalVar var, boolean explicit) {
      this(sourcePos, var, new WithPos<>(sourcePos, new Hole(false, null)), explicit);
    }

    public Param(@NotNull SourcePos sourcePos, @NotNull LocalVar ref, @NotNull WithPos<Expr> typeExpr, boolean explicit) {
      this(sourcePos, ref, typeExpr, explicit, MutableValue.create());
    }

    public @NotNull Param update(@NotNull WithPos<Expr> type) {
      return type == typeExpr() ? this : new Param(sourcePos, ref, type, explicit, theCoreType);
    }

    public @NotNull Param descent(@NotNull PosedUnaryOperator<Expr> f) {
      return update(typeExpr.descent(f));
    }
  }

  /**
   * @param filling  the inner expr of goal
   * @param explicit whether the hole is a type-directed programming goal or
   *                 a to-be-solved by tycking hole.
   * @author ice1000
   */
  record Hole(
    boolean explicit,
    @Nullable WithPos<Expr> filling,
    MutableValue<ImmutableSeq<LocalVar>> accessibleLocal
  ) implements Expr {
    public Hole(boolean explicit, @Nullable WithPos<Expr> filling) {
      this(explicit, filling, MutableValue.create());
    }

    public @NotNull Hole update(@Nullable WithPos<Expr> filling) {
      return filling == filling() ? this : new Hole(explicit, filling);
    }

    @Override public @NotNull Hole descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return update(filling == null ? null : filling.descent(f));
    }
  }

  record Error(@NotNull AyaDocile description) implements Expr {
    public Error(@NotNull Doc description) {
      this(_ -> description);
    }

    @Override public @NotNull Expr.Error descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return this;
    }
  }

  /**
   * It is possible that {@code seq.size() == 1}, cause BinOpSeq also represents a scope of operator sequence,
   * for example: the {@code (+)} in {@code f (+)} will be recognized as argument instead of a function call.
   *
   * @param seq
   */
  record BinOpSeq(@NotNull ImmutableSeq<NamedArg> seq) implements Expr, Sugar {
    public @NotNull BinOpSeq update(@NotNull ImmutableSeq<NamedArg> seq) {
      return seq.sameElements(seq(), true) ? this : new BinOpSeq(seq);
    }

    @Override public @NotNull BinOpSeq descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return update(seq.map(arg -> arg.descent(f)));
    }
  }

  record Unresolved(
    @NotNull QualifiedID name
  ) implements Expr {
    public Unresolved(@NotNull SourcePos pos, @NotNull String name) {
      this(new QualifiedID(pos, name));
    }

    @Override public @NotNull Unresolved descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return this;
    }
  }

  record Ref(@NotNull AnyVar var) implements Expr {
    @Override public @NotNull Expr descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) { return this; }
  }

  record Lambda(
    @NotNull LocalVar ref,
    @Override @NotNull WithPos<Expr> body
  ) implements Expr, Nested<Param, Expr, Lambda> {
    // compatibility shit!
    public Lambda(@NotNull Param param, @NotNull WithPos<Expr> body) {
      this(param.ref, body);
      assert param.explicit;
      assert param.type() instanceof Expr.Hole;
    }

    @Override
    public @NotNull Param param() {
      return new Param(ref.definition(), ref, true);
    }

    public @NotNull Lambda update(@NotNull WithPos<Expr> body) {
      return body == body() ? this : new Lambda(ref, body);
    }

    @Override public @NotNull Lambda descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return update(body.descent(f));
    }
  }

  record Tuple(
    @NotNull ImmutableSeq<@NotNull WithPos<Expr>> items
  ) implements Expr {
    public @NotNull Expr.Tuple update(@NotNull ImmutableSeq<@NotNull WithPos<Expr>> items) {
      return items.sameElements(items(), true) ? this : new Tuple(items);
    }

    @Override public @NotNull Expr.Tuple descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return update(items.map(x -> x.descent(f)));
    }
  }

  /**
   * @param resolvedVar will be set to the field's DefVar during resolving
   * @author re-xyr
   */
  record Proj(
    @NotNull WithPos<Expr> tup,
    @NotNull Either<Integer, QualifiedID> ix,
    @Nullable AnyVar resolvedVar,
    @NotNull MutableValue<Term> theCoreType
  ) implements Expr, WithTerm {
    public Proj(
      @NotNull WithPos<Expr> tup,
      @NotNull Either<Integer, QualifiedID> ix
    ) {
      this(tup, ix, null, MutableValue.create());
    }

    public @NotNull Expr.Proj update(@NotNull WithPos<Expr> tup) {
      return tup == tup() ? this : new Proj(tup, ix, resolvedVar, theCoreType);
    }

    @Override public @NotNull Expr.Proj descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return update(tup.descent(f));
    }
  }

  record App(
    @NotNull WithPos<Expr> function,
    @NotNull ImmutableSeq<NamedArg> argument
  ) implements Expr {
    public @NotNull App update(@NotNull WithPos<Expr> function, @NotNull ImmutableSeq<NamedArg> argument) {
      return function == function() && argument.sameElements(argument(), true)
        ? this : new App(function, argument);
    }

    @Override public @NotNull App descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return update(function.descent(f), argument.map(arg -> arg.descent(f)));
    }
  }

  record NamedArg(
    @Override boolean explicit,
    @Nullable String name,
    @NotNull WithPos<Expr> arg
  ) implements SourceNode, BinOpElem<WithPos<Expr>>, AyaDocile {
    public NamedArg(boolean explicit, @NotNull WithPos<Expr> arg) {
      this(explicit, null, arg);
    }

    @Override public @NotNull SourcePos sourcePos() {
      return arg.sourcePos();
    }

    @Override public @NotNull WithPos<Expr> term() {
      return arg;
    }

    public @NotNull NamedArg update(@NotNull WithPos<Expr> expr) {
      return expr == arg ? this : new NamedArg(explicit, name, expr);
    }

    @Override public @NotNull Doc toDoc(@NotNull PrettierOptions options) {
      var doc = name == null ? arg.data().toDoc(options) :
        Doc.braced(Doc.sep(Doc.plain(name), Doc.symbol("=>"), arg.data().toDoc(options)));
      return Doc.bracedUnless(doc, explicit);
    }

    public @NotNull NamedArg descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
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

    @Override public @NotNull Pi descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return update(param.descent(f), last.descent(f));
    }
  }

  record Sigma(
    @NotNull ImmutableSeq<@NotNull Param> params
  ) implements Expr {
    public @NotNull Sigma update(@NotNull ImmutableSeq<@NotNull Param> params) {
      return params.sameElements(params(), true) ? this : new Sigma(params);
    }

    @Override public @NotNull Sigma descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return update(params.map(param -> param.descent(f)));
    }
  }

  record RawSort(@NotNull SortKind kind) implements Expr, Sugar {
    @Override public @NotNull RawSort descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return this;
    }
  }

  sealed interface Sort extends Expr {
    int lift();

    SortKind kind();

    @Override default @NotNull Sort descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
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

  record Lift(@NotNull WithPos<Expr> expr, int lift) implements Expr {
    public @NotNull Lift update(@NotNull WithPos<Expr> expr) {
      return expr == expr() ? this : new Lift(expr, lift);
    }

    @Override public @NotNull Lift descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return update(expr.descent(f));
    }
  }

  record LitInt(int integer) implements Expr {
    @Override public @NotNull LitInt descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return this;
    }
  }

  record LitString(@NotNull String string) implements Expr {
    @Override public @NotNull LitString descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return this;
    }
  }

  record Idiom(
    @NotNull IdiomNames names,
    @NotNull ImmutableSeq<WithPos<Expr>> barredApps
  ) implements Expr, Sugar {
    public @NotNull Idiom update(@NotNull IdiomNames names, @NotNull ImmutableSeq<WithPos<Expr>> barredApps) {
      return names.identical(names()) && barredApps.sameElements(barredApps(), true) ? this
        : new Idiom(names, barredApps);
    }

    @Override public @NotNull Idiom descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return update(names.fmap(x -> f.apply(SourcePos.NONE, x)), barredApps.map(x -> x.descent(f)));
    }
  }

  record IdiomNames(
    @NotNull Expr alternativeEmpty,
    @NotNull Expr alternativeOr,
    @NotNull Expr applicativeAp,
    @NotNull Expr applicativePure
  ) {
    public IdiomNames fmap(@NotNull Function<Expr, Expr> f) {
      return new IdiomNames(
        f.apply(alternativeEmpty),
        f.apply(alternativeOr),
        f.apply(applicativeAp),
        f.apply(applicativePure));
    }

    public boolean identical(@NotNull IdiomNames names) {
      return alternativeEmpty == names.alternativeEmpty
        && alternativeOr == names.alternativeOr
        && applicativeAp == names.applicativeAp
        && applicativePure == names.applicativePure;
    }
  }

  record Do(
    @NotNull Expr bindName,   // TODO: perhaps we don't need the source pos of (>>=)
    @NotNull ImmutableSeq<DoBind> binds
  ) implements Expr, Sugar {
    public @NotNull Do update(@NotNull Expr bindName, @NotNull ImmutableSeq<DoBind> binds) {
      return bindName == bindName() && binds.sameElements(binds(), true) ? this
        : new Do(bindName, binds);
    }

    @Override public @NotNull Do descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return update(f.apply(SourcePos.NONE, bindName), binds.map(bind -> bind.descent(f)));
    }
  }

  record DoBind(
    @NotNull SourcePos sourcePos,
    @NotNull LocalVar var,
    @NotNull WithPos<Expr> expr
  ) implements SourceNode {
    public @NotNull DoBind update(@NotNull WithPos<Expr> expr) {
      return expr == expr() ? this : new DoBind(sourcePos, var, expr);
    }

    public @NotNull DoBind descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return update(expr.descent(f));
    }
  }

  /**
   * <h1>Array Expr</h1>
   *
   * @param arrayBlock <code>[ x | x <- [ 1, 2, 3 ] ]</code> (left) or <code>[ 1, 2, 3 ]</code> (right)
   * @apiNote empty array <code>[]</code> should be a right (an empty expr seq)
   */
  record Array(
    @NotNull Either<CompBlock, ElementList> arrayBlock
  ) implements Expr {
    public @NotNull Array update(@NotNull Either<CompBlock, ElementList> arrayBlock) {
      var equal = arrayBlock.bifold(this.arrayBlock, false,
        (newOne, oldOne) -> newOne == oldOne,
        (newOne, oldOne) -> newOne == oldOne);

      return equal ? this : new Array(arrayBlock);
    }

    @Override public @NotNull Array descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return update(arrayBlock.map(comp -> comp.descent(f), list -> list.descent(f)));
    }

    public record ElementList(@NotNull ImmutableSeq<WithPos<Expr>> exprList) {
      public @NotNull ElementList update(@NotNull ImmutableSeq<WithPos<Expr>> exprList) {
        return exprList.sameElements(exprList(), true) ? this : new ElementList(exprList);
      }

      public @NotNull ElementList descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
        return update(exprList.map(x -> x.descent(f)));
      }
    }

    public record ListCompNames(
      @NotNull Expr monadBind,
      @NotNull Expr functorPure
    ) {
      public ListCompNames fmap(@NotNull Function<Expr, Expr> f) {
        return new ListCompNames(f.apply(monadBind), f.apply(functorPure));
      }

      public boolean identical(@NotNull ListCompNames names) {
        return monadBind == names.monadBind && functorPure == names.functorPure;
      }
    }

    /**
     * <h1>Array Comp(?)</h1>
     * <p>
     * The (half?) primary part of {@link Array}<br/>
     * For example: {@code [x * y | x <- [1, 2, 3], y <- [4, 5, 6]]}
     *
     * @param generator {@code x * y} part above
     * @param binds     {@code x <- [1, 2, 3], y <- [4, 5, 6]} part above
     * @param names     the bind ({@code >>=}) function, it is {@link org.aya.generic.Constants#monadBind} in default,
     *                  the pure ({@code return}) function, it is {@link org.aya.generic.Constants#functorPure} in default
     * @apiNote a ArrayCompBlock will be desugar to a do-block. For the example above,
     * it will be desugared to {@code do x <- [1, 2, 3], y <- [4, 5, 6], return x * y}
     */
    public record CompBlock(
      @NotNull WithPos<Expr> generator,
      @NotNull ImmutableSeq<DoBind> binds,
      @NotNull ListCompNames names
    ) {
      public @NotNull CompBlock update(@NotNull WithPos<Expr> generator, @NotNull ImmutableSeq<DoBind> binds, @NotNull ListCompNames names) {
        return generator == generator() && binds.sameElements(binds(), true) && names.identical(names())
          ? this
          : new CompBlock(generator, binds, names);
      }

      public @NotNull CompBlock descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
        return update(generator.descent(f), binds.map(bind -> bind.descent(f)), names.fmap(x -> f.apply(SourcePos.NONE, x)));
      }
    }

    /**
     * helper constructor, also find constructor calls easily in IDE
     */
    public static Expr.Array newList(
      @NotNull ImmutableSeq<WithPos<Expr>> exprs
    ) {
      return new Expr.Array(
        Either.right(new ElementList(exprs))
      );
    }

    public static Expr.Array newGenerator(
      @NotNull WithPos<Expr> generator,
      @NotNull ImmutableSeq<DoBind> bindings,
      @NotNull ListCompNames names
    ) {
      return new Expr.Array(
        Either.left(new CompBlock(generator, bindings, names))
      );
    }
  }

  /**
   * <h1>Let Expression</h1>
   *
   * <pre>
   *   let
   *     f (x : X) : G := g
   *   in expr
   * </pre>
   * <p>
   * where:
   * <ul>
   *   <li>{@link LetBind#bindName} = f</li>
   *   <li>{@link LetBind#telescope} = (x : X)</li>
   *   <li>{@link LetBind#result} = G</li>
   *   <li>{@link LetBind#definedAs} = g</li>
   *   <li>{@link Let#body} = expr</li>
   * </ul>
   */
  record Let(
    @NotNull LetBind bind,
    @NotNull WithPos<Expr> body
  ) implements Expr, Nested<LetBind, Expr, Let> {
    public @NotNull Let update(@NotNull LetBind bind, @NotNull WithPos<Expr> body) {
      return bind() == bind && body() == body
        ? this
        : new Let(bind, body);
    }

    @Override
    public @NotNull Expr descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return update(bind().descent(f), body.descent(f));
    }

    @Override
    public @NotNull LetBind param() {
      return bind;
    }
  }

  record LetBind(
    @NotNull SourcePos sourcePos,
    @NotNull LocalVar bindName,
    @NotNull ImmutableSeq<Expr.Param> telescope,
    @NotNull WithPos<Expr> result,
    @NotNull WithPos<Expr> definedAs
  ) implements SourceNode {
    public @NotNull LetBind update(@NotNull ImmutableSeq<Expr.Param> telescope, @NotNull WithPos<Expr> result, @NotNull WithPos<Expr> definedAs) {
      return telescope().sameElements(telescope, true) && result() == result && definedAs() == definedAs
        ? this
        : new LetBind(sourcePos, bindName, telescope, result, definedAs);
    }

    public @NotNull LetBind descent(@NotNull PosedUnaryOperator<@NotNull Expr> f) {
      return update(telescope().map(x -> x.descent(f)), result.descent(f), definedAs.descent(f));
    }
  }

  static @NotNull WithPos<Expr> buildPi(@NotNull SourcePos sourcePos, @NotNull SeqView<Param> params, @NotNull WithPos<Expr> body) {
    return buildNested(sourcePos, params, body, Pi::new);
  }

  static @NotNull WithPos<Expr> buildLam(@NotNull SourcePos sourcePos, @NotNull SeqView<Param> params, @NotNull WithPos<Expr> body) {
    return buildNested(sourcePos, params, body, Lambda::new);
  }

  static @NotNull WithPos<Expr> buildLet(@NotNull SourcePos sourcePos, @NotNull SeqView<LetBind> binds, @NotNull WithPos<Expr> body) {
    return buildNested(sourcePos, binds, body, Let::new);
  }

  /** convert flattened terms into nested right-associate terms */
  static <P extends SourceNode> @NotNull WithPos<Expr> buildNested(
    @NotNull SourcePos sourcePos,
    @NotNull SeqView<P> params,
    @NotNull WithPos<Expr> body,
    @NotNull BiFunction<P, WithPos<Expr>, Expr> constructor
  ) {
    if (params.isEmpty()) return body;
    var drop = params.drop(1);
    var subPos = body.sourcePos().sourcePosForSubExpr(sourcePos.file(),
      drop.map(SourceNode::sourcePos));
    return new WithPos<>(sourcePos, constructor.apply(params.getFirst(),
      buildNested(subPos, drop, body, constructor)));
  }
}
