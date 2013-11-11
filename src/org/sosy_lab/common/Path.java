/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.common;


import java.io.File;
import java.nio.charset.Charset;

import com.google.common.base.Joiner;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;


/**
 * This class provides operations on a file path.
 * It mimics the behavior of {@link java.nio.file.Path} by using the methods available with {@link java.io.File}.
 * Furthermore it provides access to a {@link com.google.common.io.CharSource} and {@link com.google.common.io.CharSink}
 * instance for the File object underlying the path.
 */
public class Path {

  private String path = "";
  private File fileInstance = null;

  public Path(String path, String... more) {
    if (more.length == 0) {
      this.path = path;
    } else if (path.isEmpty() && more.length == 0) {
      this.path = "";
    } else {
      char separatorChar = File.separatorChar;
      Joiner joiner = Joiner.on(separatorChar).skipNulls();

      if (path.isEmpty()) {
        this.path = joiner.join(more);
      } else {
        this.path = path + separatorChar + joiner.join(more);
      }
    }

    /*
     * new File() will throw a NullPointerException if the path is null.
     * Therefore use the empty path instead to prevent nasty exceptions.
     */
    if (path == null) {
      this.path = "";
    }
  }

  public static Path fromFile(File file) {
    return new Path(file.getPath());
  }

  public File toFile() {
    // Reuse the File object since it is immutable anyway. And also the path cannot be changed.
    if (fileInstance == null) {
      fileInstance = new File(path);
      return fileInstance;
    } else {
      return fileInstance;
    }
  }

  /**
   * Returns the path that was used to create the instance.
   *
   * @return The path that was used to create the instance
   */
  public String getOriginalPath() {
    return path;
  }

  /**
   * Returns the name of the file or directory represented by this path.
   *
   * @return The name of the file or directory.
   */
  public String getName() {
    return toFile().getName();
  }

  /**
   * Returns the absolute path.
   * {@link java.io.File#getAbsolutePath()}
   *
   * @return The absolute path
   */
  public Path toAbsolutePath() {
    return new Path(toFile().getAbsolutePath());
  }

  /**
   * Returns the path's parent.
   * {@link java.io.File#getParent()}
   *
   * @return The parent
   */
  public Path getParent() {
    return new Path(toFile().getParent());
  }

  /**
   * Indicates whether the path is absolute or not.
   * {@link java.io.File#isAbsolute()}
   *
   * @return True, if the path is absolute, false otherwise
   */
  public boolean isAbsolute() {
    return toFile().isAbsolute();
  }

  /**
   * Resolves the given path against this path.
   *
   * @see #resolve(Path)
   * @param other The path to be resolved
   *
   * @return The resolved path
   */
  public Path resolve(String other) {
    return resolve(new Path(other));
  }

  /**
   * Resolves the given path against this path.
   *
   * If the other parameter is an absolute path then this method trivially returns other.
   * If other is an empty path then this method trivially returns this path.
   * Otherwise this method considers this path to be a directory and resolves the given path against this path.
   * In the simplest case, the given path does not have a root component, in which case this method joins the given path to this path and returns a resulting path
   * that ends with the given path.
   * Where the given path has a root component then resolution is highly implementation dependent
   * and therefore unspecified.
   *
   * @see {@link java.nio.file.Path#resolve(java.nio.file.Path)}
   * @param other The Path to be resolved
   *
   * @return The resolved path
   */
  public Path resolve(Path other) {
    if (other.isAbsolute()) {
      return other;
    }

    if (other.isEmpty()) {
      return this;
    }

    // other has root component
    // TODO How can a path be not absolute and still have a root component?

    // other has no root component
    Path absolutePath;
    if (toFile().isFile()) {
      absolutePath = getParent().toAbsolutePath();
    } else {
      absolutePath = toAbsolutePath();
    }

    return new Path(absolutePath + File.separator + other.getOriginalPath());
  }

  /**
   * Returns a CharSource instance that is backed by the File underlying the path.
   *
   * @see com.google.common.io.CharSource
   *
   * @param charset The charset to use.
   *
   * @return A CharSource
   */
  public CharSource asCharSource(Charset charset) {
    return Files.asCharSource(toFile(), charset);
  }

  /**
   * Returns a CharSink instance that is backed by the File underlying the path.
   *
   * @see com.google.common.io.CharSink
   *
   * @param charset The charset to use.
   * @param writeMode The write mode to use
   *
   * @return A CharSink
   */
  public CharSink asCharSink(Charset charset, FileWriteMode... writeModes) {
    return Files.asCharSink(toFile(), charset, writeModes);
  }

  /**
   * Indicates whether the path is empty.
   *
   * @return True, if the path is empty, false otherwise.
   */
  public boolean isEmpty() {
    return path.isEmpty();
  }

  /**
   * @see java.io.File#toString()
   */
  @Override
  public String toString() {
    return toFile().toString();
  }
}