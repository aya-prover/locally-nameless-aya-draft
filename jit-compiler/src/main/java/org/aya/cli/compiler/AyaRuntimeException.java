// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.cli.compiler;

import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class AyaRuntimeException {
  public static final @NotNull String BANNER = """
    You encounter a Aya runtime exception, this is thrown during execution of Compiled Aya.
    If you didn't invoke Compiled Aya directly, then this might be a Aya problem.
    """;

  public static @NotNull Panic runtime(@NotNull Exception cause) {
    return new Panic(BANNER, cause);
  }

  public static <R> R onRuntime(@NotNull Supplier<R> block) {
    try {
      return block.get();
    } catch (Panic e) {
      throw e;
    } catch (Exception e) {
      throw runtime(e);
    }
  }
}
