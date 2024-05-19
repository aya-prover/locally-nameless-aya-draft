# Backend

```java
import org.aya.syntax.core.Term;

abstract class JitData extends Telescopic {
}

abstract class JitCon extends Telescopic {
  // TODO: thik more
  public abstract int isAvailable(@NotNull Term[] dataArgs);
}

record JitDataCall<T extends JitData>(
  Class<T> clazz,
  Term... args
) implements Term {}

record JitConCall<T extends JitCon>(
  Class<T> clazz,
  Term[] dataArgs,
  Term... args
) implements Term {}

record JitFnCall(
  Method ref, 
  Supplier<?> thunk, 
  Term... args
) implements Term {}

record Nat implements JitData {
  record O() implements JitCon {}
  record S(Term n) implements JitCon {}
}

interface What {
  static Term jia(Term a, Term b) {
    if (a instanceof JitConCall && ((JitConCall<?>) a).clazz() == Nat.O.class) {
      return b;
    }
    
    if (a instanceof JitConCall && ((JitConCall<?>) a).clazz() == Nat.S.class) {
      return new JitConCall<>(Nat.S.class, jia(((JitConCall<?>) a).args()[0], b));
    }
  }
}

record Vec implements JitData {
  public Vec {
    this(2, new boolean[] { true, true } );
  }
  
  telescopeSize() { return 2; }
  @Override Term telescope(int i, Term[] teleArgs) {
    switch (i) {
      case 0: return SortTerm.Type0;
      case 1: return new JitDataCall<>(Nat.class);
      default: return Panic.unreachable();
    }
  }

  record vnil() implements JitCon {
    @Override Term telescope(int i, Term[] teleArgs) {
      switch (i) {
        case 0: return teleArgs[0];
        default: return Panic.unreachable();
      }
    }

    @Override 
    int isAvailable(@NotNull Term[] dataArgs) {
      var dataArgs0 = dataArgs[0];
      var dataArgs1 = dataArgs[1];
      
      // It is impossible that we receive a core ConCall,
      // since this is a compiled thing, so we can only refer to some compiled thing
      // therefore the ConCall of some compiled thing should be JitConCall
      if (dataArgs1 instanceof JitConCall) {
        if (((JitConCall<?>) dataArgs1).clazz() == Nat.O.class) {
          // good
          return 1;
        }
        
        // fail
        return 0;
      }

      // stuck
      return -1;
    }
  }

  record vcons(Term value, Vec xs) implements JitCon {
    @Override
    Term telescope(int i, Term[] teleArgs) {
      switch (i) {
        case 0: return daraArgs[0]; 
        case 1: return ((Nat.S) dataArgs[1]).n;
        default: return Panic.unreachable();
      }
    }

    @Override
    boolean isAvailable(@NotNull Term[] dataArgs) {
      if (dataArgs[1] instanceof Nat.S) {
        return true;
      }
      
      return false;
    }
  }
}

```
