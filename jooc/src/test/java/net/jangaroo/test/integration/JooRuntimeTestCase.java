/*
 *   Copyright (c) 2003 CoreMedia AG, Hamburg. All rights reserved.
 */

package net.jangaroo.test.integration;

import org.mozilla.javascript.*;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;

import net.jangaroo.jooc.Jooc;
import net.jangaroo.test.JooTestCase;

/**
 * A JooTestCase to be executed at runtime
 *
 * This class adds a global evaluation context and methods to load and execute script code.
 *
 * @author Andreas Gawecki
 */
public abstract class JooRuntimeTestCase extends JooTestCase {

  protected Context cx;
  protected Scriptable scope;
  protected ScriptableObject global;
  private static final String CLASS_JS_FILE_PATH =
    Jooc.CLASS_PACKAGE_NAME + File.separatorChar + Jooc.CLASS_CLASS_NAME + Jooc.JS_SUFFIX;

  public JooRuntimeTestCase(String name) {
    super(name);
  }

  static public class Global extends ScriptableObject {

    public String getClassName() {
       return "global";
    }

    static public void print(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
       for (int i=0; i < args.length; i++) {
         if (i > 0)
             System.out.print(" ");
          // Convert the arbitrary JavaScript value into a string form.
         String s = Context.toString(args[i]);
         System.out.print(s);
       }
       System.out.println();
    }
  }

  protected void setUp() throws Exception {
    super.setUp();
    global = new Global();
    cx = ContextFactory.getGlobal().enterContext();
    cx.setLanguageVersion(Context.VERSION_1_5);
    scope = cx.initStandardObjects(global);
    global.defineFunctionProperties(new String[]{ "print" },
            Global.class, ScriptableObject.DONTENUM);
    load(CLASS_JS_FILE_PATH);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    Context.exit();
  }

  protected void load(String jsFileName) throws Exception {
    load(new File(destinationDir, jsFileName));
  }

  protected void printJsResult(Object result) throws Exception {
    System.out.println(cx.toString(result) + " (" + result.getClass().getName() + ")");
  }

  protected Object load(File jsFile) throws Exception {
    String jsFileName = jsFile.getAbsolutePath();
    System.out.println("loading script " + jsFileName);
    FileReader reader = new FileReader(jsFile);
    Object result = cx.evaluateReader(scope, reader, jsFileName, 1, null);
    printJsResult(result);
    // Convert the result to a string and print it.
    System.out.println(cx.toString(result));
    return result;
  }

  protected void loadClass(String qualifiedJooClassName) throws Exception {
    String jsFileName = qualifiedJooClassName.replace('.',File.separatorChar) + Jooc.JS_SUFFIX;
    load(jsFileName);
  }

  protected void initClass(String qualifiedJooClassName) throws Exception {
    eval(Jooc.CLASS_FULLY_QUALIFIED_NAME + ".init("+qualifiedJooClassName+")");
  }

  protected void runClass(String qualifiedJooClassName) throws Exception {
    loadClass(qualifiedJooClassName);
    initClass(qualifiedJooClassName);
    eval(qualifiedJooClassName+".main()");
  }

  protected Object eval(String script) throws Exception {
    System.out.print("evaluating script '" + script + "': ");
    Reader reader = new StringReader(script);
    try {
      Object result = cx.evaluateReader(scope, reader, "", 1, null);
      // Convert the result to a string and print it.
      printJsResult(result);
      return result;
    } catch (Exception e) {
      System.out.println("failed: Exception " + e.getClass().getName() + ": " + e.getMessage());
      throw e;
    } catch (Error e) {
      System.out.println("failed: Error " + e.getClass().getName() + ": " + e.getMessage());
      throw e;
    }
  }

  protected void expectString(String expected, String script) throws Exception {
    Object result = eval(script);
    String actual = null;
    if (result instanceof String)
      actual = (String)result;
    else fail("expected string result, found: " + result.getClass().getName());
    if (!expected.equals(actual)) {
      if (actual.length() == expected.length()) {
        int i = 0;
        while (actual.charAt(i) == expected.charAt(i))
          i++;
        fail("expected: \"" + expected + "\", found: \"" + actual + "\"\nstrings differ at index " + i + "");
      } else
        fail("expected length " + expected.length() + ": \"" + expected +
            "\", found length " + actual.length() + ": \"" + actual + "\"");
    }
  }

  protected void expectNumber(double expected, String script) throws Exception {
    Object result = eval(script);
    double actual = 0;
    if (result instanceof Number)
      actual = ((Number)result).doubleValue();
    else fail("expected numeric result, found: " + result.getClass().getName());
    assertEquals(expected, actual, 0.00000000001);
  }

  public void testRhino() throws Exception {
    expectNumber(23, "23");
    expectNumber(23.1, "22.1+1");
  }

}