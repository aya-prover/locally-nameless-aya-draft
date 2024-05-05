// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.generic;

import org.aya.syntax.core.pat.PatToTerm;
import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.Callable;
import org.aya.syntax.core.term.xtt.*;
import org.aya.syntax.ref.GenerateKind;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thread-unsafe name generator
 */
public class NameGenerator {
  private int id = 0;

  public int nextId() {
    return id++;
  }

  public @NotNull String next(@Nullable Term whty) {
    return whty == null ? nextName(null) : nextName(nameOf(whty));
  }

  // TODO: replace all usage of next with nextVar except this
  public @NotNull LocalVar nextVar(@Nullable Term whty) {
    return new LocalVar(next(whty), SourcePos.SER, GenerateKind.Tyck.INSTANCE);
  }

  public @NotNull String nextName(@Nullable String typeName) {
    return (typeName == null ? "" : Constants.ANONYMOUS_PREFIX + typeName)
      + Constants.ANONYMOUS_PREFIX + nextId();
  }

  public @Nullable String nameOf(@NotNull Term ty) {
    return switch (ty) {
      case FreeTerm freeTerm -> freeTerm.name().name();
      case MetaPatTerm(var meta) -> {
        var solution = meta.solution().get();
        if (solution == null) yield nextName(null);
        yield nameOf(PatToTerm.visit(solution));
      }
      case Callable data -> data.ref().name();
      case PiTerm _ -> "Pi";
      case SigmaTerm _ -> "Sigma";
      case DimTyTerm _ -> "Dim";
      case ProjTerm p -> nameOf(p.of());
      case AppTerm a -> nameOf(a.fun());
      case PAppTerm a -> nameOf(a.fun());
      case DimTerm _, ErrorTerm _, LamTerm _, SortTerm _, TupTerm _, LocalTerm _ -> null;
      case EqTerm _ -> "Eq";
      case CoeTerm _ -> "coe";
    };
  }
}
