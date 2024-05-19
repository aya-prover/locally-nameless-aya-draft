// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

public abstract class Datatype extends Telescopic {
  public final Constructor[] constructors;

  protected Datatype(int telescopeSize, boolean[] telescopeLicit, String[] telescopeName, Constructor[] constructors) {
    super(telescopeSize, telescopeLicit, telescopeName);
    this.constructors = constructors;
  }
}
