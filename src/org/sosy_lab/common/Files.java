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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;

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
  public static File createTempFile(String prefix, @Nullable String suffix, @Nullable String content) throws IOException {
    File file = File.createTempFile(prefix, suffix);
    file.deleteOnExit();

    if (!Strings.isNullOrEmpty(content)) {
      try {
        writeFile(file, content);
      } catch (IOException e) {
        // creation was successful, but writing failed
        // -> delete file
        delete(Path.fromFile(file), e);

        throw e;
      }
    }
    return file;
  }

  /**
   * Create a temporary file similar to
   * {@link java.nio.file.Files#createTempFile(String, String, FileAttribute...)}.
   *
   * The resulting {@link Path} object is wrapped in a {@link DeleteOnCloseFile},
   * which deletes the file as soon as {@link DeleteOnCloseFile#close()} is called.
   *
   * It is recommended to use the following pattern:
   * <code>
   * try (DeleteOnCloseFile tempFile = Files.createTempFile(...)) {
   *   // use tempFile.toPath() for writing and reading of the temporary file
   * }
   * </code>
   *
   * The difference to using {@link StandardOpenOption#DELETE_ON_CLOSE} is that
   * the file can be opened and closed multiple times,
   * potentially from different processes.
   *
   * @param prefix
   * @param suffix
   * @return
   * @throws IOException
   */
  public static DeleteOnCloseFile createTempFile(@Nullable String prefix, @Nullable String suffix,
      FileAttribute<?>... attrs) throws IOException {
    return new DeleteOnCloseFile(java.nio.file.Files.createTempFile(prefix, suffix, attrs));
  }

  /**
   * A simple wrapper around {@link Path} that calls
   * {@link java.io.File#delete()} from {@link AutoCloseable#close()}.
   */
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
      path.toFile().delete();
    }
  }

  /**
   * Try to delete a file.
   *
   * If deleting fails, and an exception is given as second parameter,
   * the exception from the deletion is added to the given exception,
   * otherwise it is thrown.
   *
   * It is suggested to use this method as follows:
   * <code>
   * try {
   *    // write to file "f"
   * } catch (IOException e) {
   *    Files.delete(f, e);
   *    throw e;
   * }
   * </code>
   *
   * @param path The file to delete.
   * @param pendingException An optional pending exception.
   * @throws IOException If deletion fails and no pending exception was given.
   */
  public static void delete(Path path, @Nullable IOException pendingException) throws IOException {
    File file = path.toFile();
    boolean deletionSucceeded = file.delete();

    if (!deletionSucceeded) {
      IOException deleteException = new IOException("The file " + file + " could not be deleted.");

      if (pendingException != null) {
        pendingException.addSuppressed(deleteException);
      } else {
        throw deleteException;
      }
    }
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
    checkNotNull(content);
    try (Writer w = openOutputFile(file)) {
      Appenders.appendTo(w, content);
    }
  }

  /**
   * Open a BufferedWriter to a file with the default charset.
   * In addition to {@link java.nio.file.Files#newBufferedWriter(Path, Charset, OpenOption...)},
   * this method creates necessary parent directories first.
   *
   * Note that using the default charset is often not a good idea,
   * because it varies from platform to platform.
   * Consider using {@link #openOutputFile(Path, Charset, OpenOption...)}
   * and explicitly specifying a charset.
   *
   * TODO should we use UTF8 here instead?
   */
  public static BufferedWriter openOutputFile(Path file, OpenOption... options) throws IOException {
    return openOutputFile(file, Charset.defaultCharset(), options);
  }

  /**
   * Open a BufferedWriter to a file.
   * In addition to {@link java.nio.file.Files#newBufferedWriter(Path, Charset, OpenOption...)},
   * this method creates necessary parent directories first.
   */
  public static BufferedWriter openOutputFile(Path file, Charset charset,
      OpenOption... options) throws IOException {
    Path dir = file.getParent();
    if (dir != null) {
      java.nio.file.Files.createDirectories(dir);
    }

    return java.nio.file.Files.newBufferedWriter(file, charset, options);
  }

  /**
   * Appends content to a file (without overwriting the file,
   * but creating it if necessary).
   * @param file The file.
   * @param content The content which will be written to the end of the file.
   * @throws IOException
   */
  public static void appendToFile(File file, Object content) throws IOException {
    appendToFile(file.toPath(), content);
  }

  /**
   * Appends content to a file (without overwriting the file,
   * but creating it if necessary).
   * @param file The file.
   * @param content The content which will be written to the end of the file.
   * @throws IOException
   */
  public static void appendToFile(Path file, Object content) throws IOException {
    checkNotNull(content);
    try (Writer w = openOutputFile(file, APPEND, CREATE)) {
      Appenders.appendTo(w, content);
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
    checkReadableFile(org.sosy_lab.common.Path.fromFile(file));
  }


  /**
   * Verifies if a file exists, is a normal file and is readable. If this is not
   * the case, a FileNotFoundException with a nice message is thrown.
   *
   * @param path The file to check.
   * @throws FileNotFoundException If one of the conditions is not true.
   */
  public static void checkReadableFile(org.sosy_lab.common.Path path) throws FileNotFoundException {
    Preconditions.checkNotNull(path);

    if (!path.toFile().exists()) {
      throw new FileNotFoundException("File " + path.toAbsolutePath() + " does not exist!");
    }

    if (!path.toFile().isFile()) {
      throw new FileNotFoundException("File " + path.toAbsolutePath() + " is not a normal file!");
    }

    if (!path.toFile().canRead()) {
      throw new FileNotFoundException("File " + path.toAbsolutePath() + " is not readable!");
    }
  }
}