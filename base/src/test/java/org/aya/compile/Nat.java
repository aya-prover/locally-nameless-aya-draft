// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.compile;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Result;
import org.aya.syntax.compile.JitCon;
import org.aya.syntax.compile.JitConCall;
import org.aya.syntax.compile.JitData;
import org.aya.syntax.compile.JitFn;
import org.aya.syntax.core.term.SortTerm;
import org.aya.syntax.core.term.Term;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Nat {
  final class Nat$ extends JitData {
    public static final @NotNull Nat$ INSTANCE = new Nat$();

    public final static class O extends JitCon {
      public static final @NotNull O INSTANCE = new O();

      private O() {
        super(0, new boolean[0], new String[0], Nat$.INSTANCE);
      }

      @Override public @NotNull Term telescope(int i, Term... teleArgs) {
        switch (i) {
          default:
            return Panic.unreachable();
        }
      }

      @Override public @NotNull Term result(Term... teleArgs) {
        assert teleArgs.length == 0;
        return (Term) ((Object) Nat$.INSTANCE.of());
      }

      @Override protected @NotNull Result<ImmutableSeq<Term>, Boolean> isAvailable(@NotNull Term[] args) {
        assert args.length == 0;
        return Result.ok(ImmutableSeq.from(args));
      }
    }

    public final static class S extends JitCon {
      public static final @NotNull S INSTANCE = new S();

      private S() {
        super(1, new boolean[]{true}, new String[]{"n"}, Nat$.INSTANCE);
      }

      @Override public @NotNull Term telescope(int i, Term... teleArgs) {
        switch (i) {
          case 0:
            assert teleArgs.length == 0;
            return (Term) ((Object) Nat$.INSTANCE.of());
          default:
            return Panic.unreachable();
        }
      }

      @Override public @NotNull Term result(Term... teleArgs) {
        assert teleArgs.length == 1;
        return (Term) ((Object) Nat$.INSTANCE.of());
      }

      @Override protected @NotNull Result<ImmutableSeq<Term>, Boolean> isAvailable(@NotNull Term[] args) {
        assert args.length == 0;
        return Result.ok(ImmutableSeq.from(args));
      }
    }

    private Nat$() {
      super(0, new boolean[0], new String[0], 2);
    }

    @Override public synchronized @NotNull JitCon[] constructors() {
      if (this.constructors[0] == null) {
        this.constructors[0] = O.INSTANCE;
        this.constructors[1] = S.INSTANCE;
      }

      return this.constructors;
    }

    @Override public @NotNull Term telescope(int i, Term... teleArgs) {
      switch (i) {
        default:
          return Panic.unreachable();
      }
    }

    @Override public @NotNull Term result(Term... teleArgs) {
      return SortTerm.Type0;
    }
  }

  final class plus extends JitFn {
    public static final @NotNull plus INSTANCE = new plus();

    private plus() {
      super(2, new boolean[]{true, true,}, new String[]{"a", "b"});
    }

    @Override public @NotNull Term telescope(int i, Term... teleArgs) {
      switch (i) {
        case 0:
          assert teleArgs.length == 0;
          return (Term) ((Object) Nat$.INSTANCE.of());
        case 1:
          assert teleArgs.length == 1;
          return (Term) ((Object) Nat$.INSTANCE.of());
        default:
          return Panic.unreachable();
      }
    }

    @Override public @NotNull Term result(Term... teleArgs) {
      assert teleArgs.length == 2;
      return (Term) ((Object) Nat$.INSTANCE.of());
    }

    @Override public @Nullable Term invoke(Term... args) {
      assert args.length == 2;
      var args0 = args[0];
      var args1 = args[1];

      if (((JitConCall) (Object) args0) instanceof JitConCall) {
        if (((JitConCall) ((Object) args0)).instance() == Nat$.O.INSTANCE) {
          return args1;
        }

        if (((JitConCall) ((Object) args0)).instance() == Nat$.S.INSTANCE) {
          var var10000 = ((JitConCall) ((Object) args0)).conArgs()[0];
          // We directly call `invoke` rather than construct a JitFnCall,
          // since the normalizer ALWAYS unfolds a JitFnCall
          return (Term) ((Object) Nat$.S.INSTANCE.of(new Term[0], invoke(var10000, args1)));
        }
      }

      return null;
    }
  }
}
