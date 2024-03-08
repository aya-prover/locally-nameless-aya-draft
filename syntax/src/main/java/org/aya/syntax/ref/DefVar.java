// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.ref;

import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.syntax.core.def.Def;
import org.aya.util.binop.Assoc;
import org.aya.util.binop.OpDecl;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public final class DefVar<Core extends Def, Concrete extends Decl> implements AnyVar {
  private final @NotNull String name;
  /** Initialized in parsing, so it might be null for deserialized user definitions. */
  public @UnknownNullability Concrete concrete;
  /** Initialized in type checking or core deserialization, so it might be null for unchecked user definitions. */
  public @UnknownNullability Core core;
  /** Initialized in the resolver or core deserialization */
  public @Nullable ModulePath module;
  public @Nullable ModulePath fileModule; // TODO: unify `module` and `fileModule`
  /** Initialized in the resolver or core deserialization */
  public @Nullable OpDecl opDecl;

  @Contract(pure = true) public @Nullable Assoc assoc() {
    if (opDecl == null) return null;
    if (opDecl.opInfo() == null) return null;
    return opDecl.opInfo().assoc();
  }

  @Contract(pure = true) public @NotNull String name() {
    return name;
  }

  private DefVar(Concrete concrete, Core core, @NotNull String name) {
    this.concrete = concrete;
    this.core = core;
    this.name = name;
  }

  /** Used in user definitions. */
  public static <Core extends Def, Concrete extends Decl>
  @NotNull DefVar<Core, Concrete> concrete(@NotNull Concrete concrete, @NotNull String name) {
    return new DefVar<>(concrete, null, name);
  }

  /** Used in the serialization of core and primitive definitions. */
  public static <Core extends Def, Concrete extends Decl>
  @NotNull DefVar<Core, Concrete> empty(@NotNull String name) {
    return new DefVar<>(null, null, name);
  }

  @Override public boolean equals(Object o) {
    return this == o;
  }

  public boolean isInModule(@NotNull ModulePath moduleName) {
    return module != null && module.isInModule(moduleName);
  }

  public @NotNull ImmutableSeq<String> qualifiedName() {
    return module == null ? ImmutableSeq.of(name) : module.module().appended(name);
  }
}
