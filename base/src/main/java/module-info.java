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

  exports org.aya.generic;
  exports org.aya.prelude;
  exports org.aya.resolve.context;
  exports org.aya.syntax.concrete.stmt.decl;
  exports org.aya.syntax.concrete.stmt;
  exports org.aya.syntax.concrete;
  exports org.aya.syntax.core.def;
  exports org.aya.syntax.core.term.call;
  exports org.aya.syntax.core.term;
  exports org.aya.syntax.ref;
  exports org.aya.tyck.tycker;
  exports org.aya.tyck;
}
