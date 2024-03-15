// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.generic;

import org.aya.syntax.concrete.stmt.Stmt;
import org.aya.syntax.concrete.stmt.decl.Decl;
import org.aya.util.error.SourceNode;

public sealed interface TyckUnit
  extends SourceNode
  permits Decl, Stmt {
}
