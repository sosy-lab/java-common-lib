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
package org.sosy_lab.common.configuration;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import com.google.common.base.Strings;
import com.google.common.io.CharSource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 * A parser for a simple configuration file format based on "key = value" pairs.
 *
 * The configuration file will always be interpreted as UTF-8.
 *
 * Supported features:
 * - Comments at line beginnings with '# ' (with space!) and '//'
 * - Whitespace ignored before comments and around keys and values
 * - Key format is at least one word consisting of a-zA-Z0-9_+-,
 *   words are separated by '.'. Example: foo.bar
 * - Keys may be specified exactly once per file.
 * - Options with a common prefix of words in their key can be put into a section
 *   so that the prefix does not need to be written each time
 *   (similar to Windows-Ini-files).
 *   Example:
 *   <code>
 *   [foo]
 *   bar = abc
 *   </code>
 *   is equal to
 *   <code>foo.bar = abc</code>
 * - Options before the first section start or in a section with an empty name
 *   have no such prefix.
 * - Inside the value, put '\' at the line end to append the next line to the current value
 *   (not possible in other places like key or section start).
 *   Whitespace at the beginning and end of all lines will be removed, so indentation is possible.
 * - Other files can be included (recursively) with '#include <FILE>'.
 *   If the file name is a relative one, it is considered relative to the directory
 *   of the current file. Directives in the current file will always overwrite
 *   included directives, no matter of their placement.
 *   Directives from an included file will overwrite directives from previously
 *   included files.
 *   Circular inclusions are now allowed.
 */
class Parser {

  static class InvalidConfigurationFileException extends InvalidConfigurationException {

    private static final long serialVersionUID = 8146907093750189669L;

    private InvalidConfigurationFileException(String msg, int lineno, String source, String line) {
      super(msg + " in line " + lineno + " of " + source + ": " + line);
    }

    private InvalidConfigurationFileException(String msg) {
      super(msg);
    }
  }

  private static final Pattern OPTION_NAME =
      Pattern.compile("^[a-zA-Z0-9_+-]+(\\.[a-zA-Z0-9_+-]+)*$");

  private final Map<String, String> options = new HashMap<>();
  private final Map<String, Path> sources = new HashMap<>();

  // inclusion stack for finding circular includes
  private final Deque<String> includeStack = new ArrayDeque<>();

  private Parser() {}

  /**
   * Get the map with all configuration directives in the parsed file.
   */
  Map<String, String> getOptions() {
    return Collections.unmodifiableMap(options);
  }

  /**
   * Get the map with the source location of each defined option.
   */
  Map<String, Path> getSources() {
    return Collections.unmodifiableMap(sources);
  }

  /**
   * Parse a configuration file with the format as defined above.
   *
   * @param file The file to parse.
   * @param basePath If filename is relative, use this as parent path
   * (if null or empty, the current working directory is used).
   * @throws IOException If an I/O error occurs.
   * @throws InvalidConfigurationException If the configuration file has an invalid format.
   */
  @CheckReturnValue
  static Parser parse(Path file, @Nullable String basePath)
      throws IOException, InvalidConfigurationException {

    Parser parser = new Parser();
    parser.parse0(file, basePath);
    verify(parser.includeStack.isEmpty());
    return parser;
  }

  /**
   * Parse a configuration file with the format as defined above.
   *
   * @param file The file to parse.
   * @param basePath If filename is relative, use this as parent path
   * (if null or empty, the current working directory is used).
   * @throws IOException If an I/O error occurs.
   * @throws InvalidConfigurationException If the configuration file has an invalid format.
   */
  private void parse0(Path file, @Nullable String basePath)
      throws IOException, InvalidConfigurationException {

    if (!file.isAbsolute() && !Strings.isNullOrEmpty(basePath)) {
      file = Paths.get(basePath, file.getPath());
    }

    Files.checkReadableFile(file);

    String fileName = file.toAbsolutePath().getPath();
    if (includeStack.contains(fileName)) {
      throw new InvalidConfigurationFileException(
          "Circular inclusion of file " + file.toAbsolutePath());
    }
    includeStack.addLast(fileName);

    try (BufferedReader r = file.asCharSource(StandardCharsets.UTF_8).openBufferedStream()) {
      parse(r, file.getParent().getPath(), file.getPath());
    }
    includeStack.removeLast();
  }

  /**
   * Parse a configuration file given as a {@link CharSource} with the format as defined above.
   *
   * A stream from this source is opened and closed by this method.
   * This method may additionally access more files from the file system
   * if they are included.
   *
   * @param source The source to read the file from.
   * @param basePath If filename is relative, use this as parent path
   * (if null or empty, the current working directory is used).
   * @param sourceName A string to use as source of the file in error messages
   * (this should usually be a filename or something similar).
   * @throws IOException If an I/O error occurs.
   * @throws InvalidConfigurationException If the configuration file has an invalid format.
   */
  @CheckReturnValue
  static Parser parse(CharSource source, @Nullable String basePath, String sourceName)
      throws IOException, InvalidConfigurationException {

    Parser parser = new Parser();
    try (BufferedReader r = source.openBufferedStream()) {
      parser.parse(r, basePath, sourceName);
    }
    verify(parser.includeStack.isEmpty());
    return parser;
  }

  /**
   * Parse a configuration file given as a {@link BufferedReader} with the format as defined above.
   *
   * The reader is left open after this method returns.
   * This method may additionally access more files from the file system
   * if they are included.
   *
   * @param r The reader to read the file from.
   * @param basePath If filename is relative, use this as parent path
   * (if null or empty, the current working directory is used).
   * @param source A string to use as source of the file in error messages
   * (this should usually be a filename or something similar).
   * @throws IOException If an I/O error occurs.
   * @throws InvalidConfigurationException If the configuration file has an invalid format.
   */
  @SuppressFBWarnings(value = "SBSC_USE_STRINGBUFFER_CONCATENATION",
      justification = "performance irrelevant compared to I/O, String much more convenient")
  private void parse(BufferedReader r, @Nullable String basePath, String source)
      throws IOException, InvalidConfigurationException {
    checkNotNull(source);

    String line;
    int lineno = 0;
    String currentPrefix = "";
    String currentOptionName = null;
    String currentValue = null;
    Map<String, String> definedOptions = new HashMap<>();

    while ((line = r.readLine()) != null) {
      lineno++;
      line = line.trim();
      final String fullLine = line;

      assert (currentValue == null) == (currentOptionName == null);

      if (currentValue != null) {
        // we are in the continuation of a key = value pair
        currentValue += line;

        // no continue here, we need to run the code at the end of the loop body

      } else if (line.isEmpty() || line.startsWith("# ") || line.startsWith("//")) {
        // empty or comment
        continue;

      } else if (line.startsWith("#")) {
        // it is a parser directive
        // currently only #include is supported.

        if (!line.startsWith("#include")) {
          throw new InvalidConfigurationFileException(
              "Illegal parser directive", lineno, source, fullLine);
        }

        line = line.substring("#include".length()).trim();
        if (line.isEmpty()) {
          throw new InvalidConfigurationFileException(
              "Include without filename", lineno, source, fullLine);
        }

        // parse included file (content will be in fields of this class)
        parse0(Paths.get(line), basePath);
        continue;

      } else if (line.startsWith("[") && line.endsWith("]")) {
        // category
        line = line.substring(1, line.length() - 1);
        line = line.trim();

        if (line.isEmpty()) {
          // this is allowed, it clears the prefix
          currentPrefix = "";

        } else if (!OPTION_NAME.matcher(line).matches()) {
          throw new InvalidConfigurationFileException(
              "Invalid category \"" + line + "\"", lineno, source, fullLine);

        } else {
          currentPrefix = line + ".";
        }
        continue;

      } else if (line.length() < 3) {
        throw new InvalidConfigurationFileException(
            "Illegal content", lineno, source, fullLine);

      } else {
        // normal key=value line
        String[] bits = line.split("=", 2);
        if (bits.length != 2) {
          throw new InvalidConfigurationFileException(
              "Missing key-value separator", lineno, source, fullLine);
        }

        currentOptionName = bits[0].trim();
        if (!OPTION_NAME.matcher(currentOptionName).matches()) {
          throw new InvalidConfigurationFileException(
              "Invalid option \"" + currentOptionName + "\"", lineno, source, fullLine);
        }
        if (definedOptions.containsKey(currentPrefix + currentOptionName)) {
          throw new InvalidConfigurationFileException(
              "Duplicate option \"" + currentPrefix + currentOptionName + "\"",
              lineno, source, fullLine);
        }

        currentValue = bits[1].trim();
      }


      assert (currentValue != null) && (currentOptionName != null);

      if (currentValue.endsWith("\\")) {
        // continuation
        currentValue = currentValue.substring(0, currentValue.length() - 1);

      } else {
        definedOptions.put(currentPrefix + currentOptionName, currentValue);
        currentValue = null;
        currentOptionName = null;
      }
    }

    assert (currentValue == null) == (currentOptionName == null);

    if (currentValue != null) {
      definedOptions.put(currentPrefix + currentOptionName, currentValue);
    }

    // now overwrite included options with local ones
    options.putAll(definedOptions);

    Path thisSource = Paths.get(source);
    for (String name : definedOptions.keySet()) {
      sources.put(name, thisSource);
    }
  }
}
