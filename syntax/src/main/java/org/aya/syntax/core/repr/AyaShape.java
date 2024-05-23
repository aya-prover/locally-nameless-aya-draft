// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.repr;

import kala.collection.immutable.ImmutableMap;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableLinkedHashMap;
import kala.collection.mutable.MutableMap;
import kala.control.Either;
import kala.control.Option;
import kala.tuple.Tuple;
import kala.tuple.Tuple2;
import org.aya.generic.stmt.Shaped;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.ConDef;
import org.aya.syntax.core.def.Def;
import org.aya.syntax.core.def.FnDef;
import org.aya.syntax.core.repr.CodeShape.*;
import org.aya.syntax.core.term.Term;
import org.aya.syntax.core.term.call.DataCall;
import org.aya.syntax.core.term.repr.IntegerOps;
import org.aya.syntax.ref.DefVar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.aya.syntax.core.repr.CodeShape.GlobalId.SUC;
import static org.aya.syntax.core.repr.CodeShape.GlobalId.ZERO;
import static org.aya.syntax.core.repr.CodeShape.LocalId.*;

/**
 * @author kiva, ice1000
 */
public enum AyaShape {
  NAT_SHAPE {
    public static final @NotNull CodeShape DATA_NAT = new DataShape(
      DATA,
      ImmutableSeq.empty(), ImmutableSeq.of(
      new ConShape(ZERO, ImmutableSeq.empty()),
      new ConShape(SUC, ImmutableSeq.of(TermShape.NameCall.of(DATA)))
    ));

    @Override public @NotNull CodeShape codeShape() { return DATA_NAT; }
  },
  LIST_SHAPE {
    public static final @NotNull CodeShape DATA_LIST = new DataShape(
      DATA,
      ImmutableSeq.of(new TermShape.Sort(null, 0)),
      ImmutableSeq.of(
        new ConShape(GlobalId.NIL, ImmutableSeq.empty()),
        new ConShape(GlobalId.CONS, ImmutableSeq.of(
          new TermShape.DeBruijn(0),
          new TermShape.NameCall(DATA, ImmutableSeq.of(new TermShape.DeBruijn(1)))
        )) // List A
      ));

    @Override public @NotNull CodeShape codeShape() { return DATA_LIST; }
  },
  PLUS_LEFT_SHAPE {
    public static final @NotNull CodeShape FN_PLUS = new FnShape(
      FUNC,
      // _ : Nat -> Nat -> Nat
      ImmutableSeq.of(
        new TermShape.ShapeCall(TYPE, NAT_SHAPE.codeShape(), ImmutableSeq.empty()),
        TermShape.NameCall.of(TYPE)
      ),
      TermShape.NameCall.of(TYPE),
      Either.right(ImmutableSeq.of(
        // | a, 0 => a
        new ClauseShape(ImmutableSeq.of(
          PatShape.Basic.Bind, PatShape.ShapedCon.of(TYPE, ZERO)
        ), new TermShape.DeBruijn(0)),
        // | a, suc b => suc (_ a b)
        new ClauseShape(ImmutableSeq.of(
          PatShape.Basic.Bind, new PatShape.ShapedCon(TYPE, SUC,
            ImmutableSeq.of(PatShape.Basic.Bind))
        ), new TermShape.ConCall(TYPE, SUC, ImmutableSeq.of(new TermShape.NameCall(FUNC,
          ImmutableSeq.of(
            new TermShape.DeBruijn(1),
            new TermShape.DeBruijn(0)
          )))))
      ))
    );

    @Override public @NotNull CodeShape codeShape() { return FN_PLUS; }
  },
  PLUS_RIGHT_SHAPE {
    public static final @NotNull CodeShape FN_PLUS = new FnShape(
      FUNC,
      // _ : Nat -> Nat -> Nat
      ImmutableSeq.of(
        new TermShape.ShapeCall(TYPE, NAT_SHAPE.codeShape(), ImmutableSeq.empty()),
        TermShape.NameCall.of(TYPE)
      ),
      TermShape.NameCall.of(TYPE),
      Either.right(ImmutableSeq.of(
        // | 0, b => b
        new ClauseShape(ImmutableSeq.of(
          PatShape.ShapedCon.of(TYPE, ZERO), PatShape.Basic.Bind
        ), new TermShape.DeBruijn(0)),
        // | suc a, b => _ a (suc b)
        new ClauseShape(ImmutableSeq.of(
          new PatShape.ShapedCon(TYPE, SUC, ImmutableSeq.of(PatShape.Basic.Bind)),
          PatShape.Basic.Bind
        ), new TermShape.ConCall(TYPE, SUC, ImmutableSeq.of(new TermShape.NameCall(FUNC, ImmutableSeq.of(
          new TermShape.DeBruijn(1),
          new TermShape.DeBruijn(0)
        )))))
      ))
    );

    @Override public @NotNull CodeShape codeShape() { return FN_PLUS; }
  };

  @NotNull abstract CodeShape codeShape();

  public static Shaped.Applicable<Term, ConDef, TeleDecl.DataCon> ofCon(
    @NotNull DefVar<ConDef, TeleDecl.DataCon> ref,
    @NotNull ShapeRecognition paramRecog,
    @NotNull DataCall paramType
  ) {
    if (paramRecog.shape() == AyaShape.NAT_SHAPE) {
      return new IntegerOps.ConRule(ref, paramRecog, paramType);
    }
    return null;
  }

  public static @Nullable Shaped.Applicable<Term, FnDef, TeleDecl.FnDecl> ofFn(
    @NotNull DefVar<FnDef, TeleDecl.FnDecl> ref,
    @NotNull ShapeRecognition recog
  ) {
    var core = ref.core;
    if (core == null) return null;

    if (recog.shape() == AyaShape.PLUS_LEFT_SHAPE || recog.shape() == AyaShape.PLUS_RIGHT_SHAPE) {
      if (!(core.result instanceof DataCall paramType)) return null;
      var dataDef = paramType.ref().core;
      assert dataDef != null : "How?";

      return new IntegerOps.FnRule(ref, IntegerOps.FnRule.Kind.Add);
    }

    return null;
  }

  public static class Factory {
    public @NotNull MutableMap<DefVar<?, ?>, ShapeRecognition> discovered = MutableLinkedHashMap.of();

    public @NotNull ImmutableSeq<Tuple2<Def, ShapeRecognition>> findImpl(@NotNull AyaShape shape) {
      return discovered.view()
        .map((a, b) -> Tuple.of((Def) a.core, b))
        .filter(t -> t.component2().shape() == shape)
        .toImmutableSeq();
    }

    public @NotNull Option<ShapeRecognition> find(@Nullable Def def) {
      if (def == null) return Option.none();
      return discovered.getOption(def.ref());
    }

    public void bonjour(@NotNull Def def, @NotNull ShapeRecognition shape) {
      // TODO[literal]: what if a def has multiple shapes?
      discovered.put(def.ref(), shape);
    }

    /** Discovery of shaped literals */
    public void bonjour(@NotNull Def def) {
      for (var shape : AyaShape.values()) {
        new ShapeMatcher(ImmutableMap.from(discovered)).match(shape, def)
          .ifDefined(recog -> bonjour(def, recog));
      }
    }

    public void importAll(@NotNull Factory other) {
      discovered.putAll(other.discovered);
    }
  }
}
