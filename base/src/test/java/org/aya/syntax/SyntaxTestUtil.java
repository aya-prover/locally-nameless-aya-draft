// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax;

import kala.collection.immutable.ImmutableSeq;
import org.aya.TestUtil;
import org.aya.producer.AyaParserImpl;
import org.aya.resolve.context.EmptyContext;
import org.aya.resolve.context.WithCtx;
import org.aya.resolve.visitor.StmtResolver;
import org.aya.syntax.concrete.stmt.Stmt;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.util.error.SourceFile;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class SyntaxTestUtil {
  private static final @NotNull Path FILE = Path.of("/home/senpai/114514.aya");

  @Contract(mutates = "param1")
  public static void resolve(@NotNull Decl decl) {
    var ctx = new EmptyContext(TestUtil.THROWING, FILE);
    StmtResolver.resolveStmt(new WithCtx<>(ctx, decl));
  }

  @Contract(pure = true)
  public static @NotNull ImmutableSeq<Stmt> parse(@Language("Aya") @NotNull String code) {
    return new AyaParserImpl(TestUtil.THROWING).program(new SourceFile("<baka>", FILE, code));
  }
}
