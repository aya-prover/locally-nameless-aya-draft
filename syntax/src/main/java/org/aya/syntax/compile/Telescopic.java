// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.compile;

import kala.collection.immutable.ImmutableArray;
import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.core.term.Param;
import org.aya.syntax.core.term.Term;

public abstract class Telescopic {
  public final int telescopeSize;
  public final boolean[] telescopeLicit;
  public final String[] telescopeNames;

  /**
   * @param teleArgs the arguments before {@param i}, for constructor, it also contains the arguments to the data
   */
  public abstract Term telescope(int i, Term... teleArgs);
  public Param telescopeRich(int i, Term... teleArgs) {
    return new Param(telescopeNames[i], telescope(i, teleArgs), telescopeLicit[i]);
  }

  public abstract Term result(Term... teleArgs);

  protected Telescopic(int telescopeSize, boolean[] telescopeLicit, String[] telescopeNames) {
    this.telescopeSize = telescopeSize;
    this.telescopeLicit = telescopeLicit;
    this.telescopeNames = telescopeNames;
  }

  public static class LocallyNameless extends Telescopic {
    public final ImmutableSeq<Param> params;
    public final Term result;
    public LocallyNameless(ImmutableSeq<Param> params, Term result) {
      super(params.size(), new boolean[params.size()], new String[params.size()]);
      this.result = result;
      for (int i = 0; i < params.size(); i++) {
        telescopeLicit[i] = params.get(i).explicit();
        telescopeNames[i] = params.get(i).name();
      }
      this.params = params;
    }
    @Override public Term telescope(int i, Term... teleArgs) {
      ImmutableSeq<Term> unsafeView = ImmutableArray.Unsafe.wrap(teleArgs);
      return params.get(i).type().instantiateTele(unsafeView.sliceView(0, i));
    }
    @Override public Term result(Term... teleArgs) {
      assert teleArgs.length == telescopeSize;
      ImmutableSeq<Term> unsafeView = ImmutableArray.Unsafe.wrap(teleArgs);
      return result.instantiateTele(unsafeView.view());
    }
  }
}
