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

import static com.google.common.collect.FluentIterable.from;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.io.PathCounterTemplate;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Nullable;

/**
 * A {@link TypeConverter} for options of type {@link File} or {@link Path} which offers some
 * additional features like a common base directory for all output files.
 * In order to use these features, the options need to be annotated with
 * {@link FileOption}.
 *
 * This type converter should be registered for the type {@link FileOption}.
 *
 * The additional features are:
 * - All specified relative paths are resolved against a given root directory.
 * - All relative paths of output files are resolved against a separate output directory.
 * - All output files can be disabled by a central switch.
 *
 * In order to configure these features, the normal configuration options are used.
 */
@Options
public class FileTypeConverter implements TypeConverter {

  private static final ImmutableSet<Class<?>> SUPPORTED_TYPES =
      ImmutableSet.<Class<?>>of(
          File.class, Path.class, PathTemplate.class, PathCounterTemplate.class);

  private static final String TEMP_DIR =
      StandardSystemProperty.JAVA_IO_TMPDIR.value() + File.separator;

  @Option(secure = true, name = "output.path", description = "directory to put all output files in")
  private String outputDirectory = "output/";

  private final Path outputPath;

  @Option(
    secure = true,
    name = "output.disable",
    description =
        "disable all default output files\n(any explicitly given file will still be written)"
  )
  private boolean disableOutput = false;

  @Option(
    description =
        "base directory for all input & output files\n(except for the configuration file itself)"
  )
  private String rootDirectory = ".";

  private final Path rootPath;

  // Allow only paths below the current directory,
  // i.e., no absolute paths and no "../"
  private final boolean safePathsOnly;

  @Deprecated
  public FileTypeConverter(Configuration config) throws InvalidConfigurationException {
    this(config, false);
  }

  protected FileTypeConverter(Configuration config, boolean pSafePathsOnly)
      throws InvalidConfigurationException {
    safePathsOnly = pSafePathsOnly; // set before calls to checkSafePath
    config.inject(this, FileTypeConverter.class);

    rootPath = checkSafePath(Paths.get(rootDirectory), "rootDirectory");
    outputPath = checkSafePath(rootPath.resolve(outputDirectory), "output.path");
  }

  public static FileTypeConverter create(Configuration config)
      throws InvalidConfigurationException {
    return new FileTypeConverter(config, false);
  }

  /**
   * Create an instanceof of this class that allows only injected files
   * that are below the current directory.
   */
  public static FileTypeConverter createWithSafePathsOnly(Configuration config)
      throws InvalidConfigurationException {
    return new FileTypeConverter(config, true);
  }

  /**
   * Checks whether a path is safe (i.e., it is below the current directory).
   * Absolute paths and paths with "../" are forbidden.
   * Returns the unchanged path if it is safe,
   * or throws an exception.
   *
   * If {@link #safePathsOnly} is false, no checks are done,
   * and this method always returns the path.
   */
  @VisibleForTesting
  Path checkSafePath(Path pPath, String optionName) throws InvalidConfigurationException {
    if (!safePathsOnly) {
      return pPath; // any path allowed
    }

    String path = pPath.toString();

    if (pPath.isAbsolute()) {
      if (pPath.startsWith(TEMP_DIR)) {
        // Make it a a relative path
        path = path.substring(TEMP_DIR.length());

      } else {
        throw forbidden("because it is absolute", optionName, pPath);
      }
    }

    // We allow :: but not : in path, because the latter is used as a special marker in CPAchecker.
    if (path.replaceAll("::", "").contains(File.pathSeparator)) {
      throw forbidden(
          "because it contains the character '%s'", optionName, pPath, File.pathSeparator);
    }

    int depth = 0;
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
            from(ImmutableList.<Object>of(optionName, path)).append(args).toArray(Object.class)));
  }

  public String getOutputDirectory() {
    return outputPath.toString();
  }

  public Path getOutputPath() {
    return outputPath;
  }

  private void checkApplicability(
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

    return handleFileOption(
        optionName, Paths.get(pValue), ((FileOption) secondaryOption).value(), type, pSource);
  }

  @Override
  public <T> T convertDefaultValue(
      String optionName, T pDefaultValue, TypeToken<T> pType, Annotation secondaryOption)
      throws InvalidConfigurationException {

    final Class<?> type = pType.getRawType();
    checkApplicability(type, secondaryOption, optionName);

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

    if (disableOutput && typeInfo == FileOption.Type.OUTPUT_FILE) {
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
    T value = (T) handleFileOption(optionName, defaultValue, typeInfo, type, null);
    return value;
  }

  /** This function returns a file. It sets the path of the file to
   * the given outputDirectory in the given rootDirectory.
   *
   * @param optionName name of option only for error handling
   * @param file the file name to adjust
   * @param typeInfo info about the type of the file (outputfile, inputfile) */
  @SuppressWarnings("CheckReturnValue")
  private Object handleFileOption(
      final String optionName,
      Path file,
      final FileOption.Type typeInfo,
      final Class<?> targetType,
      final Path source)
      throws InvalidConfigurationException {

    if (typeInfo == FileOption.Type.OUTPUT_FILE) {
      file = outputPath.resolve(file);
    } else if (source != null) {
      Path baseDir = source.getParent();
      if (baseDir != null) {
        file = baseDir.resolve(file);
      }
    } else {
      file = rootPath.resolve(file);
    }

    checkSafePath(file, optionName); // throws exception if unsafe

    if (Files.isDirectory(file)) {
      throw new InvalidConfigurationException(
          "Option " + optionName + " specifies a directory instead of a file: " + file);
    }

    if (typeInfo == FileOption.Type.REQUIRED_INPUT_FILE) {
      try {
        MoreFiles.checkReadableFile(file);
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
}
