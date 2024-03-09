// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.tyck;

import org.aya.TestUtil;
import org.aya.tyck.unify.TermComparator;
import org.aya.util.Ordering;
import org.aya.util.error.SourcePos;

public class MockConversionChecker extends TermComparator {
  public MockConversionChecker() {
    super(
      new TyckState(),
      TestUtil.makeLocalCtx(),
      TestUtil.makeDBLocalCtx(),
      TestUtil.THROWING,
      SourcePos.NONE,
      Ordering.Eq);
  }
}
