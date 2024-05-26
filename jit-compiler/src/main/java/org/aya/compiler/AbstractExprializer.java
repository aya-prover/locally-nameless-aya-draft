// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compiler;

import kala.collection.immutable.ImmutableSeq;
import org.aya.generic.NameGenerator;
import org.aya.syntax.core.term.Term;
import org.aya.util.IterableUtil;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractExprializer<T> extends AbstractSerializer<T> {
  protected AbstractExprializer(@NotNull StringBuilder builder, @NotNull NameGenerator nameGen) {
    super(builder, 0, nameGen);
  }

  protected void sep() {
    builder.append(", ");
  }

  protected void appendSep(@NotNull String string) {
    builder.append(string);
    sep();
  }

  protected void buildNew(@NotNull String className, @NotNull ImmutableSeq<T> terms) {
    doSerialize(STR."new \{className}(", ")", terms);
  }

  protected void buildNew(@NotNull String className, @NotNull Runnable continuation) {
    builder.append(STR."new \{className}(");
    continuation.run();
    builder.append(")");
  }

  protected void buildImmutableSeq(@NotNull String typeName, @NotNull ImmutableSeq<T> terms) {
    if (terms.isEmpty()) {
      builder.append(STR."\{CLASS_IMMSEQ}.empty()");
    } else {
      doSerialize(STR."\{CLASS_IMMSEQ}.<\{typeName}>of(", ")", terms);
    }
  }

  protected abstract @NotNull AbstractSerializer<T> doSerialize(@NotNull T term);

  protected void doSerialize(@NotNull String prefix, @NotNull String suffix, @NotNull ImmutableSeq<T> terms) {
    builder.append(prefix);
    IterableUtil.forEach(terms, this::sep, this::doSerialize);
    builder.append(suffix);
  }
}
