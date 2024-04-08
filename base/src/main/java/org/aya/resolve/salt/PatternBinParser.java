// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.resolve.salt;

import kala.collection.SeqView;
import org.aya.resolve.ResolveInfo;
import org.aya.resolve.context.Context;
import org.aya.resolve.error.OperatorError;
import org.aya.syntax.concrete.Pattern;
import org.aya.syntax.ref.DefVar;
import org.aya.syntax.ref.LocalVar;
import org.aya.tyck.error.PatternProblem;
import org.aya.util.Arg;
import org.aya.util.binop.Assoc;
import org.aya.util.binop.BinOpParser;
import org.aya.util.binop.BinOpSet;
import org.aya.util.binop.OpDecl;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class PatternBinParser extends BinOpParser<AyaBinOpSet, WithPos<Pattern>, Arg<WithPos<Pattern>>> {
  private final @NotNull ResolveInfo resolveInfo;

  public PatternBinParser(@NotNull ResolveInfo resolveInfo, @NotNull SeqView<@NotNull Arg<WithPos<Pattern>>> seq) {
    super(resolveInfo.opSet(), seq);
    this.resolveInfo = resolveInfo;
  }

  @Override protected @NotNull BinOpParser<AyaBinOpSet, WithPos<Pattern>, Arg<WithPos<Pattern>>>
  replicate(@NotNull SeqView<@NotNull Arg<WithPos<Pattern>>> seq) {
    return new PatternBinParser(resolveInfo, seq);
  }

  private static final Arg<WithPos<Pattern>> OP_APP = new Arg<>(new WithPos<>(SourcePos.NONE,
    new Pattern.Bind(new LocalVar(BinOpSet.APP_ELEM.name()))), true);

  @Override protected @NotNull Arg<WithPos<Pattern>> appOp() {
    return OP_APP;
  }

  @Override public @NotNull Arg<WithPos<Pattern>>
  makeSectionApp(@NotNull SourcePos pos, @NotNull Arg<WithPos<Pattern>> op, @NotNull Function<Arg<WithPos<Pattern>>, WithPos<Pattern>> lamBody) {
    return new Arg<>(createErrorExpr(pos), op.explicit());
  }

  @Override protected void reportAmbiguousPred(String op1, String op2, SourcePos pos) {
    opSet.reporter.report(new OperatorError.Precedence(op1, op2, pos));
  }

  @Override protected void reportFixityError(Assoc top, Assoc current, String topOp, String currentOp, SourcePos pos) {
    opSet.reporter.report(new OperatorError.Fixity(currentOp, current, topOp, top, pos));
  }

  @Override protected void reportMissingOperand(String op, SourcePos pos) {
    opSet.reporter.report(new OperatorError.MissingOperand(pos, op));
  }

  @Override protected @NotNull WithPos<Pattern> createErrorExpr(@NotNull SourcePos sourcePos) {
    return new WithPos<>(sourcePos, new Pattern.Bind(new LocalVar("a broken constructor pattern")));
  }

  @Override protected @Nullable OpDecl underlyingOpDecl(@NotNull Arg<WithPos<Pattern>> elem) {
    return elem.term().data() instanceof Pattern.Ctor ref && ref.resolved().data() instanceof DefVar<?, ?> defVar
      ? defVar.resolveOpDecl(resolveInfo.thisModule().modulePath())
      : null;
  }

  @Override protected @NotNull Arg<WithPos<Pattern>>
  makeArg(@NotNull SourcePos pos, @NotNull WithPos<Pattern> func, @NotNull Arg<WithPos<Pattern>> arg, boolean explicit) {
    // param explicit should be ignored since the BinOpSeq we are processing already specified the explicitness
    if (func.data() instanceof Pattern.Ctor ctor) {
      var newCtor = new Pattern.Ctor(ctor.resolved(), ctor.params().appended(new Arg<>(arg.term(), arg.explicit())));
      return new Arg<>(new WithPos<>(pos, newCtor), explicit);
    } else {
      opSet.reporter.report(new PatternProblem.UnknownCtor(func));
      throw new Context.ResolvingInterruptedException();
    }
  }
}
