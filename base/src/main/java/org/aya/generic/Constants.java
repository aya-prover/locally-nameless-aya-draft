// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.generic;

import org.aya.syntax.concrete.Expr;
import org.aya.util.error.Global;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public interface Constants {
  @NotNull @NonNls String ANONYMOUS_PREFIX = "_";
  @NotNull @NonNls String GENERATED_POSTFIX = "'";
  @NotNull @NonNls String SCOPE_SEPARATOR = "::";
  @NotNull Pattern SCOPE_SEPARATOR_PATTERN = Pattern.compile(SCOPE_SEPARATOR);
  @NotNull @NonNls String AYA_POSTFIX = ".aya";
  @NotNull @NonNls String AYA_LITERATE_POSTFIX = ".aya.md"; // TODO: better name like `.laya`
  @NotNull Pattern AYA_POSTFIX_PATTERN = Pattern.compile("(\\.aya$)|(\\.aya\\.md$)");
  @NotNull @NonNls String AYAC_POSTFIX = ".ayac";
  @NotNull @NonNls String AYA_JSON = "aya.json";

  @NotNull @NonNls String ALTERNATIVE_EMPTY = "empty";
  @NotNull @NonNls String ALTERNATIVE_OR = "<|>";
  @NotNull @NonNls String LIST_NIL = "nil";
  @NotNull @NonNls String LIST_CONS = ":<";
  @NotNull @NonNls String APPLICATIVE_APP = "<*>";
  @NotNull @NonNls String FUNCTOR_PURE = "pure";
  @NotNull @NonNls String MONAD_BIND = ">>=";

  @NotNull Expr alternativeOr = new Expr.Unresolved(ALTERNATIVE_OR);
  @NotNull Expr alternativeEmpty = new Expr.Unresolved(ALTERNATIVE_EMPTY);
  @NotNull Expr listNil = new Expr.Unresolved(LIST_NIL);
  @NotNull Expr listCons = new Expr.Unresolved(LIST_CONS);
  @NotNull Expr applicativeApp = new Expr.Unresolved(APPLICATIVE_APP);
  @NotNull Expr functorPure = new Expr.Unresolved(FUNCTOR_PURE);
  @NotNull Expr monadBind = new Expr.Unresolved(MONAD_BIND);

  @Contract(pure = true)
  static @NotNull String randomName(@NotNull Object pos) {
    if (Global.NO_RANDOM_NAME) return ANONYMOUS_PREFIX;
    return ANONYMOUS_PREFIX + Math.abs(pos.hashCode()) % 10;
  }
}
