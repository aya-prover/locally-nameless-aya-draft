# Locally Nameless Aya draft

This is a temporary repo for the rewrite of Aya using locally nameless with the additional foci:

* Better modularity: decomposed `base` into `syntax`, `producer`, and `base`
* Better concrete syntax tree, decompose source info from `Expr`
* Complicated mutual recursion now work in a different way, smaller SCCs in the dependency graph
* Reimplement serializer with JIT-compiled code for better efficiency
* Replace extension types and `Glue` with `PathP` and boundary separation
* Replace boundaries in constructors with an equality in the return type
* Treat full (as opposed to head) normalization more carefully
* Implement the `elim` keyword in Arend
* Instead of using inheritance, we (actually Hoshino) use a design pattern to imitate type classes to organize type checking monad
* Less test-only APIs in the type checker since we're more confident now
* Re-think about the testing infrastructure, maybe there's a better way to organize the failing cases
* Get rid of trace builders and "codifiers" because the developers never used them anyway except me for a few times
* Run internal tests using the "Orga" type checker (non-stopping & deal with mutual recursion correctly) instead of the silly sequential type checker
* Remove first-class implicit arguments, replace with the design similar to Coq
* Improve pattern matching coverage checking error report
* Redesign classes (future plan)

Technically we didn't remove `Glue` because we didn't implement it either, but we can say we replaced it from our roadmap.

Since late 2023, I was blessed with the privilege to talk to a number of students of Professor Avigad and learned a lot about Lean and some set-theoretic proof assistants. This has opened my eyes since I only know Agda and was too much into the idea of having a higher type theory as the foundation of mathematics (instead of an internal language for some higher topoi). This motivated the transition to a set-level type theory with good handling of propositional equality. I will try to weave my new understanding and thinking into this brand-new version of Aya. I am really grateful for Hoshino Tented for helping me out on this project.

Once we're done with everything that exists in the original repo, we will create a huge pull request signed by everyone contributed to this prototype and work with the aya-dev repo instead. The version number will be `0.x` still until we've figured out Java interop (aka tactics).
