// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.

import kala.collection.immutable.ImmutableSeq;
import kala.control.Result;
import org.aya.compiler.util.SerializeUtils;
import org.aya.normalize.PatMatcher;
import org.aya.syntax.compile.JitCon;
import org.aya.syntax.compile.JitConCall;
import org.aya.syntax.compile.JitData;
import org.aya.syntax.compile.JitFn;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.core.term.MetaPatTerm;
import org.aya.syntax.core.term.SortTerm;
import org.aya.syntax.core.term.Term;
import org.aya.util.error.Panic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

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
        return Nat$.INSTANCE.of();
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
            return Nat$.INSTANCE.of();
          default:
            return Panic.unreachable();
        }
      }

      @Override public @NotNull Term result(Term... teleArgs) {
        assert teleArgs.length == 1;
        return Nat$.INSTANCE.of();
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
          return Nat$.INSTANCE.of();
        case 1:
          assert teleArgs.length == 1;
          return Nat$.INSTANCE.of();
        default:
          return Panic.unreachable();
      }
    }

    @Override public @NotNull Term result(Term... teleArgs) {
      assert teleArgs.length == 2;
      return Nat$.INSTANCE.of();
    }

    @Override public @Nullable Term invoke(Term... args) {
      assert args.length == 2;
      var args0 = args[0];
      var args1 = args[1];

      if (args0 instanceof JitConCall) {
        if (((JitConCall) args0).instance() == Nat$.O.INSTANCE) {
          return args1;
        }

        if (((JitConCall) args0).instance() == Nat$.S.INSTANCE) {
          var var10000 = ((JitConCall) args0).conArgs()[0];
          // We directly call `invoke` rather than construct a JitFnCall,
          // since the normalizer ALWAYS unfolds a JitFnCall
          return Nat$.S.INSTANCE.of(new Term[0], invoke(var10000, args1));
        }
      }

      return null;
    }
  }

  static final class Vec extends JitData {
    public static final Vec INSTANCE = new Vec();

    public static final class vnil extends JitCon {
      public static final vnil INSTANCE = new vnil();

      protected vnil() {
        super(0, new boolean[]{ }, new String[]{ }, Vec.INSTANCE);
      }

      @Override
      protected @NotNull Result<ImmutableSeq<Term>, Boolean> isAvailable(@NotNull Term[] args) {
        Term[] result = new Term[2];
        int matchState = 0;
        boolean metaState = false;

        result[0] = args[0];
        Term _0 = args[1];
        if (_0 instanceof MetaPatTerm _1) {
          _0 = PatMatcher.realSolution(_1);
          if (_0 instanceof MetaPatTerm _2) {
            SerializeUtils.copyTo(result, PatMatcher.doSolveMeta(new Pat.JCon(Nat$.O.INSTANCE, ImmutableSeq.empty()), _2.meta()), 1);
            metaState = true;
          }
        }

        if (!metaState) {
          if (args[1] instanceof JitConCall _3) {
            if (_3.instance() == Nat$.O.INSTANCE) {
              metaState = true;
            } else matchState = -1;
          } else {
            matchState = 0;
          }
        }

        if (metaState) {
          matchState = 1;
        }

        switch (matchState) {
          case -1:
            return Result.err(false);
          case 0:
            return Result.err(true);
          case 1:
            var finalResult = Arrays.copyOf(result, 1);
            return Result.ok(ImmutableSeq.from(finalResult));
          default:
            return Panic.unreachable();
        }
      }

      @Override
      public @NotNull Term telescope(int i, Term... teleArgs) {
        return null;
      }
      @Override
      public @NotNull Term result(Term... teleArgs) {
        return null;
      }
    }

    private Vec() {
      super(2, new boolean[]{true, true}, new String[]{"A", "n"}, 2);
    }

    @Override
    public @NotNull JitCon[] constructors() {
      if (constructors[0] == null) {

      }

      return this.constructors;
    }

    @Override
    public @NotNull Term telescope(int i, Term... teleArgs) {
      return null;
    }

    @Override
    public @NotNull Term result(Term... teleArgs) {
      return null;
    }
  }
}
