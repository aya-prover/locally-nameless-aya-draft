module aya.syntax {
  requires transitive aya.pretty;
  requires transitive aya.util;
  requires transitive aya.util.more;
  requires transitive kala.base;
  requires transitive kala.collection;
  requires transitive aya.ij.parsing.core;

  requires static org.jetbrains.annotations;

  exports org.aya.generic;
  exports org.aya.prettier;
  exports org.aya.syntax.concrete.stmt.decl;
  exports org.aya.syntax.concrete.stmt;
  exports org.aya.syntax.concrete;
  exports org.aya.syntax.core.def;
  exports org.aya.syntax.core.pat;
  exports org.aya.syntax.core.term.call;
  exports org.aya.syntax.core.term.xtt;
  exports org.aya.syntax.core.term;
  exports org.aya.syntax.core;
  exports org.aya.syntax.ref;
}
