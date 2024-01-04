module aya.base {
  requires transitive aya.md;
  requires transitive aya.pretty;
  requires transitive aya.util;
  requires transitive aya.util.more;
  requires transitive kala.base;
  requires transitive kala.collection;

  requires static org.jetbrains.annotations;

  requires aya.ij.parsing.core;
  requires org.commonmark;

  exports org.aya.prelude;
  exports org.aya.syntax.concrete.decl;
  exports org.aya.syntax.core.def;
  exports org.aya.syntax.core.term;
  exports org.aya.syntax.ref;
  exports org.aya.syntax.concrete.expr;
}
