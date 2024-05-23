// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.core.repr;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableLinkedHashMap;
import kala.collection.mutable.MutableMap;
import kala.control.Either;
import kala.control.Option;
import kala.tuple.Tuple;
import kala.tuple.Tuple2;
import org.aya.syntax.core.def.Def;
import org.aya.syntax.core.repr.CodeShape.*;
import org.jetbrains.annotations.NotNull;

import static org.aya.syntax.core.repr.CodeShape.GlobalId.SUC;
import static org.aya.syntax.core.repr.CodeShape.GlobalId.ZERO;
import static org.aya.syntax.core.repr.CodeShape.LocalId.*;
import static org.aya.syntax.core.repr.ParamShape.named;

/**
 * @author kiva
 */
public sealed interface AyaShape {
  @NotNull CodeShape codeShape();

  @NotNull AyaShape NAT_SHAPE = AyaIntShape.INSTANCE;
  @NotNull AyaShape LIST_SHAPE = AyaListShape.INSTANCE;
  /*
    @NotNull AyaShape PLUS_LEFT_SHAPE = AyaPlusFnLeftShape.INSTANCE;
    @NotNull AyaShape PLUS_RIGHT_SHAPE = AyaPlusFnShape.INSTANCE;
  */
  @NotNull ImmutableSeq<AyaShape> LITERAL_SHAPES = ImmutableSeq.of(NAT_SHAPE, LIST_SHAPE/*, PLUS_RIGHT_SHAPE*/);

  enum AyaIntShape implements AyaShape {
    INSTANCE;

    public static final @NotNull CodeShape DATA_NAT = new DataShape(
      DATA,
      ImmutableSeq.empty(), ImmutableSeq.of(
      new ConShape(ZERO, ImmutableSeq.empty()),
      new ConShape(SUC, ImmutableSeq.of(ParamShape.ty(TermShape.NameCall.of(DATA))))
    ));

    @Override public @NotNull CodeShape codeShape() { return DATA_NAT; }
  }

  enum AyaListShape implements AyaShape {
    INSTANCE;

    public static final @NotNull LocalId A = new LocalId("A");

    public static final @NotNull CodeShape DATA_LIST = new DataShape(
      DATA,
      ImmutableSeq.of(named(A, new TermShape.Sort(null, 0))),
      ImmutableSeq.of(
        new ConShape(GlobalId.NIL, ImmutableSeq.empty()),
        new ConShape(GlobalId.CONS, ImmutableSeq.of(
          ParamShape.ty(TermShape.NameCall.of(A)),
          ParamShape.ty(new TermShape.NameCall(DATA, ImmutableSeq.of(TermShape.NameCall.of(A))))
        )) // List A
      ));

    @Override public @NotNull CodeShape codeShape() { return DATA_LIST; }
  }

  enum AyaPlusFnShape implements AyaShape {
    INSTANCE;

    public static final @NotNull CodeShape FN_PLUS = new FnShape(
      FUNC,
      // _ : Nat -> Nat -> Nat
      ImmutableSeq.of(
        new TermShape.ShapeCall(TYPE, AyaIntShape.DATA_NAT, ImmutableSeq.empty()),
        TermShape.NameCall.of(TYPE)
      ).map(ParamShape::ty),
      TermShape.NameCall.of(TYPE),
      Either.right(ImmutableSeq.of(
        // | a, 0 => a
        new ClauseShape(ImmutableSeq.of(
          new PatShape.Bind(LHS), PatShape.ShapedCon.of(TYPE, ZERO)
        ), TermShape.NameCall.of(LHS)),
        // | a, suc b => suc (_ a b)
        new ClauseShape(ImmutableSeq.of(
          new PatShape.Bind(LHS), new PatShape.ShapedCon(TYPE, SUC, ImmutableSeq.of(new PatShape.Bind(RHS)))
        ), new TermShape.ConCall(TYPE, SUC, ImmutableSeq.of(new TermShape.NameCall(FUNC, ImmutableSeq.of(
          TermShape.NameCall.of(LHS),
          TermShape.NameCall.of(RHS)
        )))))
      ))
    );

    @Override public @NotNull CodeShape codeShape() { return FN_PLUS; }
  }

  enum AyaPlusFnLeftShape implements AyaShape {
    INSTANCE;

    public static final @NotNull CodeShape FN_PLUS = new FnShape(
      FUNC,
      // _ : Nat -> Nat -> Nat
      ImmutableSeq.of(
        new TermShape.ShapeCall(TYPE, AyaIntShape.DATA_NAT, ImmutableSeq.empty()),
        TermShape.NameCall.of(TYPE)
      ).map(ParamShape::ty),
      TermShape.NameCall.of(TYPE),
      Either.right(ImmutableSeq.of(
        // | 0, b => b
        new ClauseShape(ImmutableSeq.of(
          PatShape.ShapedCon.of(TYPE, ZERO), new PatShape.Bind(RHS)
        ), TermShape.NameCall.of(RHS)),
        // | suc a, b => _ a (suc b)
        new ClauseShape(ImmutableSeq.of(
          new PatShape.ShapedCon(TYPE, SUC, ImmutableSeq.of(new PatShape.Bind(LHS))), new PatShape.Bind(RHS)
        ), new TermShape.ConCall(TYPE, SUC, ImmutableSeq.of(new TermShape.NameCall(FUNC, ImmutableSeq.of(
          TermShape.NameCall.of(LHS),
          TermShape.NameCall.of(RHS)
        )))))
      ))
    );

    @Override public @NotNull CodeShape codeShape() { return FN_PLUS; }
  }

  class Factory {
    public @NotNull MutableMap<Def, ShapeRecognition> discovered = MutableLinkedHashMap.of();

    public @NotNull ImmutableSeq<Tuple2<Def, ShapeRecognition>> findImpl(@NotNull AyaShape shape) {
      return discovered.view().map(Tuple::of)
        .filter(t -> t.component2().shape() == shape)
        .toImmutableSeq();
    }

    public @NotNull Option<ShapeRecognition> find(@NotNull Def def) {
      return discovered.getOption(def);
    }

    public void bonjour(@NotNull Def def, @NotNull ShapeRecognition shape) {
      // TODO[literal]: what if a def has multiple shapes?
      discovered.put(def, shape);
    }

    /** Discovery of shaped literals */
    public void bonjour(@NotNull Def def) {
      AyaShape.LITERAL_SHAPES.view()
        .flatMap(shape -> new ShapeMatcher().match(shape, def))
        .forEach(shape -> bonjour(def, shape));
    }

    public void importAll(@NotNull Factory other) {
      discovered.putAll(other.discovered);
    }
  }
}
