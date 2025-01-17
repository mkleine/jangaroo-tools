package net.jangaroo.exml.mojo;

import net.jangaroo.exml.compiler.Exmlc;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Mojo that generates the Xsd files for the module. Needs the same information as the exml mojo
 *
 * @goal exml-xsd
 * @phase generate-resources
 * @requiresDependencyResolution
 * @threadSafe
 */
public class ExmlXsdMojo extends ExmlMojo {

  /**
   * @component
   */
  private MavenProjectHelper projectHelper;

  @Override
  protected List<File> getSourcePath() {
    List<File> sourcePath = new ArrayList<File>(super.getSourcePath());
    sourcePath.add(getGeneratedSourcesDirectory());
    return Collections.unmodifiableList(sourcePath);
  }

  @Override
  protected void executeExmlc(Exmlc exmlc) {
    //generate the XSD for that
    File xsdFile = exmlc.generateXsd();
    projectHelper.attachArtifact(getProject(), "xsd", xsdFile);
    getLog().info("xsd-file '" + xsdFile + "' generated.");

    // add target/generated-resources to project's resources so XSDs are always packaged:
    Resource generatedResources = new Resource();
    generatedResources.setDirectory(getGeneratedResourcesDirectory().getPath());
    getProject().addResource(generatedResources);
    getLog().info("added project resource '" + generatedResources + ".");
  }
}
