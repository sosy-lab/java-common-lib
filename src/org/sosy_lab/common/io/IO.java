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
package org.sosy_lab.common.io;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.io.CharSource;
import com.google.common.io.MoreFiles;
import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPOutputStream;
import org.sosy_lab.common.Appenders;

/**
 * Provides helper functions for file access, in addition to {@link Files} and {@link
 * com.google.common.io.MoreFiles}.
 */
public final class IO {

  private IO() {
    /* utility class */
  }

  /** Read the full content of a {@link CharSource} to a new {@link StringBuilder}. */
  public static StringBuilder toStringBuilder(CharSource source) throws IOException {
    StringBuilder sb = new StringBuilder();
    source.copyTo(sb);
    return sb;
  }

  /** Read the full content of a {@link CharSource} to a char array. */
  public static char[] toCharArray(CharSource source) throws IOException {
    // On newer Java, StringBuilder internally uses byte[] instead of char[].
    // CharArrayWriter uses char[], so copying into the result array can be optimized more easily.
    // Code from https://github.com/google/guava/issues/2713#issuecomment-516574887
    CharArrayWriter writer = new CharArrayWriter();
    source.copyTo(writer);
    return writer.toCharArray();
  }

  /**
   * Writes content to a file.
   *
   * @param file The file.
   * @param content The content which shall be written.
   */
  public static void writeFile(Path file, Charset charset, Object content) throws IOException {
    checkNotNull(content);
    try (Writer w = openOutputFile(file, charset)) {
      Appenders.appendTo(w, content);
    }
  }

  /**
   * Writes content to a file compressed in GZIP format.
   *
   * @param file The file.
   * @param content The content which shall be written.
   */
  public static void writeGZIPFile(Path file, Charset charset, Object content) throws IOException {
    checkNotNull(content);
    checkNotNull(charset);
    MoreFiles.createParentDirectories(file);
    try (OutputStream outputStream = Files.newOutputStream(file);
        OutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
        Writer outputStreamWriter = new OutputStreamWriter(gzipOutputStream, charset);
        Writer w = new BufferedWriter(outputStreamWriter)) {
      Appenders.appendTo(w, content);
    }
  }

  /**
   * Open a buffered Writer to a file. This method creates necessary parent directories beforehand.
   */
  public static Writer openOutputFile(Path file, Charset charset, OpenOption... options)
      throws IOException {
    checkNotNull(charset);
    checkNotNull(options);
    MoreFiles.createParentDirectories(file);
    return MoreFiles.asCharSink(file, charset, options).openBufferedStream();
  }

  /**
   * Appends content to a file (without overwriting the file, but creating it if necessary).
   *
   * @param file The file.
   * @param content The content which will be written to the end of the file.
   */
  public static void appendToFile(Path file, Charset charset, Object content) throws IOException {
    checkNotNull(content);
    try (Writer w = openOutputFile(file, charset, StandardOpenOption.APPEND)) {
      Appenders.appendTo(w, content);
    }
  }

  /**
   * Verifies if a file exists, is a normal file and is readable. If this is not the case, a
   * FileNotFoundException with a nice message is thrown.
   *
   * @param path The file to check.
   * @throws FileNotFoundException If one of the conditions is not true.
   */
  public static void checkReadableFile(Path path) throws FileNotFoundException {
    checkNotNull(path);

    if (!Files.exists(path)) {
      throw new FileNotFoundException("File " + path.toAbsolutePath() + " does not exist!");
    }

    if (!Files.isRegularFile(path)) {
      throw new FileNotFoundException("File " + path.toAbsolutePath() + " is not a normal file!");
    }

    if (!Files.isReadable(path)) {
      throw new FileNotFoundException("File " + path.toAbsolutePath() + " is not readable!");
    }
  }
}
