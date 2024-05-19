// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.resolve.module;

import kala.collection.immutable.ImmutableSeq;
import org.aya.normalize.PrimFactory;
import org.aya.resolve.ResolveInfo;
import org.aya.resolve.StmtResolvers;
import org.aya.resolve.context.ModuleContext;
import org.aya.resolve.salt.AyaBinOpSet;
import org.aya.syntax.concrete.stmt.Stmt;
import org.aya.syntax.core.repr.AyaShape;
import org.aya.syntax.ref.ModulePath;
import org.aya.tyck.order.AyaOrgaTycker;
import org.aya.tyck.order.AyaSccTycker;
import org.aya.tyck.tycker.Problematic;
import org.aya.util.reporter.DelayedReporter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author re-xyr
 */
public interface ModuleLoader extends Problematic {
  default <E extends Exception> @NotNull ResolveInfo
  tyckModule(ResolveInfo resolveInfo, ModuleCallback<E> onTycked) throws E {
    var SCCs = resolveInfo.depGraph().topologicalOrder();
    var delayedReporter = new DelayedReporter(reporter());
    var sccTycker = new AyaOrgaTycker(AyaSccTycker.create(resolveInfo, delayedReporter), resolveInfo);
    // in case we have un-messaged TyckException
    try (delayedReporter) {
      SCCs.forEach(sccTycker::tyckSCC);
    } finally {
      if (onTycked != null) onTycked.onModuleTycked(
        resolveInfo, sccTycker.sccTycker().wellTyped().toImmutableSeq());
    }
    return resolveInfo;
  }

  @ApiStatus.Internal
  default @NotNull ResolveInfo resolveModule(
    @NotNull PrimFactory primFactory, @NotNull AyaShape.Factory shapeFactory, @NotNull AyaBinOpSet opSet,
    @NotNull ModuleContext context, @NotNull ImmutableSeq<Stmt> program, @NotNull ModuleLoader recurseLoader
  ) {
    var resolveInfo = new ResolveInfo(context, primFactory, shapeFactory, opSet);
    new StmtResolvers(recurseLoader, resolveInfo).resolve(program, context);
    return resolveInfo;
  }

  @Nullable ResolveInfo load(@NotNull ModulePath path, @NotNull ModuleLoader recurseLoader);
  default @Nullable ResolveInfo load(@NotNull ModulePath path) {
    return load(path, this);
  }

  /**
   * @return if there is a module with path {@param path}, which can be untycked
   */
  boolean existsFileLevelModule(@NotNull ModulePath path);
}
