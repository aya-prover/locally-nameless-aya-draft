// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.util.error;

import org.jetbrains.annotations.NotNull;

/**
 * 😱😱🦀🦀🦀
 *
 * @author ice1000
 */
public class Panic extends RuntimeException {
  public Panic() {}

  public Panic(@NotNull String message) {
    super(message);
  }

  public static <T> T unreachable() {
    throw new Panic("unreachable");
  }

  public Panic(@NotNull String message, Throwable cause) {
    super(message, cause);
  }

  public void printHint() {
    System.out.println(getMessage());
  }

  public int exitCode() {
    return -1;
  }
}
