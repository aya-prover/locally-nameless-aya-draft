// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.def;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Either;
import org.aya.generic.Modifier;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.DefVar;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.function.BiFunction;

/**
 * @author ice1000
 */
public final class FnDef extends TopLevelDef<Term> {
  public final @NotNull EnumSet<Modifier> modifiers;
  public final @NotNull DefVar<FnDef, TeleDecl.FnDecl> ref;
  public final @NotNull Either<Term, ImmutableSeq<Term.Matching>> body;

  public FnDef(
    @NotNull DefVar<FnDef, TeleDecl.FnDecl> ref, @NotNull ImmutableSeq<Param> telescope,
    @NotNull Term result,
    @NotNull EnumSet<Modifier> modifiers,
    @NotNull Either<Term, ImmutableSeq<Term.Matching>> body
  ) {
    super(telescope, result);
    ref.core = this;
    this.ref = ref;
    this.modifiers = modifiers;
    this.body = body;
  }

  public static <T> BiFunction<Term, Either<Term, ImmutableSeq<Term.Matching>>, T>
  factory(BiFunction<Term, Either<Term, ImmutableSeq<Term.Matching>>, T> function) {
    return function;
  }

  public @NotNull DefVar<FnDef, TeleDecl.FnDecl> ref() {
    return ref;
  }
}
