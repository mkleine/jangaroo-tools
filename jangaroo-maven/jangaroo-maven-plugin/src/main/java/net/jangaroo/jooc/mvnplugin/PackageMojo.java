package net.jangaroo.jooc.mvnplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.mojo.javascript.archive.Types;
import org.codehaus.plexus.archiver.ArchiveFileFilter;
import org.codehaus.plexus.archiver.ArchiveFilterException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collections;

/**
 * Creates the jangaroo archive and attaches them to the project.<br>
 * The jangaroo archive is created by zipping the <code>${project.build.outputDirectory}</code>
 * and ommiting all ActionScript files.
 * <p/>
 * The <code>package</code> goal is executed in the <code>package</code> phase of the jangaroo lifecycle.
 *
 * @goal package
 * @phase package
 */
public class PackageMojo extends AbstractMojo {
  /**
   * The maven project.
   *
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;

  /**
   * Destination directory for the Maven artifacts (*.jar). Default is <code>
   * ${project.build.directory}</code>
   *
   * @parameter expression="${project.build.directory}"
   */
  private File targetDir;

  /**
   * @component
   */
  MavenProjectHelper projectHelper;

  /**
   * List of files to exclude. Specified as fileset patterns.
   *
   * @parameter
   */
  private String[] excludes;

  /**
   * The filename of the merged javascript file generated by the Jooc. Defaults to ${project.artifactId}
   *
   * @parameter default-value="${project.build.finalName}"
   * "
   */
  private String finalName;

  /**
   * Plexus archiver.
   *
   * @component role="org.codehaus.plexus.archiver.Archiver" role-hint="jar"
   * @required
   */
  private JarArchiver archiver;

  /**
   * @parameter
   */
  private File manifest;

  /**
   * Location of the compiled scripts files. Defaults to ${project.build.directory}/joo/
   *
   * @parameter expression="${project.build.directory}/joo/"
   */
  private File compilerOutputDirectory;

  public void execute()
      throws MojoExecutionException {
    File jsarchive = new File(targetDir, finalName + "." + Types.JAVASCRIPT_EXTENSION);
    try {
      if (manifest != null) {
        archiver.setManifest(manifest);
      } else {
        createDefaultManifest(project, archiver);
      }
      archiver.setArchiveFilters(Collections.singletonList(new PackagerArchiveFilter()));
      if (compilerOutputDirectory.exists()) {
        archiver.addDirectory(compilerOutputDirectory);
      }

      String groupId = project.getGroupId();
      String artifactId = project.getArtifactId();
      archiver.addFile(project.getFile(), "META-INF/maven/" + groupId + "/" + artifactId
          + "/pom.xml");
      archiver.setDestFile(jsarchive);
      archiver.createArchive();
      archiver.reset();
    }
    catch (Exception e) {
      throw new MojoExecutionException("Failed to create the javascript archive", e);
    }
    project.getArtifact().setFile(jsarchive);

  }

  private static void createDefaultManifest(MavenProject project, JarArchiver jarArchiver)
      throws ManifestException, IOException, ArchiverException {
    Manifest manifest = new Manifest();
    Manifest.Attribute attr = new Manifest.Attribute("Created-By", "Apache Maven");
    manifest.addConfiguredAttribute(attr);
    attr = new Manifest.Attribute("Implementation-Title", project.getName());
    manifest.addConfiguredAttribute(attr);
    attr = new Manifest.Attribute("Implementation-Version", project.getVersion());
    manifest.addConfiguredAttribute(attr);
    attr = new Manifest.Attribute("Implementation-Vendor-Id", project.getGroupId());
    manifest.addConfiguredAttribute(attr);
    if (project.getOrganization() != null) {
      String vendor = project.getOrganization().getName();
      attr = new Manifest.Attribute("Implementation-Vendor", vendor);
      manifest.addConfiguredAttribute(attr);
    }
    attr = new Manifest.Attribute("Built-By", System.getProperty("user.name"));
    manifest.addConfiguredAttribute(attr);

    File mf = File.createTempFile("maven", ".mf");
    mf.deleteOnExit();
    PrintWriter writer = null;
    try {
      writer = new PrintWriter(new FileWriter(mf));
      manifest.write(writer);
    } finally {
      if (writer != null) {
        writer.close();
      }
    }
    jarArchiver.setManifest(mf);
  }

  private static class PackagerArchiveFilter implements ArchiveFileFilter {
    @Override
    public boolean include(InputStream dataStream, String entryName) throws ArchiveFilterException {
      return !entryName.endsWith(".as");
    }

  }
}