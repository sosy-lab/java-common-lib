/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.common.configuration.converters;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.errorprone.annotations.Var;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathCounterTemplate;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;

/**
 * A {@link TypeConverter} for options of type {@link File} or {@link Path} which offers some
 * additional features like a common base directory for all output files. In order to use these
 * features, the options need to be annotated with {@link FileOption}.
 *
 * <p>This type converter should be registered for the type {@link FileOption}.
 *
 * <p>The additional features are:
 *
 * <ul>
 *   <li>All specified relative paths are resolved against a given root directory.
 *   <li>All relative paths of output files are resolved against a separate output directory.
 *   <li>All output files can be disabled by a central switch.
 * </ul>
 *
 * <p>In order to configure these features, the normal configuration options are used.
 */
@Options
public final class FileTypeConverter implements TypeConverter {

  private static final ImmutableSet<Class<?>> SUPPORTED_TYPES =
      ImmutableSet.<Class<?>>of(
          File.class, Path.class, PathTemplate.class, PathCounterTemplate.class);

  private static final String TEMP_DIR =
      Paths.get(StandardSystemProperty.JAVA_IO_TMPDIR.value() + File.separator).toString();

  @Option(secure = true, name = "output.path", description = "directory to put all output files in")
  private String outputDirectory = "output/";

  @VisibleForTesting final Path outputPath;

  @Option(
      secure = true,
      name = "output.disable",
      description =
          "disable all default output files\n(any explicitly given file will still be written)")
  private boolean disableOutput = false;

  @Option(
      description =
          "base directory for all input & output files\n(except for the configuration file itself)")
  private String rootDirectory = ".";

  @VisibleForTesting final Path rootPath;

  // Allow only paths below the current directory,
  // i.e., no absolute paths and no "../"
  @VisibleForTesting final boolean safePathsOnly;

  private FileTypeConverter(Configuration config, boolean pSafePathsOnly)
      throws InvalidConfigurationException {
    safePathsOnly = pSafePathsOnly; // set before calls to checkSafePath
    config.inject(this, FileTypeConverter.class);

    rootPath = checkSafePath(Paths.get(rootDirectory), "rootDirectory");
    outputPath = checkSafePath(rootPath.resolve(outputDirectory), "output.path");
  }

  private FileTypeConverter(Configuration config, FileTypeConverter defaultsInstance)
      throws InvalidConfigurationException {
    safePathsOnly = defaultsInstance.safePathsOnly; // set before calls to checkSafePath
    config.injectWithDefaults(this, FileTypeConverter.class, defaultsInstance);

    rootPath = checkSafePath(Paths.get(rootDirectory), "rootDirectory");
    outputPath = checkSafePath(rootPath.resolve(outputDirectory), "output.path");
  }

  public static FileTypeConverter create(Configuration config)
      throws InvalidConfigurationException {
    return new FileTypeConverter(config, false);
  }

  /**
   * Create an instanceof of this class that allows only injected files that are below the current
   * directory.
   */
  public static FileTypeConverter createWithSafePathsOnly(Configuration config)
      throws InvalidConfigurationException {
    return new FileTypeConverter(config, true);
  }

  @Override
  public FileTypeConverter getInstanceForNewConfiguration(Configuration pNewConfiguration)
      throws InvalidConfigurationException {
    return new FileTypeConverter(pNewConfiguration, this);
  }

  /**
   * Checks whether a path is safe (i.e., it is below the current directory). Absolute paths and
   * paths with "../" are forbidden. Returns the unchanged path if it is safe, or throws an
   * exception.
   *
   * <p>If {@link #safePathsOnly} is false, no checks are done, and this method always returns the
   * path.
   */
  @VisibleForTesting
  Path checkSafePath(Path pPath, String optionName) throws InvalidConfigurationException {
    if (!safePathsOnly) {
      return pPath; // any path allowed
    }

    @Var String path = pPath.toString();

    if (pPath.isAbsolute()) {
      if (pPath.startsWith(TEMP_DIR)) {
        // Make it a a relative path
        path = path.substring(TEMP_DIR.length());

      } else {
        throw forbidden("because it is absolute", optionName, pPath);
      }
    }

    if (path.contains(File.pathSeparator)) {
      throw forbidden(
          "because it contains the character '%s'", optionName, pPath, File.pathSeparator);
    }

    @Var int depth = 0;
    for (String component : Splitter.on(File.separator).split(path)) {
      switch (component) {
        case "":
        case ".":
          break;
        case "..":
          depth--;
          break;
        default:
          depth++;
          break;
      }

      if (depth < 0) {
        throw forbidden("because it is not below the current directory", optionName, pPath);
      }
    }

    return pPath;
  }

  private static InvalidConfigurationException forbidden(
      String reason, String optionName, Path path, Object... args)
      throws InvalidConfigurationException {
    throw new InvalidConfigurationException(
        String.format(
            "The option %s specifies the path '%s' that is forbidden in safe mode " + reason + ".",
            FluentIterable.<Object>of(optionName, path).append(args).toArray(Object.class)));
  }

  public String getOutputDirectory() {
    return outputPath.toString();
  }

  public Path getOutputPath() {
    return outputPath;
  }

  private static void checkApplicability(
      Class<?> type, @Nullable Annotation secondaryOption, String optionName) {
    if (!SUPPORTED_TYPES.contains(type) || !(secondaryOption instanceof FileOption)) {

      throw new UnsupportedOperationException(
          "A FileTypeConverter can handle only options of type File"
              + " and with a @FileOption annotation, but "
              + optionName
              + " does not fit.");
    }
  }

  @Override
  @Nullable
  public Object convert(
      String optionName,
      String pValue,
      TypeToken<?> pType,
      Annotation secondaryOption,
      Path pSource,
      LogManager logger)
      throws InvalidConfigurationException {

    Class<?> type = pType.getRawType();
    checkApplicability(type, secondaryOption, optionName);
    assert secondaryOption != null : "checkApplicability should ensure this";

    Path path;
    try {
      path = Paths.get(pValue);
    } catch (InvalidPathException e) {
      throw new InvalidConfigurationException(
          String.format("Invalid file name in option %s: %s", optionName, e.getMessage()), e);
    }

    return handleFileOption(
        optionName,
        path,
        ((FileOption) secondaryOption).value(),
        type,
        pSource,
        /*doResolve=*/ true);
  }

  @Override
  @Nullable
  public <T> T convertDefaultValue(
      String optionName, T pDefaultValue, TypeToken<T> pType, Annotation secondaryOption)
      throws InvalidConfigurationException {

    return convertDefaultValue(
        optionName, pDefaultValue, pType, secondaryOption, /*doResolve=*/ true);
  }

  @Nullable
  private <T> T convertDefaultValue(
      String optionName,
      T pDefaultValue,
      TypeToken<T> pType,
      Annotation secondaryOption,
      boolean doResolve)
      throws InvalidConfigurationException {

    Class<?> type = pType.getRawType();
    checkApplicability(type, secondaryOption, optionName);
    assert secondaryOption != null : "checkApplicability should ensure this";

    FileOption.Type typeInfo = ((FileOption) secondaryOption).value();

    if (pDefaultValue == null) {
      if (typeInfo == FileOption.Type.REQUIRED_INPUT_FILE) {
        throw new UnsupportedOperationException(
            "The option "
                + optionName
                + " specifies a required input file,"
                + " but the option is neither required nor has a default value.");
      }

      return null;
    }

    if (disableOutput && isOutputOption(typeInfo)) {
      // disable output by setting the option to null
      return null;
    }

    Path defaultValue;
    if (type.equals(File.class)) {
      defaultValue = ((File) pDefaultValue).toPath();
    } else if (type.equals(PathTemplate.class)) {
      defaultValue = Paths.get(((PathTemplate) pDefaultValue).getTemplate());
    } else if (type.equals(PathCounterTemplate.class)) {
      defaultValue = Paths.get(((PathCounterTemplate) pDefaultValue).getTemplate());
    } else {
      defaultValue = (Path) pDefaultValue;
    }

    @SuppressWarnings("unchecked")
    T value = (T) handleFileOption(optionName, defaultValue, typeInfo, type, null, doResolve);
    return value;
  }

  @Override
  @Nullable
  public <T> T convertDefaultValueFromOtherInstance(
      String optionName,
      @Nullable T pDefaultValue,
      TypeToken<T> pType,
      @Nullable Annotation secondaryOption)
      throws InvalidConfigurationException {

    // If we take the default value from an existing instance, it was already resolved, so we do not
    // resolve it again.
    // However, we must do all other sanity and safety checks,
    // and we also must create new instances of e.g. PathCounterTemplate.
    return convertDefaultValue(
        optionName, pDefaultValue, pType, secondaryOption, /*doResolve=*/ false);
  }

  /**
   * This function returns a file. It sets the path of the file to the given outputDirectory in the
   * given rootDirectory.
   *
   * @param optionName name of option only for error handling
   * @param file the file name to adjust
   * @param typeInfo info about the type of the file (outputfile, inputfile)
   * @param doResolve whether to resolve the file according to outputPath, rootPath, etc.
   */
  @SuppressWarnings("CheckReturnValue")
  private Object handleFileOption(
      String optionName,
      @Var Path file,
      FileOption.Type typeInfo,
      Class<?> targetType,
      Path source,
      boolean doResolve)
      throws InvalidConfigurationException {

    if (doResolve) {
      if (isOutputOption(typeInfo)) {
        file = outputPath.resolve(file);
      } else if (source != null) {
        file = source.resolveSibling(file);
      } else {
        file = rootPath.resolve(file);
      }
    }

    checkSafePath(file, optionName); // throws exception if unsafe

    if (typeInfo == FileOption.Type.OUTPUT_DIRECTORY) {
      if (Files.isRegularFile(file)) {
        throw new InvalidConfigurationException(
            "Option " + optionName + " specifies a file instead of a directory: " + file);
      }
    } else {
      if (Files.isDirectory(file)) {
        throw new InvalidConfigurationException(
            "Option " + optionName + " specifies a directory instead of a file: " + file);
      }
    }

    if (typeInfo == FileOption.Type.REQUIRED_INPUT_FILE) {
      try {
        IO.checkReadableFile(file);
      } catch (FileNotFoundException e) {
        throw new InvalidConfigurationException(
            "Option " + optionName + " specifies an invalid input file: " + e.getMessage(), e);
      }
    }

    if (targetType.equals(File.class)) {
      return file.toFile();
    } else if (targetType.equals(PathTemplate.class)) {
      return PathTemplate.ofFormatString(file.toString());
    } else if (targetType.equals(PathCounterTemplate.class)) {
      return PathCounterTemplate.ofFormatString(file.toString());
    } else {
      assert targetType.equals(Path.class);
      return file;
    }
  }

  private static boolean isOutputOption(FileOption.Type typeInfo) {
    return typeInfo == FileOption.Type.OUTPUT_FILE || typeInfo == FileOption.Type.OUTPUT_DIRECTORY;
  }
}
