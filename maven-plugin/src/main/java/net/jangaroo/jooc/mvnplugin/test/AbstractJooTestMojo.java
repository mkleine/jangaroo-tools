package net.jangaroo.jooc.mvnplugin.test;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: sannies
 * Date: 15.09.2009
 * Time: 20:04:43
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractJooTestMojo extends AbstractMojo {
  /**
   * Classname of the Actionscript TestSuite that will start all tests.
   *
   * @parameter default-value="suite.TestSuite"
   */
  protected String testSuiteName;

  /**
   * The maven project.
   *
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  protected MavenProject project;

  /**
   * Output directory for the janagroo artifact  unarchiver. All jangaroo dependencies will be unpacked into
   * this directory.
   *
   * @parameter expression="${project.build.testOutputDirectory}"  default-value="${project.build.testOutputDirectory}"
   * @required
   */
  protected File testOutputDirectory;

  /**
   * Source directory to scan for files to compile.
   *
   * @parameter expression="${project.build.testSourceDirectory}"
   */
  protected File testSourceDirectory;


  protected boolean isTestAvailable() {
    File testSuite = new File(testSourceDirectory, testSuiteName.replace(".", File.separator) + ".as");
    if (!testSuite.exists()) {
      getLog().info("The testSuite '" + testSuite + "' could not be found. Skipping.");
    }
    return testSuite.exists();
  }
}