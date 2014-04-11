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
package org.sosy_lab.common.io;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;

import javax.annotation.Nullable;

import org.sosy_lab.common.Appenders;

import com.google.common.base.Strings;
import com.google.common.io.FileWriteMode;

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
  public static Path createTempFile(String prefix, @Nullable String suffix, @Nullable String content) throws IOException {
    Path path = Paths.createTempPath(prefix, suffix);
    path.deleteOnExit();

    if (!Strings.isNullOrEmpty(content)) {
      try {
        writeFile(path, content);
      } catch (IOException e) {
        // creation was successful, but writing failed
        // -> delete file
        delete(path, e);

        throw e;
      }
    }
    return path;
  }

  /**
   * Create a temporary file similar to
   * {@link java.io.File#createTempFile(String, String)}.
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
   * The file can be opened and closed multiple times, potentially from different processes.
   *
   * @param prefix
   * @param suffix
   * @return
   * @throws IOException
   */
  public static DeleteOnCloseFile createTempFile(String prefix, @Nullable String suffix) throws IOException {
    Path tempFile = Paths.createTempPath(prefix, suffix);
    return new DeleteOnCloseFile(tempFile);
  }

  /**
   * A simple wrapper around {@link Path} that calls
   * {@link Path#delete()} from {@link AutoCloseable#close()}.
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
      path.delete();
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
    boolean deletionSucceeded = path.delete();

    if (!deletionSucceeded) {
      IOException deleteException = new IOException("The file " + path + " could not be deleted.");

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
  public static void writeFile(Path file, Object content) throws IOException {
    checkNotNull(content);
    try (Writer w = openOutputFile(file)) {
      Appenders.appendTo(w, content);
    }
  }

  /**
   * Open a buffered Writer to a file with the default charset.
   * This method creates necessary parent directories beforehand.
   *
   * Note that using the default charset is often not a good idea,
   * because it varies from platform to platform.
   * Consider explicitly specifying a charset.
   *
   * TODO should we use UTF8 here instead?
   */
  public static Writer openOutputFile(Path file, FileWriteMode... options) throws IOException {
    return openOutputFile(file, Charset.defaultCharset(), options);
  }

  /**
   * Open a buffered Writer to a file.
   * This method creates necessary parent directories beforehand.
   */
  public static Writer openOutputFile(Path file, Charset charset,
      FileWriteMode... options) throws IOException {
    Path dir = file.getParent();
    if (dir != null) {
      dir.mkdirs();
    }

    return file.asCharSink(charset, options).openBufferedStream();
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
    try (Writer w = openOutputFile(file, FileWriteMode.APPEND)) {
      Appenders.appendTo(w, content);
    }
  }


  /**
   * Verifies if a file exists, is a normal file and is readable. If this is not
   * the case, a FileNotFoundException with a nice message is thrown.
   *
   * @param path The file to check.
   * @throws FileNotFoundException If one of the conditions is not true.
   */
  public static void checkReadableFile(Path path) throws FileNotFoundException {
    checkNotNull(path);

    if (!path.exists()) {
      throw new FileNotFoundException("File " + path.toAbsolutePath() + " does not exist!");
    }

    if (!path.isFile()) {
      throw new FileNotFoundException("File " + path.toAbsolutePath() + " is not a normal file!");
    }

    if (!path.canRead()) {
      throw new FileNotFoundException("File " + path.toAbsolutePath() + " is not readable!");
    }
  }

  /**
   * {@link com.google.common.io.Files#createParentDirs(java.io.File)}
   *
   * @param path
   * @throws IOException
   */
  public static void createParentDirs(Path path) throws IOException {
    checkNotNull(path);
    Path parent = path.getParent();
    if (parent == null) {
      // the parent is the root directory and therefore exists or cannot be created.
      return;
    }
    parent.mkdirs();
    if (!parent.isDirectory()) {
      throw new IOException("Unable to create parent directories of "+path);
    }
  }
}