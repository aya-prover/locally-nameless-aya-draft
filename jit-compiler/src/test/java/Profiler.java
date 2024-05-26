// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.

import org.jetbrains.annotations.NotNull;

public class Profiler {
  public static long profile(@NotNull Runnable runnable) {
    var begin = System.currentTimeMillis();
    runnable.run();
    var end = System.currentTimeMillis();
    return end - begin;
  }

  public static long profileMany(int count, @NotNull Runnable runnable) {
    assert count > 0;
    var sum = 0L;
    long begin, end;

    for (int i = 0; i < count; ++i) {
      begin = System.currentTimeMillis();
      runnable.run();
      end = System.currentTimeMillis();
      sum += end - begin;
    }

    return sum / count;
  }
}
