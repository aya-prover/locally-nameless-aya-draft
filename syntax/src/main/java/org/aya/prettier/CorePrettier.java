// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.prettier;

import kala.collection.SeqLike;
import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import org.aya.generic.ParamLike;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.*;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.core.pat.PatToTerm;
import org.aya.syntax.core.term.*;
import org.aya.pretty.doc.Doc;
import org.aya.syntax.core.term.call.*;
import org.aya.syntax.core.term.xtt.DimTerm;
import org.aya.syntax.core.term.xtt.DimTyTerm;
import org.aya.syntax.core.term.xtt.PartialTerm;
import org.aya.syntax.ref.DefVar;
import org.aya.syntax.ref.GenerateKind;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.Arg;
import org.aya.util.error.SourcePos;
import org.aya.util.prettier.PrettierOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

/**
 * It's the pretty printer.
 * Credit after <a href="https://github.com/jonsterling/dreamtt/blob/main/frontend/Distiller.ml">Jon Sterling</a>
 *
 * @author ice1000, kiva
 * @see ConcretePrettier
 */
public class CorePrettier extends BasePrettier<Term> {
  private static int genId = 0;

  public CorePrettier(@NotNull PrettierOptions options) {
    super(options);
  }

  // private @Nullable Doc binCube(Restr.Side<Term> a, Restr.Side<Term> b, LocalVar var, @NotNull Outer outer) {
  //   if (!(a.cof().ands().sizeEquals(1) && b.cof().ands().sizeEquals(1)))
  //     return null;
  //   var aa = a.cof().ands().get(0);
  //   var bb = b.cof().ands().get(0);
  //   if (aa.inst() instanceof RefTerm(var ref) && ref == var && aa.isOne() == !bb.isOne()
  //     && bb.inst() instanceof RefTerm(var ref2) && ref2 == var
  //   ) {
  //     var aaa = term(Outer.BinOp, a.u());
  //     var bbb = term(Outer.BinOp, b.u());
  //     var eq = Doc.symbol("=");
  //     var doc = aa.isOne() ? Doc.sep(bbb, eq, aaa) : Doc.sep(aaa, eq, bbb);
  //     return checkParen(outer, doc, Outer.BinOp);
  //   }
  //   return null;
  // }

  @Override public @NotNull Doc term(@NotNull Outer outer, @NotNull Term preterm) {
    return switch (preterm) {
      case FreeTerm(var var) -> varDoc(var);
      case LocalTerm(_) -> throw new UnsupportedOperationException("TODO");
      // case MetaTerm term -> {
      //   var name = term.ref();
      //   var inner = varDoc(name);
      //   var showImplicits = options.map.get(AyaPrettierOptions.Key.ShowImplicitArgs);
      //   if (options.map.get(AyaPrettierOptions.Key.InlineMetas))
      //     yield visitCalls(null, inner, term.args().view(), outer, showImplicits);
      //   yield Doc.wrap("{?", "?}",
      //     visitCalls(null, inner, term.args().view(), Outer.Free, showImplicits));
      // }
      // case MetaLitTerm lit ->
      //   lit.repr() instanceof AyaDocile docile ? docile.toDoc(options) : Doc.plain(lit.repr().toString());
      case TupTerm(var items) -> Doc.parened(argsDoc(options, items.map(Arg::ofExplicitly)));
      // case IntegerTerm shaped -> shaped.repr() == 0
      //   ? linkLit(0, shaped.ctorRef(CodeShape.GlobalId.ZERO), CON)
      //   : linkLit(shaped.repr(), shaped.ctorRef(CodeShape.GlobalId.SUC), CON);
      case ConCallLike conCall -> visitCoreCalls(conCall.ref(), CON, conCall.conArgs(), outer, optionImplicit());
      case FnCall fnCall -> visitCoreCalls(fnCall.ref(), FN, fnCall.args(), outer, optionImplicit());
      case SigmaTerm(var params) -> {
        var tele = generateNames(params.dropLast(1));
        var last = params.getLast().instantiateTele(tele.view().map(p -> new FreeTerm(p.ref())));
        var doc = Doc.sep(
          Doc.styled(KEYWORD, Doc.symbol("Sig")),
          visitTele(tele, last, FindUsage.Free),
          Doc.symbol("**"),
          justType(Arg.ofExplicitly(last), Outer.Codomain)
        );
        // Same as Pi
        yield checkParen(outer, doc, Outer.BinOp);
      }
      case LamTerm lam -> {
        var pair = LamTerm.unwrap(lam);
        var params = generateNames(pair.component1()).view();
        var body = pair.component2();
        Doc bodyDoc;
        // Syntactic eta-contraction
        if (body instanceof Callable call && call.ref() instanceof DefVar<?, ?> defVar) {
          var paramRef = params.<Term>map(FreeTerm::new);
          var args = visibleArgsOf(call).view().map(x -> x.instantiateTele(paramRef));
          while (params.isNotEmpty() && args.isNotEmpty()) {
            if (checkUneta(args, params.getLast())) {
              args = args.dropLast(1);
              params = params.dropLast(1);
            } else break;
          }
          // if (call instanceof FieldTerm access) bodyDoc = visitAccessHead(access);
          // else {
          var style = chooseStyle(defVar);
          bodyDoc = style != null
            ? visitCoreCalls(defVar, style, args, outer, optionImplicit())
            : visitCalls(defVar.assoc(), varDoc(defVar), args.map(Arg::ofExplicitly),
              params.isEmpty() ? outer : Outer.Free,
              options.map.get(AyaPrettierOptions.Key.ShowImplicitArgs));
          // }
        } else bodyDoc = term(Outer.Free, body);

        if (params.isEmpty()) yield bodyDoc;

        var list = MutableList.of(Doc.styled(KEYWORD, Tokens.LAMBDA));
        params.forEach(param -> list.append(Doc.bracedUnless(linkDef(param), true)));
        list.append(Tokens.FN_DEFINED_AS);
        list.append(bodyDoc);
        var doc = Doc.sep(list);
        yield checkParen(outer, doc, Outer.BinOp);
      }
      case SortTerm(var kind, var lift) -> {
        var fn = Doc.styled(KEYWORD, kind.name());
        if (!kind.hasLevel()) yield fn;
        yield visitCalls(null, fn, (_, t) -> t.toDoc(options), outer,
          SeqView.of(new Arg<>(_ -> Doc.plain(String.valueOf(lift)), true)),
          options.map.get(AyaPrettierOptions.Key.ShowImplicitArgs)
        );
      }
      // case IntervalTerm _ -> Doc.styled(PRIM, "I");
      // case NewTerm(var inner) -> visitCalls(null, Doc.styled(KEYWORD, "new"), (nest, t) -> t.toDoc(options), outer,
      //   SeqView.of(new Arg<>(o -> term(Outer.AppSpine, inner), true)),
      //   options.map.get(AyaPrettierOptions.Key.ShowImplicitArgs)
      // );
      // case FieldTerm term -> visitCalls(null, visitAccessHead(term), term.args().view(), outer,
      //   options.map.get(AyaPrettierOptions.Key.ShowImplicitArgs));
      case MetaPatTerm(var ref) -> {
        if (ref.solution().get() == null) yield varDoc(generateName(null));
        yield Doc.wrap("<", ">", pat(ref, true, outer));
      }
      case ErrorTerm(_) -> {
        throw new UnsupportedOperationException("TODO");
        // var doc = desc.toDoc(options);
        // yield really ? Doc.angled(doc) : doc;
      }
      case AppTerm app -> {
        var pair = AppTerm.unapp(app);
        var args = pair.component1();
        var head = pair.component2();
        // if (head instanceof RefTerm.Field fieldRef) yield visitArgsCalls(fieldRef.ref(), MEMBER, args, outer);
        var implicits = options.map.get(AyaPrettierOptions.Key.ShowImplicitArgs);
        // Infix def-calls
        if (head instanceof Callable call && call.ref() instanceof DefVar<?, ?> var) {
          yield visitCoreCalls(var, defVar(var),
            call.args().view().appendedAll(args), outer, implicits);
        }
        yield visitCalls(null, term(Outer.AppHead, head), args.view().map(Arg::ofExplicitly), outer, implicits);
      }
      // case PrimCall prim -> visitArgsCalls(prim.ref(), PRIM, prim.args(), outer);
      // case RefTerm.Field term -> linkRef(term.ref(), MEMBER);
      case ProjTerm(var of, var ix) ->
        Doc.cat(term(Outer.ProjHead, of), Doc.symbol("."), Doc.plain(String.valueOf(ix)));
      // case MatchTerm match -> Doc.cblock(Doc.sep(Doc.styled(KEYWORD, "match"),
      //     Doc.commaList(match.discriminant().map(t -> term(Outer.Free, t)))), 2,
      //   Doc.vcat(match.clauses().view()
      //     .map(clause -> Doc.sep(Doc.symbol("|"),
      //       Doc.commaList(clause.patterns().map(p -> pat(p, Outer.Free))),
      //       Doc.symbol("=>"), term(Outer.Free, clause.body())))
      //     .toImmutableSeq()));
      case PiTerm(var params0, var body0) -> {
        // Try to omit the Pi keyword
        if (FindUsage.Bound.applyAsInt(body0, 0) == 0) yield checkParen(outer, Doc.sep(
          justType(Arg.ofExplicitly(params0), Outer.BinOp),
          Tokens.ARROW,
          term(Outer.Codomain, body0)
        ), Outer.BinOp);
        var pair = PiTerm.unpi(body0, UnaryOperator.identity());
        var params = generateNames(pair.component1());
        var body = pair.component2().instantiateTele(params.view().map(x -> new FreeTerm(x.ref())));
        var doc = Doc.sep(
          Doc.styled(KEYWORD, Tokens.KW_PI),
          visitTele(params, body, FindUsage.Free),
          Tokens.ARROW,
          term(Outer.Codomain, body)
        );
        // Add paren when it's not free or a codomain
        yield checkParen(outer, doc, Outer.BinOp);
      }
      // case ClassCall classCall -> visitArgsCalls(classCall.ref(), CLAZZ, classCall.orderedArgs(), outer);
      case DataCall dataCall -> visitCoreCalls(dataCall.ref(), DATA, dataCall.args(), outer, optionImplicit());
      // case ListTerm shaped -> {
      //   var subterms = shaped.repr().map(x -> term(Outer.Free, x));
      //   var nil = shaped.ctorRef(CodeShape.GlobalId.NIL);
      //   var cons = shaped.ctorRef(CodeShape.GlobalId.CONS);
      //   yield Doc.sep(
      //     linkListLit(Doc.symbol("["), nil, CON),
      //     Doc.join(linkListLit(Doc.COMMA, cons, CON), subterms),
      //     linkListLit(Doc.symbol("]"), nil, CON)
      //   );
      // }
      // case StringTerm(var str) -> Doc.plain("\"" + StringUtil.escapeStringCharacters(str) + "\"");
      // case PartialTyTerm(var ty, var restr) -> checkParen(outer, Doc.sep(Doc.styled(KEYWORD, "Partial"),
      //   term(Outer.AppSpine, ty), Doc.parened(restr(options, restr))), Outer.AppSpine);
      // case PartialTerm el -> partial(options, el.partial(), true, Doc.symbol("{|"), Doc.symbol("|}"));
      // case FormulaTerm(var mula) -> formula(outer, mula);
      // case PathTerm cube -> {
      //   if (cube.params().sizeEquals(1)
      //     && cube.partial() instanceof Partial.Split<Term> split
      //     && split.clauses().sizeEquals(2)
      //   ) {
      //     var var = cube.params().get(0);
      //     var clause1 = split.clauses().get(0);
      //     var clause2 = split.clauses().get(1);
      //     var beauty = binCube(clause1, clause2, var, outer);
      //     if (beauty != null) yield beauty;
      //   }
      //   yield Doc.sepNonEmpty(
      //     Doc.symbol("[|"),
      //     Doc.commaList(cube.params().map(BasePrettier::linkDef)),
      //     Doc.symbol("|]"),
      //     cube.type().toDoc(options),
      //     partial(options, cube.partial(), false, Doc.symbol("{"), Doc.symbol("}"))
      //   );
      // }
      // case PLamTerm(var params, var body) -> checkParen(outer,
      //   Doc.sep(Doc.styled(KEYWORD, "\\"),
      //     Doc.sep(params.map(BasePrettier::varDoc)),
      //     Doc.symbol("=>"),
      //     body.toDoc(options)),
      //   Outer.BinOp);
      // case PAppTerm app -> visitCalls(null, term(Outer.AppHead, app.of()),
      //   app.args().view(), outer, options.map.get(AyaPrettierOptions.Key.ShowImplicitArgs));
      // case CoeTerm(var ty, var r, var s) -> visitCalls(null,
      //   Doc.styled(KEYWORD, "coe"),
      //   Seq.of(r, s, ty).view().map(t -> new Arg<>(t, true)),
      //   outer, true);
      // case HCompTerm hComp -> throw new InternalException("TODO");
      // case InTerm(var phi, var u) -> insideOut(outer, phi, u, "inS");
      // case OutTerm(var phi, var par, var u) -> insideOut(outer, phi, u, "outS");
      // TODO
      case DimTerm _ -> throw new UnsupportedOperationException("TODO");
      case DimTyTerm _ -> throw new UnsupportedOperationException("TODO");
      case PartialTerm _ -> throw new UnsupportedOperationException("TODO");
    };
  }

  private @NotNull Doc insideOut(@NotNull Outer outer, @NotNull Term phi, @NotNull Term u, String fnName) {
    return checkParen(outer, Doc.sep(Doc.styled(KEYWORD, fnName),
      term(Outer.AppSpine, phi), term(Outer.AppSpine, u)), Outer.AppSpine);
  }

  /** @return if we can eta-contract the last argument */
  private boolean checkUneta(@NotNull SeqView<Term> args, @NotNull LocalVar param) {
    var arg = args.getLast();
    if (!(arg instanceof FreeTerm(var var))) return false;
    if (var != param) return false;
    var counter = new FindUsage(new Usage.Ref.Free(param));
    return args.dropLast(1).allMatch(a -> counter.apply(0, a) == 0);
  }

  private ImmutableSeq<Term> visibleArgsOf(Callable call) {
    return call instanceof ConCall con
      ? con.conArgs() : call.args();
    // call instanceof FieldTerm access
    // ? access.args() : call.args();
  }

  // private @NotNull Doc visitAccessHead(@NotNull FieldTerm term) {
  //   return Doc.cat(term(Outer.ProjHead, term.of()), Doc.symbol("."),
  //     linkRef(term.ref(), MEMBER));
  // }

  public @NotNull Doc pat(@NotNull Arg<Pat> pat, @NotNull Outer outer) {
    return pat(pat.term(), pat.explicit(), outer);
  }

  public @NotNull Doc pat(@NotNull Pat pat, boolean licit, Outer outer) {
    return switch (pat) {
      case Pat.Meta meta -> {
        var sol = meta.solution().get();
        yield sol != null
          ? pat(sol, licit, outer)
          : Doc.bracedUnless(linkDef(generateName(null)), licit);   // TODO: supply type name
      }
      case Pat.Bind bind -> Doc.bracedUnless(linkDef(bind.bind()), licit);
      case Pat.Ctor ctor -> {
        var ctorDoc = visitCoreCalls(ctor.ref(), CON, ctor.args().view().map(PatToTerm::visit), outer,
          optionImplicit());
        yield ctorDoc(outer, licit, ctorDoc, ctor.args().isEmpty());
      }
      case Pat.Absurd _ -> Doc.bracedUnless(Doc.styled(KEYWORD, "()"), licit);
      case Pat.Tuple tuple -> Doc.licit(licit,
        Doc.commaList(tuple.elements().view().map(sub -> pat(sub, true, Outer.Free))));
      // case Pat.ShapedInt lit -> Doc.bracedUnless(lit.repr() == 0
      //     ? linkLit(0, lit.ctorRef(CodeShape.GlobalId.ZERO), CON)
      //     : linkLit(lit.repr(), lit.ctorRef(CodeShape.GlobalId.SUC), CON),
      //   licit);
    };
  }

  public @NotNull Doc def(@NotNull Def predef) {
    return switch (predef) {
      case FnDef def -> {
        var line1 = MutableList.of(Doc.styled(KEYWORD, "def"));
        def.modifiers.forEach(m -> line1.append(Doc.styled(KEYWORD, m.keyword)));
        var tele = enrich(def.telescope());
        var subst = tele.view().<Term>map(p -> new FreeTerm(p.ref()));
        line1.appendAll(new Doc[]{
          linkDef(def.ref(), FN),
          visitTele(tele),
          Tokens.HAS_TYPE,
          term(Outer.Free, def.result)
        });
        var line1sep = Doc.sepNonEmpty(line1);
        yield def.body.fold(
          term -> Doc.sep(line1sep, Tokens.FN_DEFINED_AS, term(Outer.Free, term.instantiateTele(subst))),
          clauses -> Doc.vcat(line1sep, Doc.nest(2, visitClauses(clauses, subst))));
      }
      // case MemberDef field -> Doc.sepNonEmpty(Doc.symbol("|"),
      //   coe(field.coerce),
      //   linkDef(field.ref(), MEMBER),
      //   visitTele(field.telescope),
      //   Doc.symbol(":"),
      //   term(Outer.Free, field.result));
      // case PrimDef def -> primDoc(def.ref());
      case CtorDef ctor -> {
        var doc = Doc.sepNonEmpty(coe(ctor.coerce),
          linkDef(ctor.ref(), CON),
          visitTele(enrich(ctor.selfTele)));
        Doc line1;
        // if (ctor.pats.isNotEmpty()) {
        //   var pats = Doc.commaList(ctor.pats.view().map(pat -> pat(pat, Outer.Free)));
        //   line1 = Doc.sep(Doc.symbol("|"), pats, Doc.symbol("=>"), doc);
        // } else {
        line1 = Doc.sep(Tokens.BAR, doc);
        // }
        yield Doc.cblock(line1, 2, Doc.empty() /*partial(options, ctor.clauses, false, Doc.empty(), Doc.empty())*/);
      }
      // case ClassDef def -> Doc.vcat(Doc.sepNonEmpty(Doc.styled(KEYWORD, "class"),
      //   linkDef(def.ref(), CLAZZ),
      //   Doc.nest(2, Doc.vcat(def.members.view().map(this::def)))));
      case DataDef def -> {
        var richDataTele = enrich(def.telescope());
        var reversedRichDataTele = richDataTele.view()
          .<Term>map(t -> new FreeTerm(t.ref()))
          .reversed()
          .toImmutableSeq();

        var line1 = MutableList.of(Doc.styled(KEYWORD, "data"),
          linkDef(def.ref(), DATA),
          visitTele(richDataTele),
          Doc.symbol(":"),
          term(Outer.Free, def.result));
        yield Doc.vcat(Doc.sepNonEmpty(line1),
          Doc.nest(2, Doc.vcat(def.body.view().map(ctor ->
            // we need to instantiate the tele of ctor, but we can't modify the CtorDef
            visitCtor(ctor.ref, enrich(ctor.selfTele.mapIndexed((i, p) -> {
              // i: nth param
              // p: the param
              // instantiate reference to data tele
              return p.map(t -> t.replaceAllFrom(i, reversedRichDataTele));
            })), ctor.coerce)))));
      }
    };
  }

  private @NotNull Doc visitCtor(
    @NotNull DefVar<? extends CtorDef, ? extends TeleDecl.DataCtor> ref,
    @NotNull ImmutableSeq<ParamLike<Term>> richSelfTele,
    boolean coerce
  ) {
    var doc = Doc.sepNonEmpty(coe(coerce),
      linkDef(ref, CON),
      visitTele(richSelfTele));
    Doc line1;
    // if (ctor.pats.isNotEmpty()) {
    //   var pats = Doc.commaList(ctor.pats.view().map(pat -> pat(pat, Outer.Free)));
    //   line1 = Doc.sep(Doc.symbol("|"), pats, Doc.symbol("=>"), doc);
    // } else {
    line1 = Doc.sep(Tokens.BAR, doc);
    // }
    return Doc.cblock(line1, 2, Doc.empty() /*partial(options, ctor.clauses, false, Doc.empty(), Doc.empty())*/);
  }

  private @NotNull Doc visitClauses(
    @NotNull ImmutableSeq<Term.Matching> clauses,
    @NotNull SeqView<Term> teleSubst
  ) {
    // TODO: subset clause body with [teleSubst]
    return Doc.vcat(clauses.view().map(matching ->
      Doc.sep(Tokens.BAR, matching.toDoc(options))));
  }

  /// region Name Generating

  private record CoreParam(
    @Override @NotNull LocalVar ref,
    @Override @NotNull Term type
  ) implements ParamLike<Term> {
    @Override
    public boolean explicit() {
      return true;
    }
  }

  private @NotNull ImmutableSeq<ParamLike<Term>> enrich(@NotNull SeqLike<Param> tele) {
    var richTele = MutableList.<ParamLike<Term>>create();

    for (var param : tele) {
      var freeTy = param.type().instantiateTele(richTele.view()
        .map(x -> new FreeTerm(x.ref())));
      richTele.append(new CoreParam(new LocalVar(param.name(), SourcePos.SER), freeTy));
    }

    return richTele.toImmutableSeq();
  }

  /**
   * Generate human friendly names for {@param tele}
   *
   * @return a {@link ParamLike} telescope
   * @apiNote remember to instantiate body with corresponding {@link FreeTerm}
   */
  private @NotNull ImmutableSeq<ParamLike<Term>> generateNames(
    @NotNull ImmutableSeq<Term> tele
  ) {
    var richTele = MutableList.<ParamLike<Term>>create();
    for (var param : tele) {
      var freeTy = param.instantiateTele(richTele.view()    // mutable view!!😱
        .map(x -> new FreeTerm(x.ref())));
      // perhaps we can obtain the whnf of ty as the name
      richTele.append(new CoreParam(generateName(null), freeTy));
    }

    return richTele.toImmutableSeq();
  }

  // used for lambda
  private @NotNull ImmutableSeq<LocalVar> generateNames(int count) {
    return ImmutableSeq.fill(count, () -> generateName(null));
  }

  private int nextId() {
    return genId++;
  }

  private @NotNull LocalVar generateName(@Nullable String typeName) {
    var name = typeName == null ? STR."_\{nextId()}" : STR."_\{typeName}_\{nextId()}";
    return new LocalVar(name, SourcePos.SER, GenerateKind.Anonymous.INSTANCE);
  }

  /// endregion Name Generating
}
