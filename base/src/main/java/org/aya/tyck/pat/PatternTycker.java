// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.pat;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.immutable.primitive.ImmutableIntSeq;
import kala.collection.mutable.MutableList;
import kala.control.Result;
import kala.range.primitive.IntRange;
import kala.value.MutableValue;
import org.aya.generic.Constants;
import org.aya.syntax.concrete.Expr;
import org.aya.syntax.concrete.Pattern;
import org.aya.syntax.core.def.ConDef;
import org.aya.syntax.core.def.TeleDef;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.core.pat.PatToTerm;
import org.aya.syntax.core.term.MetaPatTerm;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.SigmaTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.ConCallLike;
import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.ref.AnyVar;
import org.aya.syntax.ref.LocalVar;
import org.aya.tyck.ExprTycker;
import org.aya.tyck.Jdg;
import org.aya.tyck.TyckState;
import org.aya.tyck.ctx.LocalSubstitution;
import org.aya.tyck.error.PatternProblem;
import org.aya.tyck.error.TyckOrderError;
import org.aya.tyck.tycker.Problematic;
import org.aya.tyck.tycker.Stateful;
import org.aya.util.Arg;
import org.aya.util.error.Panic;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.aya.util.reporter.Problem;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Tyck for {@link Pattern}'s, the left hand side of one clause.
 */
public class PatternTycker implements Problematic, Stateful {
  private final @NotNull ExprTycker exprTycker;

  private final ImmutableIntSeq indices;

  /**
   * A bound telescope (i.e. all the reference to the former parameter are LocalTerm)
   */
  private @NotNull SeqView<Param> telescope;

  /** Substitution for parameter, in the same order as parameter */
  private final @NotNull MutableList<Jdg> paramSubst;

  /**
   * Substitution for `as` pattern
   */
  private final @NotNull LocalSubstitution asSubst;

  private @UnknownNullability Param currentParam = null;
  private boolean hasError = false;

  public PatternTycker(@NotNull ExprTycker tycker, @NotNull SeqView<Param> tele, @NotNull LocalSubstitution sub) {
    this(tycker, IntRange.closedOpen(0, tele.size()).collect(ImmutableIntSeq.factory()), tele, sub);
  }

  public PatternTycker(
    @NotNull ExprTycker exprTycker,
    @NotNull ImmutableIntSeq indices,
    @NotNull SeqView<Param> telescope,
    @NotNull LocalSubstitution asSubst
  ) {
    this.exprTycker = exprTycker;
    this.indices = indices;
    this.telescope = telescope;
    this.paramSubst = MutableList.create();
    this.asSubst = asSubst;
  }

  public record TyckResult(
    @NotNull ImmutableSeq<Pat> wellTyped,
    @NotNull ImmutableSeq<Jdg> paramSubst,
    @NotNull LocalSubstitution asSubst,
    @Nullable WithPos<Expr> newBody,
    boolean hasError
  ) {
    public @NotNull SeqView<Term> paramSubstObj() {
      return paramSubst.view().map(Jdg::wellTyped);
    }
  }

  /**
   * Tyck a {@param type} against {@param type}
   *
   * @param pattern a concrete {@link Pattern}
   * @param type    the type of {@param pattern}, it probably contains {@link MetaPatTerm}
   * @return a well-typed {@link Pat}, but still need to be inline!
   */
  private @NotNull Pat doTyck(@NotNull WithPos<Pattern> pattern, @NotNull Term type) {
    return switch (pattern.data()) {
      case Pattern.Absurd _ -> {
        var selection = selectCon(type, null, pattern);
        if (selection != null) {
          foundError(new PatternProblem.PossiblePat(pattern, selection.conHead));
        }
        yield Pat.Absurd.INSTANCE;
      }
      case Pattern.Tuple tuple -> {
        if (!(exprTycker.whnf(type) instanceof SigmaTerm sigma)) {
          var frozen = freezeHoles(type);
          yield withError(new PatternProblem.TupleNonSig(pattern.replace(tuple), frozen), frozen);
        }
        yield new Pat.Tuple(tyckInner(
          generateNames(sigma.params()),
          tuple.patterns().view().map(Arg::ofExplicitly),
          pattern
        ));
      }
      case Pattern.Con con -> {
        var var = con.resolved().data();
        var realCon = selectCon(type, var, pattern);
        if (realCon == null) yield randomPat(type);
        var conRef = realCon.conHead.ref();
        var conCore = conRef.core;

        // It is possible that `con.params()` is empty.
        var patterns = tyckInner(conCore.selfTele.view(), con.params().view(), pattern);

        // check if this Con is a ShapedCon
        // var typeRecog = exprTycker.shapeFacony.find(conRef.core.dataRef.core).getOrNull();

        yield new Pat.Con(realCon.conHead.ref(), patterns, /* typeRecog */ realCon.data());
      }
      case Pattern.Bind(var bind, var tyRef) -> {
        exprTycker.localCtx().put(bind, type);
        tyRef.set(type);
        yield new Pat.Bind(bind, type);
      }
      case Pattern.CalmFace.INSTANCE ->
        new Pat.Meta(MutableValue.create(), Constants.ANONYMOUS_PREFIX, type, pattern.sourcePos());
      case Pattern.Number(var number) -> {
        var ty = whnf(type);
        throw new UnsupportedOperationException("TODO");
        // if (ty instanceof DataCall dataCall) {
        //   var data = dataCall.ref().core;
        //   var shape = exprTycker.shapeFacony.find(data);
        //   if (shape.isDefined() && shape.get().shape() == AyaShape.NAT_SHAPE)
        //     yield new Pat.ShapedInt(number, shape.get(), dataCall);
        // }
        // yield withError(new PatternProblem.BadLitPattern(pattern, term), term);
      }
      case Pattern.List(var el) -> {
        // desugar `Pattern.List` to `Pattern.Con` here, but use `CodeShape` !
        // Note: this is a special case (maybe), If there is another similar requirement,
        //       a PatternDesugarer is recommended.

        throw new UnsupportedOperationException("TODO");

        // var ty = term.normalize(exprTycker.state, NormalizeMode.WHNF);
        // if (ty instanceof DataCall dataCall) {
        //   var data = dataCall.ref().core;
        //   var shape = exprTycker.shapeFacony.find(data);
        //   if (shape.isDefined() && shape.get().shape() == AyaShape.LIST_SHAPE)
        //     yield doTyck(new Pattern.FakeShapedList(pos, el, shape.get(), dataCall)
        //       .constructorForm(), term);
        // }
        // yield withError(new PatternProblem.BadLitPattern(pattern, term), term);
      }
      case Pattern.As(var inner, var as, var typeRef) -> {
        var innerPat = doTyck(inner, type);

        typeRef.set(type);
        addAsSubst(as, innerPat, type);
        // exprTycker.localCtx().put(as, type);

        yield innerPat;
      }
      case Pattern.Salt _ -> Panic.unreachable();
    };
  }

  private void moveNext() { currentParam = telescope.getFirstOrNull(); }

  /**
   * Find the next param against to {@param pattern}
   *
   * @return null if failed, i.e. too many pattern
   * @apiNote after call: {@param currentParam} is an unchecked parameter, and {@code currentParam.explicit == pattern.explicit}
   */
  private @Nullable ImmutableSeq<Pat> nextParam(@NotNull Arg<WithPos<Pattern>> pattern) {
    var generatedPats = MutableList.<Pat>create();

    while (currentParam != null && pattern.explicit() != currentParam.explicit()) {
      // Hwhile : pattern.explicit != currentParam.explicit
      if (pattern.explicit()) {
        // Hif : pattern.explicit = true
        // Corollary : currentParam.explicit = false

        // then generate pattern
        generatedPats.append(generatePattern());
        // [generatePattern] drops the first parameter
        moveNext();
      } else {
        // Hif = pattern.explicit = false
        // Corollary : currentParam.explicit = true
        // too many implicit pattern!
        foundError(new PatternProblem.TooManyImplicitPattern(pattern.term(), currentParam));
        return null;
      }
    }

    // Hwhile : currentParam == null || pattern.explicit = currentParam.explicit

    if (currentParam == null) {
      // too many pattern
      foundError(new PatternProblem.TooManyPattern(pattern.term()));
      return null;
    }

    // Hwhile : pattern.explicit = currentParam.explicit
    // good, this is the parameter we want!
    return generatedPats.toImmutableSeq();
  }

  record PushTelescope(@NotNull ImmutableSeq<Pat> wellTyped, @NotNull WithPos<Expr> newBody) { }

  /**
   * @apiNote requires: {@code currentParam} is not null and is an unchecked parameter, say, no well typed pat for it.
   * after call: {@code currentParam} is an unchecked parameter if not null
   * @implNote No need to report if too many parameter
   */
  private @NotNull PushTelescope pushTelescope(@NotNull WithPos<Expr> body) {
    var wellTyped = MutableList.<Pat>create();

    while (currentParam != null && body.data() instanceof Expr.Lambda lam) {
      // good, we can use the parameter of [lam] as pattern
      var pat = new Pattern.Bind(lam.param().ref());
      wellTyped.append(tyckPattern(body.replace(pat)));

      // update state
      body = lam.body();
      moveNext();
    }

    // Hwhile : currentParam = null || body is not Expr.Lambda
    return new PushTelescope(wellTyped.toImmutableSeq(), body);
  }

  public @NotNull TyckResult tyck(
    @NotNull SeqView<Arg<WithPos<Pattern>>> patterns,
    @Nullable WithPos<Pattern> outerPattern,
    @Nullable WithPos<Expr> body
  ) {
    assert currentParam == null : "this tycker is dirty";
    var wellTyped = MutableList.<Pat>create();
    // last user given pattern, that is, not aya generated
    @Nullable Arg<WithPos<Pattern>> lastPat = null;

    moveNext();

    // loop invariant: currentParam is the last unchecked parameter if not null
    while (currentParam != null && patterns.isNotEmpty()) {
      var currentPat = patterns.getFirst();
      patterns = patterns.drop(1);
      lastPat = currentPat;

      // find the next appropriate parameter
      var generated = nextParam(currentPat);
      if (generated == null) {
        return done(wellTyped, body);
      }

      wellTyped.appendAll(generated);
      wellTyped.append(tyckPattern(currentPat.term()));
      moveNext();
    }

    // [currentParam] is the next unchecked parameter if not null (by loop invariant)

    if (body != null) {
      var result = pushTelescope(body);
      wellTyped.appendAll(result.wellTyped);
      body = result.newBody;
    }

    // [currentParam] is the next unchecked parameter if not null

    if (currentParam != null) {
      // too few patterns !
      // the body does not have pattern, too sad
      WithPos<Pattern> errorPattern = lastPat == null
        ? Objects.requireNonNull(outerPattern)
        : lastPat.term();
      foundError(new PatternProblem.InsufficientPattern(errorPattern, currentParam));
      return done(wellTyped, body);
    }

    // [currentParam] = null

    return done(wellTyped, body);
  }

  private <T> T onTyck(@NotNull Supplier<T> block) {
    currentParam = currentParam.descent(t -> t.instantiateTele(paramSubst.view().map(Jdg::wellTyped)));
    var result = block.get();
    telescope = telescope.drop(1);
    return result;
  }

  /**
   * Checking {@param pattern} with {@link PatternTycker#currentParam}
   */
  private @NotNull Pat tyckPattern(@NotNull WithPos<Pattern> pattern) {
    return onTyck(() -> {
      var result = doTyck(pattern, currentParam.type());
      addArgSubst(result, currentParam.type());
      return result;
    });
  }

  /**
   * For every implicit parameter which is not explicitly (not user given pattern) matched,
   * we generate a MetaPat for each,
   * so that they can be inferred during {@link org.aya.tyck.pat.ClauseTycker}
   */
  private @NotNull Pat generatePattern() {
    return onTyck(() -> {
      var type = currentParam.type();
      Pat pat;
      var freshName = currentParam.name();
      if (exprTycker.whnf(type) instanceof DataCall dataCall) {
        // this pattern would be a Con, it can be inferred
        // TODO: I NEED A SOURCE POS!!
        pat = new Pat.Meta(MutableValue.create(), freshName, dataCall, SourcePos.NONE);
      } else {
        var freshVar = new LocalVar(freshName);
        // If the type is not a DataCall, then the only available pattern is Pat.Bind
        pat = new Pat.Bind(freshVar, type);
        exprTycker.localCtx().put(freshVar, type);
      }

      addArgSubst(pat, currentParam.type());
      return pat;
    });
  }

  private @NotNull ImmutableSeq<Pat> tyckInner(
    @NotNull SeqView<Param> telescope,
    @NotNull SeqView<Arg<WithPos<Pattern>>> patterns,
    @NotNull WithPos<Pattern> outerPattern
  ) {
    //noinspection ViewSize
    var sub = new PatternTycker(exprTycker, telescope, asSubst);
    var tyckResult = sub.tyck(patterns, outerPattern, null);

    hasError = hasError || sub.hasError;
    return tyckResult.wellTyped;
  }

  private void addArgSubst(@NotNull Pat pattern, @NotNull Term type) {
    paramSubst.append(new Jdg.Default(PatToTerm.visit(pattern), type));
  }

  private void addAsSubst(@NotNull LocalVar as, @NotNull Pat pattern, @NotNull Term type) {
    asSubst.put(as, new Jdg.Default(PatToTerm.visit(pattern), type));
  }

  private @NotNull TyckResult done(@NotNull MutableList<Pat> wellTyped, @Nullable WithPos<Expr> newBody) {
    var paramSubst = this.paramSubst.toImmutableSeq();

    return new TyckResult(
      wellTyped.toImmutableSeq(),
      paramSubst,
      asSubst,
      newBody,
      hasError
    );
  }

  private record Selection(DataCall data, ImmutableSeq<Term> args, ConCallLike.Head conHead) { }

  /**
   * @param name if null, the selection will be performed on all constructors
   * @return null means selection failed
   */
  private @Nullable Selection selectCon(Term type, @Nullable AnyVar name, @NotNull WithPos<Pattern> pattern) {
    if (!(exprTycker.whnf(type) instanceof DataCall dataCall)) {
      foundError(new PatternProblem.SplittingOnNonData(pattern, type));
      return null;
    }

    var dataRef = dataCall.ref();
    var core = dataRef.core;
    if (core == null && name == null) {
      // We are checking an absurd pattern, but the data is not yet fully checked
      throw TyckOrderError.notYetTycked(dataRef);
    }

    var body = TeleDef.dataBody(dataRef);
    for (var con : body) {
      if (name != null && con.ref() != name) continue;
      var matchy = isConAvailable(dataCall, con, exprTycker.state);
      if (matchy.isOk()) {
        return new Selection(dataCall, matchy.get(), dataCall.conHead(con.ref()));
      }
      // For absurd pattern, we look at the next constructor
      if (name == null) {
        // Is blocked
        if (matchy.getErr()) {
          foundError(new PatternProblem.BlockedEval(pattern, dataCall));
          return null;
        }
        continue;
      }
      // Since we cannot have two constructors of the same name,
      // if the name-matching constructor mismatches the type,
      // we get an error.
      foundError(new PatternProblem.UnavailableCon(pattern, dataCall));
      return null;
    }
    // Here, name != null, and is not in the list of checked body
    if (core == null) {
      throw new UnsupportedOperationException("TODO");
      // foundError(new TyckOrderError.NotYetTyckedError(pos.sourcePos(), name));
      // return null;
    }
    if (name != null) foundError(new PatternProblem.UnknownCon(pattern));
    return null;
  }

  /**
   * Check whether {@param con} is available under {@param type}
   */
  private static @NotNull Result<ImmutableSeq<Term>, Boolean> isConAvailable(
    @NotNull DataCall type,
    @NotNull ConDef con,
    @NotNull TyckState state
  ) {
    // TODO: con pattern
    return Result.ok(type.args());
  }

  /// region Helper

  private @NotNull Pat randomPat(Term param) {
    return new Pat.Bind(new LocalVar("?"), param);
  }

  /**
   * Generate names for core telescope
   */
  private static @NotNull SeqView<Param> generateNames(@NotNull ImmutableSeq<Term> telescope) {
    return telescope.view().mapIndexed((i, t) -> {
      // TODO: add type to generated name
      return new Param(STR."\{Constants.ANONYMOUS_PREFIX}\{i}", t, true);
    });
  }

  /// endregion Helper

  /// region Error Reporting

  @Override public @NotNull Reporter reporter() { return exprTycker.reporter; }
  @Override public @NotNull TyckState state() { return exprTycker.state; }

  private @NotNull Pat withError(Problem problem, Term param) {
    foundError(problem);
    // In case something's wrong, produce a random pattern
    return randomPat(param);
  }

  private void foundError(@Nullable Problem problem) {
    hasError = true;
    if (problem != null) fail(problem);
  }

  /// endregion Error Reporting
}
