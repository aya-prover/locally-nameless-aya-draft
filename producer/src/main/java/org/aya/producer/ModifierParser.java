// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.producer;

import com.intellij.psi.tree.IElementType;
import kala.collection.Seq;
import kala.collection.immutable.ImmutableMap;
import kala.collection.immutable.ImmutableSeq;
import org.aya.syntax.concrete.stmt.Stmt;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.function.Predicate;

import static org.aya.parser.AyaPsiElementTypes.*;
import static org.aya.producer.ModifierParser.ModifierGroup.*;

/**
 * Generalized modifier parser. There are two assumptions in this parser:
 * 1. Availability. Whether a modifier is usable (can occur) in a declaration.
 * 2. Presence. Whether a modifier is present (is specified) in a declaration.
 *
 * @author Hoshino Tened
 */
public record ModifierParser(@NotNull Reporter reporter) {
  public enum ModifierGroup {
    None,
    Accessibility,
    Alpha
  }

  public enum Modifier {
    // Common Modifiers
    Private(KW_PRIVATE, Accessibility, "private"),

    // ModuleLike Modifiers
    Open(OPEN_KW, None, "open"),

    // Function Modifiers
    Opaque(KW_OPAQUE, Alpha, "opaque"),
    Inline(KW_INLINE, Alpha, "inline"),
    Overlap(KW_OVERLAP, None, "overlap");

    public final @NotNull IElementType type;
    public final @NotNull ModifierGroup group;
    public final @NotNull String keyword;

    /**
     * {@code implies} will/should expand only once
     */
    public final @NotNull Modifier[] implies;

    Modifier(
      @NotNull IElementType type, @NotNull ModifierGroup group,
      @NotNull String keyword, @NotNull Modifier @NotNull ... implies
    ) {
      this.type = type;
      this.group = group;
      this.keyword = keyword;
      this.implies = implies;
    }
  }

  /***
   * For different declarations we have different modifiers that are available.
   * @param defaultMods If a modifier is available, but not present in the declaration, we use the default value from here.
   * @param available Checks if a modifier is available.
   */
  public record Filter(
    @NotNull Modifiers defaultMods,
    @NotNull Predicate<Modifier> available
  ) {
    public @NotNull Filter and(@NotNull Predicate<Modifier> and) {
      return new Filter(defaultMods, available.and(and));
    }

    /**
     * @param defaultAcc Default {@link org.aya.syntax.concrete.stmt.Stmt.Accessibility}
     * @param miscAvail  Available miscellaneous modifiers, see {@link DefaultModifiers#miscAvail}
     */
    public static @NotNull Filter create(
      @NotNull WithPos<Stmt.Accessibility> defaultAcc,
      @NotNull EnumSet<Modifier> miscAvail
    ) {
      return new Filter(new DefaultModifiers(defaultAcc, miscAvail), mod -> switch (mod) {
        case Private -> true;
        case Open, Opaque, Inline, Overlap -> miscAvail.contains(mod);
      });
    }
  }

  /**
   * @param miscAvail Miscellaneous modifiers (open, inline, opaque, overlap) availability map.
   *                  If a modifier is in the map (as the key), it is available.
   */
  record DefaultModifiers(
    @NotNull WithPos<Stmt.Accessibility> accessibility,
    @NotNull EnumSet<Modifier> miscAvail
  ) implements Modifiers {
    @Override public @Nullable SourcePos misc(@NotNull Modifier key) {
      // Do not throw anything here, even the modifier is not available.
      return null; // always not present, because miscAvail only says availability, not presence.
    }
  }

  /** Only "open" is available to (data/struct) decls */
  public static final Filter DECL_FILTER = Filter.create(
    new WithPos<>(SourcePos.NONE, Stmt.Accessibility.Public),
    EnumSet.of(Modifier.Open)
  );

  /** "opaque", "inline" and "overlap" is available to functions. */
  public static final Filter FN_FILTER = Filter.create(
    new WithPos<>(SourcePos.NONE, Stmt.Accessibility.Public),
    EnumSet.of(Modifier.Opaque, Modifier.Inline, Modifier.Overlap));

  /** nothing is available to sub-level decls (ctor/field). */
  public static final Filter SUBDECL_FILTER = Filter.create(
    new WithPos<>(SourcePos.NONE, Stmt.Accessibility.Public),
    EnumSet.noneOf(Modifier.class)
  ).and(_ -> false);

  /** All parsed modifiers */
  public interface Modifiers {
    @Contract(pure = true) @NotNull WithPos<Stmt.Accessibility> accessibility();
    /**
     * Miscellaneous modifiers are function modifiers ({@link org.aya.generic.Modifier}) plus "open".
     *
     * @return non-null source position if the modifier is present.
     */
    @Contract(pure = true) @Nullable SourcePos misc(@NotNull Modifier key);
    default @NotNull EnumSet<org.aya.generic.Modifier> toFnModifiers() {
      var fnMods = EnumSet.noneOf(org.aya.generic.Modifier.class);
      if (misc(Modifier.Inline) != null) fnMods.add(org.aya.generic.Modifier.Inline);
      if (misc(Modifier.Opaque) != null) fnMods.add(org.aya.generic.Modifier.Opaque);
      if (misc(Modifier.Overlap) != null) fnMods.add(org.aya.generic.Modifier.Overlap);
      return fnMods;
    }
  }

  private record ModifierSet(
    @NotNull ImmutableMap<Modifier, SourcePos> mods,
    @NotNull Modifiers parent
  ) implements Modifiers {
    @Override @Contract(pure = true) public @NotNull WithPos<Stmt.Accessibility> accessibility() {
      return mods.getOption(Modifier.Private)
        .map(pos -> new WithPos<>(pos, Stmt.Accessibility.Private))
        .getOrElse(parent::accessibility);
    }

    @Override public @Nullable SourcePos misc(@NotNull Modifier key) {
      return mods.getOrElse(key, () -> parent.misc(key));
    }
  }

  private @NotNull ImmutableSeq<WithPos<Modifier>> implication(@NotNull ImmutableSeq<WithPos<Modifier>> modifiers) {
    return modifiers.view()
      .flatMap(modi -> Seq.from(modi.data().implies).map(imply -> new WithPos<>(modi.sourcePos(), imply)))
      .collect(ImmutableMap.collector(WithPos::data, x -> x))
      // ^ distinctBy(WithPos::data)
      .valuesView()
      .toImmutableSeq();
  }

  /**
   * @param filter The filter also performs on the modifiers that expanded from input.
   */
  public @NotNull Modifiers parse(@NotNull ImmutableSeq<WithPos<Modifier>> modifiers, @NotNull Filter filter) {
    EnumMap<ModifierGroup, EnumMap<Modifier, SourcePos>> map = new EnumMap<>(ModifierGroup.class);

    modifiers = implication(modifiers).concat(modifiers);

    // parsing
    for (var data : modifiers) {
      var pos = data.sourcePos();
      var modifier = data.data();

      // do filter
      if (!filter.available.test(data.data())) {
        reportUnsuitableModifier(data);
        continue;
      }

      map.computeIfAbsent(modifier.group, _ -> new EnumMap<>(Modifier.class));
      var exists = map.get(modifier.group);

      if (exists.containsKey(modifier)) {
        reportDuplicatedModifier(data);
        continue;
      }

      if (modifier.group != None
        && !exists.isEmpty()
        // In fact, this boolean expression is always true
        && !exists.containsKey(modifier)) {
        // one (not None) group one modifier
        assert exists.size() == 1;
        var contradict = Seq.from(exists.entrySet()).getFirst();
        reportContradictModifier(data, new WithPos<>(contradict.getValue(), contradict.getKey()));
        continue;
      }

      // no contradict modifier, no redundant modifier, everything is good
      exists.put(modifier, pos);
    }

    return new ModifierSet(ImmutableMap.from(
      ImmutableSeq.from(map.values()).flatMap(EnumMap::entrySet)
    ), filter.defaultMods);
  }

  public void reportUnsuitableModifier(@NotNull WithPos<Modifier> data) {
    reporter.report(new ModifierProblem(data.sourcePos(), data.data(), ModifierProblem.Reason.Inappropriate));
  }

  public void reportDuplicatedModifier(@NotNull WithPos<Modifier> data) {
    reporter.report(new ModifierProblem(data.sourcePos(), data.data(), ModifierProblem.Reason.Duplicative));
  }

  public void reportContradictModifier(@NotNull WithPos<Modifier> current, @NotNull WithPos<Modifier> that) {
    reporter.report(new ModifierProblem(current.sourcePos(), current.data(), ModifierProblem.Reason.Contradictory));
  }
}
