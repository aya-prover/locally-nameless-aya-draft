// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck.error;

import kala.collection.immutable.ImmutableSeq;
import org.aya.pretty.doc.Doc;
import org.aya.syntax.core.term.call.MetaCall;
import org.aya.syntax.ref.LocalVar;
import org.aya.tyck.TyckState;
import org.aya.util.error.SourcePos;
import org.aya.util.prettier.PrettierOptions;
import org.aya.util.reporter.Problem;
import org.jetbrains.annotations.NotNull;

public record Goal(
  @NotNull TyckState state, @NotNull MetaCall hole,
  @NotNull ImmutableSeq<LocalVar> scope
) implements Problem {
  @Override public @NotNull Doc describe(@NotNull PrettierOptions options) {
    var meta = hole.ref();
    return Doc.plain("TODO: meta pretty print");
    /*var nullableResult = meta.req().result();
    var result = nullableResult != null ? nullableResult*//*.freezeHoles(state)*//*
      : new ErrorTerm(Doc.plain("???"), false);
    var doc = Doc.vcatNonEmpty(
      Doc.english("Goal of type"),
      Doc.par(1, result.toDoc(options)),
      Doc.par(1, Doc.parened(Doc.sep(Doc.plain("WHNF:"), new Normalizer(state).whnf(result).toDoc(options)))),
      Doc.plain("Context:"),
      Doc.vcat(meta.fullTelescope().map(param -> {
        param = new Term.Param(param, param.type().freezeHoles(state));
        var paramDoc = param.toDoc(options);
        return Doc.par(1, scope.contains(param.ref()) ? paramDoc : Doc.sep(paramDoc, Doc.parened(Doc.english("not in scope"))));
      })),
      meta.conditions.isNotEmpty() ? Doc.vcat(
        ImmutableSeq.of(Doc.plain("To ensure confluence:"))
          .concat(meta.conditions.toImmutableSeq().map(tup -> Doc.par(1, Doc.cat(
            Doc.plain("Given "),
            Doc.parened(tup.component1().toDoc(options)),
            Doc.plain(", we should have: "),
            tup.component2().freezeHoles(state).toDoc(options)
          )))))
        : Doc.empty()
    );
    var metas = state.metas();
    return !metas.containsKey(meta) ? doc :
      Doc.vcat(Doc.plain("Candidate exists:"), Doc.par(1, metas.get(meta).toDoc(options)), doc);
  */
  }

  @Override public @NotNull SourcePos sourcePos() {
    return hole.ref().pos();
  }
  @Override public @NotNull Severity level() { return Severity.GOAL; }
  @Override public @NotNull Stage stage() { return Stage.TYCK; }
}
