/*
 *   Copyright (c) 2004 CoreMedia AG, Hamburg. All rights reserved.
 */
package net.jangaroo.joodoc;

import com.sun.javadoc.Doc;
import com.sun.javadoc.ParamTag;

public class ParamTagImpl extends TagImpl implements ParamTag {

  private int splitPos;

  public ParamTagImpl(Doc doc, String text) {
    super(doc, "@param", "@param", text);
    splitPos = Util.getIdentifierLength(text);
    //System.out.println("**************\n"+this+"****************\n\n");
  }

  protected String getInlineText() {
    return parameterComment();
  }

  public String parameterComment() {
    return text.substring(splitPos).trim();
  }

  public String parameterName() {
    return text.substring(0,splitPos).trim();
  }

  public String toString() {
    return "@param '"+parameterName()+"': '"+parameterComment()+"'";
  }

  public boolean isTypeParameter() {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }
}