// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.normalize;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import kala.control.Result;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.TupTerm;
import org.aya.syntax.core.term.call.ConCallLike;
import org.aya.syntax.ref.LocalCtx;
import org.aya.util.error.InternalException;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

/**
 * @param inferMeta whether infer the PatMetaTerm
 */
public record PatternMatcher(boolean inferMeta, @NotNull UnaryOperator<Term> pre) {
  /**
   * Match {@param term} against to {@param pat}
   *
   * @return a substitution of corresponding bindings of {@param pat}.
   * The binding order is the same as {@link Pat#storeBindings(LocalCtx, UnaryOperator)}
   */
  public @NotNull Result<ImmutableSeq<Term>, Boolean> match(@NotNull Pat pat, @NotNull Term term) {
    return switch (pat) {
      case Pat.Absurd _ -> throw new InternalException("unreachable");
      case Pat.Bind _ -> Result.ok(ImmutableSeq.of(term));
      case Pat.Ctor ctor -> {
        term = pre.apply(term);
        yield switch (term) {
          case ConCallLike kon -> {
            if (ctor.ref() != kon.ref()) yield Result.err(false);
            yield matchMany(ctor.args(), kon.conArgs());    // arguments for data should not be matched, they are annoying
          }
          default -> Result.err(true);
        };
      }
      case Pat.Tuple tuple -> {
        term = pre.apply(term);
        yield switch (term) {
          case TupTerm tup -> matchMany(tuple.elements(), tup.items());
          default -> Result.err(true);
        };
      }
      case Pat.Meta meta -> throw new InternalException("Illegal pattern: Pat.Meta");
    };
  }

  public @NotNull Result<ImmutableSeq<Term>, Boolean> matchMany(
    @NotNull ImmutableSeq<Pat> pats,
    @NotNull ImmutableSeq<Term> terms
  ) {
    assert pats.sizeEquals(terms) : "List size mismatch 😱";

    var subst = MutableList.<Term>create();

    for (var p : pats.zipView(terms)) {
      var pat = p.component1();
      var term = p.component2();
      var result = match(pat, term);

      if (result.isErr()) return result;
      subst.appendAll(result.get());
    }

    return Result.ok(subst.toImmutableSeq());
  }
}
