// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.error;

/** @author ice1000 */
/*
public sealed interface HoleProblem extends Problem {
  @NotNull MetaTerm term();

  @Override default @NotNull Severity level() {
    return Severity.ERROR;
  }

  @Override default @NotNull SourcePos sourcePos() {
    return term().ref().sourcePos;
  }

  */
/** @author ice1000 *//*

  record BadSpineError(
    @Override @NotNull MetaTerm term
  ) implements HoleProblem {
    @Override public @NotNull Doc describe(@NotNull PrettierOptions options) {
      return Doc.vcat(
        Doc.english("Can't perform pattern unification on hole with the following spine:"),
        BasePrettier.argsDoc(options, term.args())
      );
    }
  }

  record IllTypedError(
    @Override @NotNull MetaTerm term,
    @NotNull TyckState state,
    @Override @NotNull Term solution
  ) implements HoleProblem {
    @Override public @NotNull Doc describe(@NotNull PrettierOptions options) {
      var list = MutableList.of(Doc.english("The meta (denoted ? below) is supposed to satisfy:"),
        Doc.par(1, term.ref().info.toDoc(options)),
        Doc.english("However, the solution below does not seem so:"));
      UnifyInfo.exprInfo(solution, options, state, list);
      return Doc.vcat(list);
    }
  }

  record BadlyScopedError(
    @Override @NotNull MetaTerm term,
    @NotNull Term solved,
    @NotNull Seq<LocalVar> scopeCheck
  ) implements HoleProblem {
    @Override public @NotNull Doc describe(@NotNull PrettierOptions options) {
      return Doc.vcat(
        Doc.english("The solution"),
        Doc.par(1, solved.toDoc(options)),
        Doc.plain("is not well-scoped"),
        Doc.cat(Doc.english("In particular, these variables are not in scope:"),
          Doc.ONE_WS,
          Doc.commaList(scopeCheck.view()
            .map(BasePrettier::varDoc)
            .map(Doc::code))));
    }
  }

  */
/**
   * @author ice1000
   *//*

  record RecursionError(
    @Override @NotNull MetaTerm term,
    @NotNull Term sol
  ) implements HoleProblem {
    @Override public @NotNull Doc describe(@NotNull PrettierOptions options) {
      return Doc.vcat(
        Doc.sep(
          Doc.english("Trying to solve hole"),
          Doc.code(BasePrettier.linkDef(term.ref())),
          Doc.plain("as")),
        Doc.par(1, sol.toDoc(options)),
        Doc.english("which is recursive"));
    }
  }

  record CannotFindGeneralSolution(
    @NotNull ImmutableSeq<TyckState.Eqn> eqns
  ) implements Problem {
    @Override public @NotNull SourcePos sourcePos() {
      return eqns.getLast().pos();
    }

    @Override public @NotNull SeqView<WithPos<Doc>> inlineHints(@NotNull PrettierOptions options) {
      return eqns.view().map(eqn -> new WithPos<>(eqn.pos(), eqn.toDoc(options)));
    }

    @Override public @NotNull Doc describe(@NotNull PrettierOptions options) {
      return Doc.english("Solving equation(s) with not very general solution(s)");
    }

    @Override public @NotNull Severity level() {
      return Severity.INFO;
    }
  }
}
*/
