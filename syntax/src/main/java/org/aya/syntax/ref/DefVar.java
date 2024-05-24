// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.syntax.ref;

import kala.collection.Map;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import org.aya.generic.AyaDocile;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.util.binop.Assoc;
import org.aya.util.binop.OpDecl;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public final class DefVar<Core extends AyaDocile, Concrete extends Decl> implements AnyVar {
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

  /**
   * Binary operators can be renamed in other modules.
   * Initialized in the resolver or core deserialization.
   */
  public @NotNull Map<ModulePath, OpDecl> opDeclRename = Map.empty();

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
  public static <Core extends AyaDocile, Concrete extends Decl>
  @NotNull DefVar<Core, Concrete> concrete(@NotNull Concrete concrete, @NotNull String name) {
    return new DefVar<>(concrete, null, name);
  }

  /** Used in the serialization of core and primitive definitions. */
  public static <Core extends AyaDocile, Concrete extends Decl>
  @NotNull DefVar<Core, Concrete> empty(@NotNull String name) {
    return new DefVar<>(null, null, name);
  }

  @Override public boolean equals(@Nullable Object o) {return this == o;}
  @Override public int hashCode() {return System.identityHashCode(this);}

  public boolean isInModule(@NotNull ModulePath moduleName) {
    return module != null && module.isInModule(moduleName);
  }

  public @NotNull ImmutableSeq<String> qualifiedName() {
    return module == null ? ImmutableSeq.of(name) : module.module().appended(name);
  }

  public @Nullable OpDecl resolveOpDecl(@NotNull ModulePath modulePath) {
    return opDeclRename.getOrDefault(modulePath, opDecl);
  }

  public void addOpDeclRename(@NotNull ModulePath modulePath, @NotNull OpDecl opDecl) {
    if (opDeclRename instanceof MutableMap<ModulePath, OpDecl> mutable) {
      mutable.put(modulePath, opDecl);
    } else {
      opDeclRename = MutableMap.of(modulePath, opDecl);
    }
  }
}
