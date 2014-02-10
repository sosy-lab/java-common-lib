/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

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

  private static final Pattern OPTION_NAME = Pattern.compile("^[a-zA-Z0-9_+-]+(\\.[a-zA-Z0-9_+-]+)*$");

  /**
   * Parse a configuration file with the format as defined above.
   *
   * @param filename The file to parse.
   * @param basePath If filename is relative, use this as parent path (if null or empty, the current working directory is used).
   * @return A map with all configuration directives in this file, and a map with the source location of each defined option.
   * @throws IOException If an I/O error occurs.
   * @throws InvalidConfigurationException If the configuration file has an invalid format.
   */
  static Pair<Map<String, String>, Map<String, Path>> parse(Path file, @Nullable String basePath)
      throws IOException, InvalidConfigurationException {

    return parse(file, basePath, Collections.<String>emptySet());
  }

  /**
   * Parse a configuration file with the format as defined above.
   *
   * @param basePath If filename is relative, use this as parent path (if null or empty, the current working directory is used).
   * @param filename The file to parse.
   * @param includeStack A set of all files present in the current stack of #include directives.
   * @return A map with all configuration directives in this file, and a map with the source location of each defined option.
   * @throws IOException If an I/O error occurs.
   * @throws InvalidConfigurationException If the configuration file has an invalid format.
   */
  private static Pair<Map<String, String>, Map<String, Path>> parse(Path file, @Nullable String basePath,
      Set<String> includeStack) throws IOException, InvalidConfigurationException {

    if (!file.isAbsolute() && !Strings.isNullOrEmpty(basePath)) {
      file = Paths.get(basePath, file.getPath());
    }

    Files.checkReadableFile(file);

    includeStack = new HashSet<>(includeStack);
    boolean newFile = includeStack.add(file.toAbsolutePath().getPath());
    if (!newFile) {
      throw new InvalidConfigurationFileException("Circular inclusion of file " + file.toAbsolutePath());
    }

    try (InputStream is = file.asByteSource().openStream()) {
      return parse(is, file.getParent().getPath(), file.getPath(), includeStack);
    }
  }

  /**
   * Parse a configuration file given as an {@link InputStream} with the format as defined above.
   *
   * The stream is left open after this method returns.
   * This method may itself open files from the filesystem if they are included,
   * those files are closed.
   *
   * @param is The stream to read the file from.
   * @param basePath If filename is relative, use this as parent path (if null or empty, the current working directory is used).
   * @param source A string to use as source of the file in error messages (this should usually be a filename or something similar).
   * @return A map with all configuration directives in this file, and a map with the source location of each defined option.
   * @throws IOException If an I/O error occurs.
   * @throws InvalidConfigurationException If the configuration file has an invalid format.
   */
  static Pair<Map<String, String>, Map<String, Path>> parse(InputStream is, @Nullable String basePath,
      String source) throws IOException, InvalidConfigurationException {

    return parse(is, basePath, source, Collections.<String>emptySet());
  }

  /**
   * Parse a configuration file given as an {@link InputStream} with the format as defined above.
   *
   * The stream is left open after this method returns.
   * This method may itself open files from the filesystem if they are included,
   * those files are closed.
   *
   * @param is The stream to read the file from.
   * @param basePath If filename is relative, use this as parent path (if null or empty, the current working directory is used).
   * @param source A string to use as source of the file in error messages (this should usually be a filename or something similar).
   * @param includeStack A set of all files present in the current stack of #include directives.
   * @return A map with all configuration directives in this file, and a map with the source location of each defined option.
   * @throws IOException If an I/O error occurs.
   * @throws InvalidConfigurationException If the configuration file has an invalid format.
   */
  private static Pair<Map<String, String>, Map<String, Path>> parse(
      InputStream is, @Nullable String basePath,
      String source, Set<String> includeStack) throws IOException, InvalidConfigurationException {
    checkNotNull(source);

    BufferedReader r = new BufferedReader(new InputStreamReader(is, Charsets.UTF_8));

    String line;
    int lineno = 0;
    String currentPrefix = "";
    String currentOptionName = null;
    String currentValue = null;
    Map<String, String> definedOptions = Maps.newHashMap();
    Map<String, String> includedOptions = Maps.newHashMap();
    Map<String, Path> includedOptionsSources = Maps.newHashMap();

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
          throw new InvalidConfigurationFileException("Illegal parser directive", lineno, source, fullLine);
        }

        line = line.substring("#include".length()).trim();
        if (line.isEmpty()) {
          throw new InvalidConfigurationFileException("Include without filename", lineno, source, fullLine);
        }

        final Pair<Map<String, String>, Map<String, Path>> includedContent = parse(Paths.get(line), basePath, includeStack);
        includedOptions.putAll(includedContent.getFirst());
        includedOptionsSources.putAll(includedContent.getSecond());
        continue;

      } else if (line.startsWith("[") && line.endsWith("]")) {
        // category
        line = line.substring(1, line.length()-1);
        line = line.trim();

        if (line.isEmpty()) {
          // this is allowed, it clears the prefix
          currentPrefix = "";

        } else if (!OPTION_NAME.matcher(line).matches()) {
          throw new InvalidConfigurationFileException("Invalid category \"" + line + "\"", lineno, source, fullLine);

        } else {
          currentPrefix = line + ".";
        }
        continue;

      } else if (line.length() < 3) {
        throw new InvalidConfigurationFileException("Illegal content", lineno, source, fullLine);

      } else {
        // normal key=value line
        String[] bits = line.split("=", 2);
        if (bits.length != 2) {
          throw new InvalidConfigurationFileException("Missing key-value separator", lineno, source, fullLine);
        }

        currentOptionName = bits[0].trim();
        if (!OPTION_NAME.matcher(currentOptionName).matches()) {
          throw new InvalidConfigurationFileException("Invalid option \"" + currentOptionName + "\"", lineno, source, fullLine);
        }
        if (definedOptions.containsKey(currentPrefix + currentOptionName)) {
          throw new InvalidConfigurationFileException("Duplicate option \"" + currentPrefix + currentOptionName + "\"", lineno, source, fullLine);
        }

        currentValue = bits[1].trim();
      }


      assert (currentValue != null) && (currentOptionName != null);

      if (currentValue.endsWith("\\")) {
        // continuation
        currentValue = currentValue.substring(0, currentValue.length()-1);

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
    includedOptions.putAll(definedOptions);

    Path thisSource = Paths.get(source);
    for (String name : definedOptions.keySet()) {
      includedOptionsSources.put(name, thisSource);
    }

    return Pair.of(includedOptions, includedOptionsSources);
  }

}
