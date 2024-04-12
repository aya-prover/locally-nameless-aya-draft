// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.prettier;

import org.aya.pretty.doc.Doc;

import static org.aya.prettier.BasePrettier.*;

public final class Tokens {

  private Tokens() {
  }

  public static final Doc LAMBDA = Doc.symbol("\\");
  public static final Doc ARROW = Doc.symbol("->");
  public static final Doc LARROW = Doc.symbol("<-");
  public static final Doc FN_DEFINED_AS = Doc.symbol("=>");
  public static final Doc DEFINED_AS = Doc.symbol(":=");
  public static final Doc HOLE = Doc.symbol("{??}");
  public static final Doc HOLE_LEFT = Doc.symbol("{?");
  public static final Doc HOLE_RIGHT = Doc.symbol("?}");
  public static final Doc BAR = Doc.symbol("|");
  public static final Doc COLON = Doc.symbol(":");
  public static final Doc DOT = Doc.symbol(".");
  public static final Doc SIGMA_RESULT = Doc.styled(KEYWORD, "**");

  public static final Doc KW_DO = Doc.styled(KEYWORD, "do");
  public static final Doc KW_TIGHTER = Doc.styled(KEYWORD, "tighter");
  public static final Doc KW_AS = Doc.styled(KEYWORD, "as");
  public static final Doc KW_SIGMA = Doc.styled(KEYWORD, "Sig");
  public static final Doc KW_PI = Doc.styled(KEYWORD, "Fn");
}
