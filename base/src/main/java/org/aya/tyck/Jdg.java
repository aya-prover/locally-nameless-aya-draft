// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import kala.value.LazyValue;
import org.aya.generic.AyaDocile;
import org.aya.syntax.core.term.SortTerm;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.ref.LocalVar;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

public sealed interface Jdg {
  @NotNull Term wellTyped();
  @NotNull Term type();

  @NotNull Jdg bind(@NotNull LocalVar var);

  /**
   * {@link Default#type} is the type of {@link Default#wellTyped}.
   *
   * @author ice1000
   */
  record Default(@Override @NotNull Term wellTyped, @Override @NotNull Term type) implements Jdg {
    public static @NotNull Default error(@NotNull AyaDocile description) {
      throw new UnsupportedOperationException("TODO");
    }
    @Override public @NotNull Default bind(@NotNull LocalVar var) {
      return new Default(wellTyped().bind(var), type().bind(var));
    }
  }

  record Sort(@Override @NotNull SortTerm wellTyped) implements Jdg {
    @Override public @NotNull SortTerm type() { return wellTyped.succ(); }
    @Override public @NotNull Sort bind(@NotNull LocalVar var) { return this; }
  }

  record Lazy(@Override @NotNull Term wellTyped, @NotNull LazyValue<Term> lazyType) implements Jdg {
    @Override public @NotNull Term type() { return lazyType.get(); }
    @Override public @NotNull Jdg bind(@NotNull LocalVar var) { return map(t -> t.bind(var)); }
    public @NotNull Lazy map(@NotNull UnaryOperator<Term> f) {
      return new Lazy(f.apply(wellTyped), lazyType.map(f));
    }
  }
}
