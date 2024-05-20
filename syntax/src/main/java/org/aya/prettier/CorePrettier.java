// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.prettier;

import kala.collection.SeqLike;
import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import org.aya.generic.AyaDocile;
import org.aya.generic.NameGenerator;
import org.aya.generic.ParamLike;
import org.aya.pretty.doc.Doc;
import org.aya.syntax.compile.Compiled;
import org.aya.syntax.concrete.stmt.decl.TeleDecl;
import org.aya.syntax.core.def.*;
import org.aya.syntax.core.pat.Pat;
import org.aya.syntax.core.pat.PatToTerm;
import org.aya.syntax.core.repr.CodeShape;
import org.aya.syntax.core.term.*;
import org.aya.syntax.core.term.call.*;
import org.aya.syntax.core.term.repr.IntegerTerm;
import org.aya.syntax.core.term.repr.MetaLitTerm;
import org.aya.syntax.core.term.xtt.*;
import org.aya.syntax.ref.DefVar;
import org.aya.syntax.ref.GenerateKind;
import org.aya.syntax.ref.LocalVar;
import org.aya.util.Arg;
import org.aya.util.error.SourcePos;
import org.aya.util.prettier.PrettierOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.UnaryOperator;

import static org.aya.prettier.Tokens.*;

/**
 * It's the pretty printer.
 * Credit after <a href="https://github.com/jonsterling/dreamtt/blob/main/frontend/Distiller.ml">Jon Sterling</a>
 *
 * @author ice1000, kiva
 * @see ConcretePrettier
 */
public class CorePrettier extends BasePrettier<Term> {
  private final NameGenerator nameGen = new NameGenerator();

  public CorePrettier(@NotNull PrettierOptions options) {
    super(options);
  }

  @Override public @NotNull Doc term(@NotNull Outer outer, @NotNull Term preterm) {
    return switch (preterm) {
      case FreeTerm(var var) -> varDoc(var);
      case LocalTerm(var idx) -> Doc.plain(STR."^\{idx}");
      case MetaCall term -> {
        var name = term.ref();
        var inner = Doc.cat(Doc.plain("?"), varDoc(name));
        Function<Outer, Doc> factory = o -> visitCoreApp(null, inner, term.args().view(), o, optionImplicit());
        if (options.map.get(AyaPrettierOptions.Key.InlineMetas)) yield factory.apply(outer);
        yield Doc.wrap(HOLE_LEFT, HOLE_RIGHT, factory.apply(Outer.Free));
      }
      case MetaLitTerm lit ->
        lit.repr() instanceof AyaDocile docile ? docile.toDoc(options) : Doc.plain(lit.repr().toString());
      case TupTerm(var items) -> Doc.parened(argsDoc(options, items.view().map(Arg::ofExplicitly)));
      case IntegerTerm shaped -> shaped.repr() == 0
        ? linkLit(0, shaped.ctorRef(CodeShape.GlobalId.ZERO), CON)
        : linkLit(shaped.repr(), shaped.ctorRef(CodeShape.GlobalId.SUC), CON);
      case ConCallLike conCall -> visitCoreCalls(conCall.ref(), conCall.conArgs(), outer, optionImplicit());
      case FnCall fnCall -> visitCoreCalls(fnCall.ref(), fnCall.args(), outer, optionImplicit());
      case SigmaTerm(var params) -> {
        var tele = generateNames(params.dropLast(1));
        var last = params.getLast().instantiateTele(tele.view().map(p -> new FreeTerm(p.ref())));
        var doc = Doc.sep(
          KW_SIGMA,
          visitTele(tele, last, FindUsage::free),
          SIGMA_RESULT,
          justType(Arg.ofExplicitly(last), Outer.Codomain)
        );
        // Same as Pi
        yield checkParen(outer, doc, Outer.BinOp);
      }
      case LamTerm lam -> {
        var pair = LamTerm.unwrap(lam);
        var params = generateNames(pair.component1()).view();
        var paramRef = params.<Term>map(FreeTerm::new);
        var body = pair.component2().instantiateTele(paramRef);
        Doc bodyDoc;
        // Syntactic eta-contraction
        if (body instanceof Callable call && call.ref() instanceof DefVar<?, ?> defVar) {
          var args = visibleArgsOf(call).view();
          while (params.isNotEmpty() && args.isNotEmpty()) {
            if (checkUneta(args, params.getLast())) {
              args = args.dropLast(1);
              params = params.dropLast(1);
            } else break;
          }
          // if (call instanceof FieldTerm access) bodyDoc = visitAccessHead(access);
          // else {
          bodyDoc = visitCoreCalls(defVar, args,
            params.isEmpty() ? outer : Outer.Free,
            optionImplicit());
          // }
        } else bodyDoc = term(Outer.Free, body);

        if (params.isEmpty()) yield bodyDoc;

        var list = MutableList.of(LAMBDA);
        params.forEach(param -> list.append(Doc.bracedUnless(linkDef(param), true)));
        list.append(FN_DEFINED_AS);
        list.append(bodyDoc);
        var doc = Doc.sep(list);
        yield checkParen(outer, doc, Outer.BinOp);
      }
      case SortTerm(var kind, var lift) -> {
        var fn = Doc.styled(KEYWORD, kind.name());
        if (!kind.hasLevel()) yield fn;
        yield visitCalls(null, fn, (_, t) -> t.toDoc(options), outer,
          SeqView.of(new Arg<>(_ -> Doc.plain(String.valueOf(lift)), true)),
          optionImplicit()
        );
      }
      case DimTyTerm _ -> KW_INTERVAL;
      // case NewTerm(var inner) -> visitCalls(null, Doc.styled(KEYWORD, "new"), (nest, t) -> t.toDoc(options), outer,
      //   SeqView.of(new Arg<>(o -> term(Outer.AppSpine, inner), true)),
      //   options.map.get(AyaPrettierOptions.Key.ShowImplicitArgs)
      // );
      // case FieldTerm term -> visitCalls(null, visitAccessHead(term), term.args().view(), outer,
      //   options.map.get(AyaPrettierOptions.Key.ShowImplicitArgs));
      case MetaPatTerm(var ref) -> {
        if (ref.solution().get() == null) yield varDoc(generateName(null));
        yield Doc.wrap(META_LEFT, META_RIGHT, pat(ref, true, outer));
      }
      case ErrorTerm(var desc, var isReallyError) -> {
        var doc = desc.toDoc(options);
        yield isReallyError ? Doc.angled(doc) : doc;
      }
      case AppTerm app -> {
        var pair = AppTerm.unapp(app);
        var args = pair.args();
        var head = pair.fun();
        // if (head instanceof RefTerm.Field fieldRef) yield visitArgsCalls(fieldRef.ref(), MEMBER, args, outer);
        var implicits = optionImplicit();
        // Infix def-calls
        if (head instanceof Callable call && call.ref() instanceof DefVar<?, ?> var) {
          yield visitCoreCalls(var, call.args().view().appendedAll(args), outer, implicits);
        }
        yield visitCoreApp(null, term(Outer.AppHead, head), args.view(), outer, implicits);
      }
      case PrimCall prim -> visitCoreCalls(prim.ref(), prim.args(), outer, optionImplicit());
      // case RefTerm.Field term -> linkRef(term.ref(), MEMBER);
      case ProjTerm(var of, var ix) -> Doc.cat(term(Outer.ProjHead, of), PROJ, Doc.plain(String.valueOf(ix)));
      // case MatchTerm match -> Doc.cblock(Doc.sep(Doc.styled(KEYWORD, "match"),
      //     Doc.commaList(match.discriminant().map(t -> term(Outer.Free, t)))), 2,
      //   Doc.vcat(match.clauses().view()
      //     .map(clause -> Doc.sep(Doc.symbol("|"),
      //       Doc.commaList(clause.patterns().map(p -> pat(p, Outer.Free))),
      //       Doc.symbol("=>"), term(Outer.Free, clause.body())))
      //     .toImmutableSeq()));
      case PiTerm piTerm -> {
        // Try to omit the Pi keyword
        if (FindUsage.bound(piTerm.body(), 0) == 0) yield checkParen(outer, Doc.sep(
          justType(Arg.ofExplicitly(piTerm.param()), Outer.BinOp),
          ARROW,
          term(Outer.Codomain, piTerm.body())
        ), Outer.BinOp);
        var pair = PiTerm.unpi(piTerm, UnaryOperator.identity());
        var params = generateNames(pair.params());
        var body = pair.body().instantiateTele(params.view().map(x -> new FreeTerm(x.ref())));
        var doc = Doc.sep(
          KW_PI,
          visitTele(params, body, FindUsage::free),
          ARROW,
          term(Outer.Codomain, body)
        );
        // Add paren when it's not free or a codomain
        yield checkParen(outer, doc, Outer.BinOp);
      }
      // case ClassCall classCall -> visitArgsCalls(classCall.ref(), CLAZZ, classCall.orderedArgs(), outer);
      case DataCall dataCall -> visitCoreCalls(dataCall.ref(), dataCall.args(), outer, optionImplicit());
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
      case PAppTerm app -> visitCalls(null, term(Outer.AppHead, app.fun()),
        SeqView.of(new Arg<>(app.arg(), true)), outer, optionImplicit());
      case CoeTerm(var ty, var r, var s) -> visitCalls(null,
        KW_COE,
        ImmutableSeq.of(r, s, ty).view().map(t -> new Arg<>(t, true)),
        outer, true);
      // case HCompTerm hComp -> throw new InternalException("TODO");
      case DimTerm dim -> Doc.styled(KEYWORD, switch (dim) {
        case I0 -> "0";
        case I1 -> "1";
      });
      // TODO: in case we want to show implicits, display the type
      case EqTerm(var _, var a, var b) -> {
        var doc = Doc.sep(term(Outer.BinOp, a), EQ, term(Outer.BinOp, b));
        yield checkParen(outer, doc, Outer.BinOp);
      }
      case Compiled _ -> Doc.plain("<TODO: pretty print compiled terms>");
    };
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
        yield sol != null ? pat(sol, licit, outer)
          : Doc.bracedUnless(linkDef(generateName(meta.type())), licit);
      }
      case Pat.Bind bind -> Doc.bracedUnless(linkDef(bind.bind()), licit);
      case Pat.Con con -> {
        var conDoc = visitCoreCalls(con.ref(), con.args().view().map(PatToTerm::visit), outer,
          optionImplicit());
        yield ctorDoc(outer, licit, conDoc, con.args().isEmpty());
      }
      case Pat.Absurd _ -> Doc.bracedUnless(PAT_ABSURD, licit);
      case Pat.Tuple tuple -> Doc.licit(licit,
        Doc.commaList(tuple.elements().view().map(sub -> pat(sub, true, Outer.Free))));
      case Pat.ShapedInt lit -> Doc.bracedUnless(lit.repr() == 0
          ? linkLit(0, lit.ctorRef(CodeShape.GlobalId.ZERO), CON)
          : linkLit(lit.repr(), lit.ctorRef(CodeShape.GlobalId.SUC), CON),
        licit);
    };
  }

  public @NotNull Doc def(@NotNull Def predef) {
    return switch (predef) {
      case PrimDef def -> primDoc(def.ref());
      case FnDef def -> {
        var line1 = MutableList.of(KW_DEF);
        def.modifiers.forEach(m -> line1.append(Doc.styled(KEYWORD, m.keyword)));
        var tele = enrich(def.telescope());
        var subst = tele.view().<Term>map(p -> new FreeTerm(p.ref()));
        line1.appendAll(new Doc[]{
          defVar(def.ref()),
          visitTele(tele),
          HAS_TYPE,
          term(Outer.Free, def.result)
        });
        var line1sep = Doc.sepNonEmpty(line1);
        yield def.body.fold(
          term -> Doc.sep(line1sep, FN_DEFINED_AS, term(Outer.Free, term.instantiateTele(subst))),
          clauses -> Doc.vcat(line1sep, Doc.nest(2, visitClauses(clauses, subst))));
      }
      // case MemberDef field -> Doc.sepNonEmpty(Doc.symbol("|"),
      //   coe(field.coerce),
      //   linkDef(field.ref(), MEMBER),
      //   visitTele(field.telescope),
      //   Doc.symbol(":"),
      //   term(Outer.Free, field.result));
      case ConDef con -> {
        var doc = Doc.sepNonEmpty(coe(con.coerce),
          defVar(con.ref()),
          visitTele(enrich(con.selfTele)));
        Doc line1;
        if (con.pats.isNotEmpty()) {
          var pats = Doc.commaList(con.pats.view().map(pat -> pat(pat, true, Outer.Free)));
          line1 = Doc.sep(Doc.symbol("|"), pats, Doc.symbol("=>"), doc);
        } else {
          line1 = Doc.sep(BAR, doc);
        }
        yield Doc.cblock(line1, 2, Doc.empty() /*partial(options, con.clauses, false, Doc.empty(), Doc.empty())*/);
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

        var line1 = MutableList.of(KW_DATA,
          defVar(def.ref()),
          visitTele(richDataTele),
          HAS_TYPE,
          term(Outer.Free, def.result));
        var cons = def.body.view().map(con ->
          // we need to instantiate the tele of con, but we can't modify the CtorDef
          visitCon(con.ref, enrich(con.selfTele.mapIndexed((i, p) -> {
            // i: nth param
            // p: the param
            // instantiate reference to data tele
            return p.descent(t -> t.replaceAllFrom(i, reversedRichDataTele));
          })), con.coerce));

        yield Doc.vcat(Doc.sepNonEmpty(line1),
          Doc.nest(2, Doc.vcat(cons)));
      }
    };
  }

  private @NotNull Doc visitCon(
    @NotNull DefVar<? extends ConDef, ? extends TeleDecl.DataCon> ref,
    @NotNull ImmutableSeq<ParamLike<Term>> richSelfTele,
    boolean coerce
  ) {
    var doc = Doc.sepNonEmpty(coe(coerce), defVar(ref), visitTele(richSelfTele));
    Doc line1;
    // if (ctor.pats.isNotEmpty()) {
    //   var pats = Doc.commaList(ctor.pats.view().map(pat -> pat(pat, Outer.Free)));
    //   line1 = Doc.sep(Doc.symbol("|"), pats, Doc.symbol("=>"), doc);
    // } else {
    line1 = Doc.sep(BAR, doc);
    // }
    return Doc.cblock(line1, 2, Doc.empty() /*partial(options, ctor.clauses, false, Doc.empty(), Doc.empty())*/);
  }

  private @NotNull Doc visitClauses(
    @NotNull ImmutableSeq<Term.Matching> clauses,
    @NotNull SeqView<Term> teleSubst
  ) {
    // TODO: subset clause body with [teleSubst]
    return Doc.vcat(clauses.view().map(matching ->
      // TODO: toDoc use a new CorePrettier => new NameGenerator
      Doc.sep(BAR, matching.toDoc(options))));
  }

  public @NotNull Doc visitParam(@NotNull Param param, @NotNull Outer outer) {
    return justType(
      new CoreParam(new LocalVar(param.name(), SourcePos.SER, GenerateKind.Basic.Tyck), param.type()),
      outer);
  }

  /// region Name Generating
  private record CoreParam(
    @Override @NotNull LocalVar ref,
    @Override @NotNull Term type
  ) implements ParamLike<Term> {
    @Override public boolean explicit() { return true; }

    @Override public @NotNull ParamLike<Term> map(@NotNull UnaryOperator<Term> mapper) {
      return new CoreParam(ref, mapper.apply(type));
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
      // ice: just use freeTy I think it's ok
      richTele.append(new CoreParam(generateName(freeTy), freeTy));
    }

    return richTele.toImmutableSeq();
  }

  // used for lambda
  private @NotNull ImmutableSeq<LocalVar> generateNames(int count) {
    return ImmutableSeq.fill(count, () -> generateName(null));
  }

  private @NotNull LocalVar generateName(@Nullable Term whty) {
    return new LocalVar(nameGen.next(whty), SourcePos.SER);
  }
  /// endregion Name Generating
}
