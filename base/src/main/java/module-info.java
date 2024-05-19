module aya.base {
  requires transitive aya.md;
  requires transitive aya.syntax;

  requires static org.jetbrains.annotations;

  requires org.commonmark;
  requires jdk.jdi;
  // requires manifold.delegation.rt;

  exports org.aya.normalize;
  exports org.aya.prelude;
  exports org.aya.resolve.context;
  exports org.aya.tyck.error;
  exports org.aya.tyck.ctx;
  exports org.aya.tyck.tycker;
  exports org.aya.tyck;
  exports org.aya.unify;
}
