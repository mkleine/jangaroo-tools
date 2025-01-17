package net.jangaroo.exml.model;

import net.jangaroo.exml.api.Exmlc;
import net.jangaroo.exml.api.ExmlcException;
import net.jangaroo.exml.as.ConfigClassBuilder;
import net.jangaroo.exml.config.ExmlConfiguration;
import net.jangaroo.exml.parser.ExmlToConfigClassParser;
import net.jangaroo.jooc.JangarooParser;
import net.jangaroo.jooc.Jooc;
import net.jangaroo.jooc.StdOutCompileLog;
import net.jangaroo.jooc.ast.CompilationUnit;
import net.jangaroo.jooc.config.ParserOptions;
import net.jangaroo.jooc.config.SemicolonInsertionMode;
import net.jangaroo.jooc.input.FileInputSource;
import net.jangaroo.jooc.input.InputSource;
import net.jangaroo.jooc.input.PathInputSource;
import net.jangaroo.utils.CompilerUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class ConfigClassRegistry {
  private Map<String, ConfigClass> configClassesByName = new HashMap<String, ConfigClass>();
  private Set<File> scannedExmlFiles = new HashSet<File>();

  private ExmlConfiguration config;
  private InputSource sourcePathInputSource;

  private JangarooParser jangarooParser;
  private ExmlToConfigClassParser exmlToConfigClassParser;

  public ConfigClassRegistry(final ExmlConfiguration config) throws IOException {
    this.config = config;

    sourcePathInputSource = PathInputSource.fromFiles(config.getSourcePath(), new String[0], true);
    InputSource classPathInputSource = PathInputSource.fromFiles(config.getClassPath(),
      new String[]{"", JangarooParser.JOO_API_IN_JAR_DIRECTORY_PREFIX}, false);

    ParserOptions parserOptions = new CCRParserOptions();
    jangarooParser = new JangarooParser(parserOptions, new StdOutCompileLog()) {
      @Override
      protected InputSource findSource(String qname) {
        InputSource inputSource = super.findSource(qname);
        if (inputSource instanceof FileInputSource && !((FileInputSource)inputSource).getSourceDir().equals(config.getOutputDirectory())) {
          // A regular source file (not a generated file) has been found. Use it.
          return inputSource;
        }
        // Just in case the requested class is a config class
        // that is generated from an EXML file, regenerate the config file before
        // it is too late. This will only affect files in the config package
        // and only generated files, so it is pretty safe.
        tryBuildConfigClassFromExml(qname);
        // Just in case the source was not found on the first attempt, fetch it again.
        return super.findSource(qname);
      }
    };
    List<File> fullSourcePath = new ArrayList<File>(config.getSourcePath());
    fullSourcePath.add(config.getOutputDirectory());
    jangarooParser.setUp(PathInputSource.fromFiles(fullSourcePath, new String[0], true), classPathInputSource);
    exmlToConfigClassParser = new ExmlToConfigClassParser(config);
  }

  public ExmlConfiguration getConfig() {
    return config;
  }

  /**
   * Returns the list of registered Config classes
   * @return list of registered Config classes
   */
  public Collection<ConfigClass> getRegisteredConfigClasses() {
    return configClassesByName.values();
  }

  /**
   * Returns the list of registered Config classes
   * @return list of registered Config classes
   */
  public Map<String, Collection<ConfigClass>> getRegisteredConfigClassesByTargetClassPackage() {
    Map<String, Collection<ConfigClass>> result = new HashMap<String, Collection<ConfigClass>>();
    for (ConfigClass configClass : configClassesByName.values()) {
      String packageName = CompilerUtils.packageName(configClass.getComponentClassName());
      Collection<ConfigClass> configClasses = result.get(packageName);
      if (configClasses == null) {
        configClasses = new HashSet<ConfigClass>();
        result.put(packageName, configClasses);
      }
      configClasses.add(configClass);
    }
    return Collections.unmodifiableMap(result);
  }

  /**
   * Scans all AS files the in the config class package
   */
  public void scanAllAsFiles() {
    InputSource configPackage = sourcePathInputSource.getChild(config.getConfigClassPackage().replace('.', File.separatorChar));
    if(configPackage != null) {
      scanAsFiles(configPackage);
    }
  }

  private void scanAsFiles(InputSource inputSource) {
    for (InputSource source : inputSource.list()) {
      File file = ((FileInputSource) source).getFile();
      if(file.isFile()) {
        if(file.getName().endsWith(Jooc.AS_SUFFIX)) {
          String qName;
          try {
            qName = CompilerUtils.qNameFromFile(getConfig().findSourceDir(file), file);
          } catch (IOException e) {
            throw new ExmlcException("could not read AS file", e);
          }
          final ConfigClass configClass = findActionScriptConfigClass(qName);
          addConfigClass(configClass);
        }
      } else {
        // Recurse into the tree.
        scanAsFiles(source);
      }
    }
  }

  /**
   * Setup the config class registry by scanning for .exml files, parsing them and adding their models to this registry.
   * This has to be called before you use the registry once.
   */
  public void scanAllExmlFiles() {
    scanExmlFiles(sourcePathInputSource);
  }

  private void scanExmlFiles(InputSource inputSource) {
    for (InputSource source : inputSource.list()) {
      File exmlFile = ((FileInputSource) source).getFile();
      if (exmlFile.isFile()) {
        if (exmlFile.getName().endsWith(Exmlc.EXML_SUFFIX)) {
          if (!scannedExmlFiles.contains(exmlFile)) {
            scannedExmlFiles.add(exmlFile);
            try {
              ConfigClass configClass = exmlToConfigClassParser.parseExmlToConfigClass(exmlFile);
              evaluateSuperClass(configClass);
              addConfigClass(configClass);
            } catch (IOException e) {
              // TODO Log and continue?
              throw new ExmlcException("could not read EXML file", e);
            }
          }
        }
      } else {
        // Recurse into the tree.
        scanExmlFiles(source);
      }
    }
  }

  /**
   * Get a ConfigClass for the given name. Returns null if no class was found
   * @param name the name of the class
   * @return the configClass or null if none was found
   */
  public ConfigClass getConfigClassByName(String name) {
    return getConfigClassByName(name, new HashSet<String>());
  }

  private ConfigClass getConfigClassByName(String name, Set<String> visited) {
    ConfigClass configClass = configClassesByName.get(name);
    if (configClass != null) {
      return configClass;
    }
    // The config class has not been registered so far.
    tryBuildConfigClassFromExml(name);
    configClass = configClassesByName.get(name);
    if (configClass != null) {
      return configClass;
    }
    // The given name does not denote a config class of an EXML component in the source tree.
    configClass = findActionScriptConfigClass(name);
    addConfigClass(configClass);
    return configClass;
  }

  public ConfigClass getConfigClassOfTargetClass(String targetClassName) {
    for (ConfigClass configClass : configClassesByName.values()) {
      if (targetClassName.equals(configClass.getComponentClassName())) {
        return configClass;
      }
    }
    return null;
  }

  private void addConfigClass(ConfigClass configClass) {
    if(configClass != null) {
      String name = configClass.getFullName();
      ConfigClass existingConfigClass = configClassesByName.get(name);
      if (existingConfigClass != null) {
        if (!existingConfigClass.equals(configClass)) {
          // todo: Keep track of source.
          throw new ExmlcException(String.format("config class '%s' declared in '%s' and '%s'.", name, configClass.getComponentClassName(), existingConfigClass.getComponentClassName()));
        }
      } else {
        configClassesByName.put(name, configClass);
      }
    }
  }

  private ConfigClass tryBuildConfigClassFromExml(String name) {
    FileInputSource exmlInputSource = (FileInputSource)sourcePathInputSource.getChild(JangarooParser.getInputSourceFileName(name, sourcePathInputSource, Exmlc.EXML_SUFFIX));
    if (exmlInputSource != null) {
      return buildFromExml(exmlInputSource);
    }
    if (name.startsWith(config.getConfigClassPackage() + ".")) {
      // The config class might originate from one of of this module's EXML files.
      FileInputSource outputDirInputSource = new FileInputSource(config.getOutputDirectory(), true);
      InputSource generatedConfigAsFile = outputDirInputSource.getChild(JangarooParser.getInputSourceFileName(name, outputDirInputSource, Jooc.AS_SUFFIX));
      if (generatedConfigAsFile != null) {
        // A candidate AS config class has already been generated.
        CompilationUnit compilationUnit = Jooc.doParse(generatedConfigAsFile, new StdOutCompileLog(), SemicolonInsertionMode.QUIRKS);
        ConfigClass generatedAsConfigClass = buildConfigClass(compilationUnit);
        if (generatedAsConfigClass != null) {
          // It is really a generated config class.
          // We can determine the name of the EXML component class
          // that was last used to create this config file.
          String componentName = generatedAsConfigClass.getComponentClassName();
          // We must parse the EXMl file again, because the parent class (and hence the
          // parent config class) might have changed.
          exmlInputSource = (FileInputSource)sourcePathInputSource.getChild(JangarooParser.getInputSourceFileName(componentName, sourcePathInputSource, Exmlc.EXML_SUFFIX));
          if (exmlInputSource == null) {
            // try again with lower-case component name:
            String lowerCaseComponentClassName = CompilerUtils.qName(CompilerUtils.packageName(componentName),
              CompilerUtils.uncapitalize(CompilerUtils.className(componentName)));
            exmlInputSource = (FileInputSource)sourcePathInputSource.getChild(JangarooParser.getInputSourceFileName(lowerCaseComponentClassName, sourcePathInputSource, Exmlc.EXML_SUFFIX));
          }
          if (exmlInputSource != null) {
            return buildFromExml(exmlInputSource);
          }
        }
        // The AS file should not exist. However, we do not consider this class
        // to be responsible to deleting outdated config files.
      }
      // The EXML was not found. Scan all EXML files to be sure the right one will be found.
      scanAllExmlFiles();
      return configClassesByName.get(name);
    }
    return null;
  }

  private ConfigClass buildFromExml(FileInputSource exmlInputSource) {
    scannedExmlFiles.add(exmlInputSource.getFile());
    ConfigClass configClass;
    try {
      configClass = new ExmlToConfigClassParser(config).parseExmlToConfigClass(exmlInputSource.getFile());
      evaluateSuperClass(configClass);
    } catch (IOException e) {
      // TODO log
      throw new IllegalStateException(e);
    }
    addConfigClass(configClass);
    return configClass;
  }

  private ConfigClass findActionScriptConfigClass(String name) {
    return findActionScriptConfigClass(name, new LinkedHashSet<String>());
  }

  private ConfigClass findActionScriptConfigClass(String name, Set<String> visited) {
    if (visited.contains(name)) {
      throw new ExmlcException("cyclic inheritance: " + Arrays.toString(visited.toArray()));
    }
    visited.add(name);
    CompilationUnit compilationsUnit = jangarooParser.getCompilationsUnit(name);
    ConfigClass configClass = null;
    if (compilationsUnit != null) {
      try {
        configClass = buildConfigClass(compilationsUnit);
        if (configClass != null && configClass.getComponentClassName() == null) {
          // it was actually a target class! try again with the resolved config class:
          return findActionScriptConfigClass(configClass.getFullName(), visited);
        }
        evaluateSuperClass(configClass, visited);
      } catch (RuntimeException e) {
        throw new ExmlcException("while building config class '" + name + "': " + e.getMessage(), e);
      }
    }
    return configClass;
  }

  public void evaluateSuperClass(ConfigClass configClass) {
    evaluateSuperClass(configClass, new LinkedHashSet<String>());
  }

  private void evaluateSuperClass(ConfigClass configClass, Set<String> visited) {
    if(configClass != null) {
      String superClassName = configClass.getSuperClassName();
      if(superClassName != null && !"joo.JavaScriptObject".equals(superClassName)) {
        ConfigClass configClassOfTargetClass = findConfigClassOfTargetClass(superClassName);
        if (configClassOfTargetClass != null) {
          superClassName = configClassOfTargetClass.getFullName();
        }
        if(configClass.getFullName().equals(superClassName)) {
          throw new ExmlcException(String.format("Cyclic inheritance error: superclass '%s' of config class '%s' are the same!", superClassName, configClass.getFullName()));
        }
        ConfigClass superClass = findActionScriptConfigClass(superClassName, visited);
        if(superClass == null) {
          // might be a not-yet-compiled EXML:
          superClass = tryBuildConfigClassFromExml(superClassName);
          if (superClass == null) {
            throw new ExmlcException(String.format("Superclass '%s' of class '%s' not found!", superClassName, configClass.getFullName()));
          }
        }
        if (!configClass.getSuperClassName().equals(superClass.getFullName())) {
          configClass.setSuperClassName(superClass.getFullName()); // needed for target classes that have been mapped to their config class!
        }
        configClass.setSuperClass(superClass);
      }
    }
  }

  private ConfigClass findConfigClassOfTargetClass(String targetClassName) {
    for (ConfigClass configClass : configClassesByName.values()) {
      if (targetClassName.equals(configClass.getComponentClassName())) {
        return configClass;
      }
    }
    return null;
  }

  private ConfigClass buildConfigClass(CompilationUnit compilationUnit) {
    ConfigClassBuilder configClassBuilder = new ConfigClassBuilder(compilationUnit);
    return configClassBuilder.buildConfigClass();
  }

  private static class CCRParserOptions implements ParserOptions {
    @Override
    public SemicolonInsertionMode getSemicolonInsertionMode() {
      return SemicolonInsertionMode.QUIRKS;
    }

    @Override
    public boolean isVerbose() {
      return false;
    }
  }
}
