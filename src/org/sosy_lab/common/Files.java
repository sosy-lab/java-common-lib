/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.common;

import static java.nio.file.StandardOpenOption.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Provides helper functions for file access.
 */
public final class Files {

  private Files() { /* utility class */ }

  /**
   * Creates a temporary file with an optional content. The file is marked for
   * deletion when the Java VM exits.
   * @param  prefix     The prefix string to be used in generating the file's
   *                    name; must be at least three characters long
   * @param  suffix     The suffix string to be used in generating the file's
   *                    name; may be <code>null</code>, in which case the
   *                    suffix <code>".tmp"</code> will be used
   * @param content The content to write (may be null).
   *
   * @throws  IllegalArgumentException
   *          If the <code>prefix</code> argument contains fewer than three
   *          characters
   * @throws  IOException  If a file could not be created
   */
  public static File createTempFile(String prefix, String suffix, @Nullable String content) throws IOException {
    File file = File.createTempFile(prefix, suffix);
    file.deleteOnExit();

    if (!Strings.isNullOrEmpty(content)) {
      try {
        writeFile(file, content);
      } catch (IOException e) {
        file.delete();

        throw e;
      }
    }
    return file;
  }

  /**
   * Writes content to a file.
   * @param file The file.
   * @param content The content which shall be written.
   * @throws IOException
   */
  public static void writeFile(File file, Object content) throws IOException {
    writeFile(file.toPath(), content);
  }

  /**
   * Writes content to a file.
   * @param file The file.
   * @param content The content which shall be written.
   * @throws IOException
   */
  public static void writeFile(Path file, Object content) throws IOException {
    java.nio.file.Files.createDirectories(file.getParent());

    try (Writer w = java.nio.file.Files.newBufferedWriter(file, Charset.defaultCharset())) {
      w.write(content.toString());
    }
  }

  /**
   * Writes content to a file.
   * @param file The file.
   * @param content The content which will be written to the end of the file.
   * @throws IOException
   */
  public static void appendToFile(File file, Object content) throws IOException {
    appendToFile(file.toPath(), content);
  }

  /**
   * Writes content to a file.
   * @param file The file.
   * @param content The content which will be written to the end of the file.
   * @throws IOException
   */
  public static void appendToFile(Path file, Object content) throws IOException {
    java.nio.file.Files.createDirectories(file.getParent());

    try (Writer w = java.nio.file.Files.newBufferedWriter(file, Charset.defaultCharset(),
        APPEND, CREATE)) {
      w.write(content.toString());
    }
  }

  /**
   * Verifies if a file exists, is a normal file and is readable. If this is not
   * the case, a FileNotFoundException with a nice message is thrown.
   *
   * @param file The file to check.
   * @throws FileNotFoundException If one of the conditions is not true.
   */
  public static void checkReadableFile(File file) throws FileNotFoundException {
    checkReadableFile(file.toPath());
  }


  /**
   * Verifies if a file exists, is a normal file and is readable. If this is not
   * the case, a FileNotFoundException with a nice message is thrown.
   *
   * @param file The file to check.
   * @throws FileNotFoundException If one of the conditions is not true.
   */
  public static void checkReadableFile(Path file) throws FileNotFoundException {
    Preconditions.checkNotNull(file);

    if (!java.nio.file.Files.exists(file)) {
      throw new FileNotFoundException("File " + file.toAbsolutePath() + " does not exist!");
    }

    if (!java.nio.file.Files.isRegularFile(file)) {
      throw new FileNotFoundException("File " + file.toAbsolutePath() + " is not a normal file!");
    }

    if (!java.nio.file.Files.isReadable(file)) {
      throw new FileNotFoundException("File " + file.toAbsolutePath() + " is not readable!");
    }
  }
}