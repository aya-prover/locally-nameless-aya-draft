// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compile;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CompileTest {
  @Test
  public void test0() {
    var o = Nat.Nat$.O.INSTANCE;
    assertNotNull(o.dataType.constructors()[0]);
    assertNotNull(o.dataType.constructors()[1]);
  }
}
