package net.jangaroo.extxml;

import freemarker.template.TemplateException;
import net.sf.saxon.s9api.SaxonApiException;
import org.apache.maven.shared.model.fileset.mappers.MapperException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Ext JS Component XML to Action Script (Jangaroo) transformer.
 * <p>Usage: src2xsd <i>input-dir</i> <i>output-dir</i> <i>imported-xsd-file-name*</i>
 */
public class XML2Joo {
  public static void main(String[] args) throws IOException, TemplateException, SaxonApiException, SAXException, XPathExpressionException, ParserConfigurationException, MapperException {
    List<File> importedXsds = new ArrayList<File>(args.length - 2);
    for (int i = 2; i < args.length; i++) {
      importedXsds.add(new File(args[i]));
    }
    ComponentSuites componentSuites = new ComponentSuites(importedXsds);
    new JooClassGenerator(componentSuites).transformFiles(new File(args[0]), new File(args[1]));
  }
}