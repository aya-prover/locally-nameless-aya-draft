package org.aya.syntax.ref;

import org.jetbrains.annotations.Debug;
import org.jetbrains.annotations.NotNull;

/**
 * @author kiva
 */
@Debug.Renderer(hasChildren = "false", text = "name()")
public interface AnyVar {
  @NotNull String name();
}
