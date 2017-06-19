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

import com.google.common.base.StandardSystemProperty;
import com.google.common.base.Strings;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.FileWriteMode;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import org.sosy_lab.common.Appenders;

/** Provides helper functions for file access. */
public final class MoreFiles {

  private MoreFiles() {
    /* utility class */
  }

  /** @see com.google.common.io.Files#asByteSink(java.io.File, FileWriteMode...) */
  public static ByteSink asByteSink(Path path, FileWriteMode... options) {
    return com.google.common.io.MoreFiles.asByteSink(path, fileWriteModeToOption(options));
  }

  /** @see com.google.common.io.Files#asByteSource(java.io.File) */
  public static ByteSource asByteSource(Path path) {
    return com.google.common.io.MoreFiles.asByteSource(path);
  }

  /** @see com.google.common.io.Files#asCharSink(java.io.File, Charset, FileWriteMode...) */
  public static CharSink asCharSink(Path path, Charset charset, FileWriteMode... options) {
    return com.google.common.io.MoreFiles.asCharSink(path, charset, fileWriteModeToOption(options));
  }

  /** @see com.google.common.io.Files#asCharSource(java.io.File, Charset) */
  public static CharSource asCharSource(Path path, Charset charset) {
    return com.google.common.io.MoreFiles.asCharSource(path, charset);
  }

  private static OpenOption[] fileWriteModeToOption(FileWriteMode[] modes) {
    boolean append = false;
    for (FileWriteMode mode : modes) {
      if (mode == FileWriteMode.APPEND) {
        append = true;
      } else if (mode != null) {
        throw new AssertionError("unknown FileWriteMode " + mode);
      }
    }
    return append ? new OpenOption[] {StandardOpenOption.APPEND} : new OpenOption[0];
  }

  /**
   * Creates a temporary file with an optional content. The file is marked for deletion when the
   * Java VM exits.
   *
   * @param prefix The prefix string to be used in generating the file's name; must be at least
   *     three characters long
   * @param suffix The suffix string to be used in generating the file's name; may be <code>null
   *     </code>, in which case the suffix <code>".tmp"</code> will be used
   * @param content The content to write (may be null). Will be written with default charset.
   * @throws IllegalArgumentException If the <code>prefix</code> argument contains fewer than three
   *     characters
   * @throws IOException If a file could not be created
   */
  public static Path createTempFile(
      String prefix, @Nullable String suffix, @Nullable String content) throws IOException {
    if (prefix.length() < 3) {
      throw new IllegalArgumentException("The prefix must at least be three characters long.");
    }

    if (suffix == null) {
      suffix = ".tmp";
    }

    File file;
    try {
      file = File.createTempFile(prefix, suffix);
    } catch (IOException e) {
      // The message of this exception is often quite unhelpful,
      // improve it by adding the path were we attempted to write.
      String tmpDir = StandardSystemProperty.JAVA_IO_TMPDIR.value();
      if (e.getMessage() != null && e.getMessage().contains(tmpDir)) {
        throw e;
      }

      String fileName = Paths.get(tmpDir, prefix + "*" + suffix).toString();
      if (Strings.nullToEmpty(e.getMessage()).isEmpty()) {
        throw new IOException(fileName, e);
      } else {
        throw new IOException(fileName + " (" + e.getMessage() + ")", e);
      }
    }

    Path path = file.toPath();
    file.deleteOnExit();

    if (!Strings.isNullOrEmpty(content)) {
      try {
        writeFile(path, Charset.defaultCharset(), content);
      } catch (IOException e) {
        // creation was successful, but writing failed
        // -> delete file
        try {
          Files.delete(path);
        } catch (IOException deleteException) {
          e.addSuppressed(deleteException);
        }
        throw e;
      }
    }
    return path;
  }

  /**
   * Create a temporary file similar to {@link java.io.File#createTempFile(String, String)}.
   *
   * <p>The resulting {@link Path} object is wrapped in a {@link DeleteOnCloseFile}, which deletes
   * the file as soon as {@link DeleteOnCloseFile#close()} is called.
   *
   * <p>It is recommended to use the following pattern: <code>
   * try (DeleteOnCloseFile tempFile = Files.createTempFile(...)) {
   *   // use tempFile.toPath() for writing and reading of the temporary file
   * }
   * </code> The file can be opened and closed multiple times, potentially from different processes.
   */
  public static DeleteOnCloseFile createTempFile(String prefix, @Nullable String suffix)
      throws IOException {
    Path tempFile = createTempFile(prefix, suffix, null);
    return new DeleteOnCloseFile(tempFile);
  }

  /**
   * A simple wrapper around {@link Path} that calls {@link Files#deleteIfExists(Path)} from {@link
   * AutoCloseable#close()}.
   */
  @javax.annotation.concurrent.Immutable
  public static class DeleteOnCloseFile implements AutoCloseable {

    private final Path path;

    private DeleteOnCloseFile(Path pFile) {
      path = pFile;
    }

    public Path toPath() {
      return path;
    }

    @Override
    public void close() throws IOException {
      Files.deleteIfExists(path);
    }
  }

  /**
   * Read the full content of a file.
   *
   * @param file The file.
   * @deprecated use {@code asCharSource(file, charset).read()}
   */
  @Deprecated
  public static String toString(Path file, Charset charset) throws IOException {
    return asCharSource(file, charset).read();
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
    createParentDirs(file);
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
  public static Writer openOutputFile(Path file, Charset charset, FileWriteMode... options)
      throws IOException {
    checkNotNull(charset);
    checkNotNull(options);
    createParentDirs(file);
    return asCharSink(file, charset, options).openBufferedStream();
  }

  /**
   * Appends content to a file (without overwriting the file, but creating it if necessary).
   *
   * @param file The file.
   * @param content The content which will be written to the end of the file.
   */
  public static void appendToFile(Path file, Charset charset, Object content) throws IOException {
    checkNotNull(content);
    try (Writer w = openOutputFile(file, charset, FileWriteMode.APPEND)) {
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

  /** @see com.google.common.io.Files#createParentDirs(java.io.File) */
  public static void createParentDirs(Path path) throws IOException {
    com.google.common.io.MoreFiles.createParentDirectories(path);
  }
}
