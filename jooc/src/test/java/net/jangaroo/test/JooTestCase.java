/*
 *   Copyright (c) 2003 CoreMedia AG, Hamburg. All rights reserved.
 */

package net.jangaroo.test;

import net.jangaroo.jooc.Jooc;
import junit.framework.TestCase;

import java.io.File;

/**
 * An abstract base class for Jangaroo Compiler tests. Contains methods to compile JangarooScript
 * source files.
 * 
 * @author Andreas Gawecki
 */
public abstract class JooTestCase extends TestCase {

  public JooTestCase(String name) {
    super(name);
  }

  protected boolean debug = false;
  protected String sourceDir = null;
  protected String destinationDir = null;

  protected String getProperty(String name, String defaultValue) {
    return System.getProperty(name, defaultValue);
  }

  protected void setUp() throws Exception {
    sourceDir = getProperty("net.jangaroo.jooc.test.source", ".");
    destinationDir = getProperty("net.jangaroo.jooc.test.destination", null);
  }

  protected String[] concat(String[] arr1, String[] arr2) {
    String[] result = new String[arr1.length + arr2.length];
    System.arraycopy(arr1, 0, result, 0, arr1.length);
    System.arraycopy(arr2, 0, result, arr1.length, arr2.length);
    return result;
  }

  protected String[] concat(String s, String[] arr2) {
    return concat(new String[]{s}, arr2);
  }

  protected String toString(String[] arr) {
    String result = "";
    for (int i = 0; i < arr.length; i++) {
      if (i > 0)
        result += " ";
      String s = arr[i];
      result += s;
    }
    return result;
  }

  protected String[] copy(String[] arr) {
    String[] result = new String[arr.length];
    System.arraycopy(arr, 0, result, 0, arr.length);
    return result;
  }

  protected String[] prependSourceDir(String[] fileNames) {
    String[] result = copy(fileNames);
    for (int i = 0; i < result.length; i++) {
      String s = result[i];
      result[i] = (new File(sourceDir, s)).getPath();
    }
    return result;
  }

  protected int runJooc(String[] fileNames) {
    String[] args = prependSourceDir(fileNames);
    if (debug) args = concat("-g", args);
    if (destinationDir != null) args = concat(new String[]{"-d", destinationDir}, args);
    Jooc compiler = new Jooc();
    System.out.println("jooc " + toString(args));
    return compiler.run(args);
  }

  protected int runJooc(String filename) {
    return runJooc(new String[]{filename});
  }

  protected void expectCompileSuccess(String fileName) {
    compile(new String[]{fileName});
  }

  protected void expectCompileResult(int resultCode, String[] fileNames) {
    int actualResult = runJooc(fileNames);
    if (resultCode != actualResult) {
      fail("unexpected compilation result: expected " + resultCode + " (" + Jooc.getResultCodeDescription(resultCode) + "), " +
              "found " + actualResult + " (" + Jooc.getResultCodeDescription(actualResult) + ")");
    }
  }

  protected void compile(String[] fileNames) {
    expectCompileResult(Jooc.RESULT_CODE_OK, fileNames);
  }

  protected void compile(String fileName) {
    compile(new String[] { fileName });
  }

}