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

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.FileWriteMode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.annotation.Nullable;

/**
 * This interface provides operations on a file path.
 * It mimics the behavior of {@link java.nio.file.Path}
 * but is usable on platforms where the latter is not available.
 * For creating Path instances, use {@link Paths}.
 * To reads and writes it provides access via the following interfaces:
 * - {@link com.google.common.io.CharSource}
 * - {@link com.google.common.io.CharSink}
 * - {@link com.google.common.io.ByteSource}
 * - {@link com.google.common.io.ByteSink}
 */
public interface Path {

  /**
   * Returns a File object representing the path object.
   *
   * @return A File object representing the path.
   */
  File toFile();

  /**
   * Returns the path that was used to create the instance.
   *
   * @return The path that was used to create the instance
   */
  String getOriginalPath();

  /**
   * Returns the name of the file or directory represented by this path.
   *
   * @return The name of the file or directory.
   */
  String getName();

  /**
   * Returns the absolute path.
   *
   * @return The absolute path
   */
  Path toAbsolutePath();

  /**
   * Returns the absolute path.
   *
   * @return The absolute path
   */
  String getAbsolutePath();

  /**
   * Returns the path represented by this instance.
   *
   * @return The path
   */
  String getPath();

  /**
   * Returns the path as canonical path.
   *
   * @return The canonical path.
   */
  String getCanonicalPath() throws IOException;

  /**
   * Returns a list containing the names of paths and directories contained within the current path.
   *
   * @return The contained files and directories
   */
  @Nullable
  String[] list();

  /**
   * Returns the path's parent.
   *
   * @return The parent
   */
  Path getParent();

  /**
   * Indicates whether the path is absolute or not.
   *
   * @return True, if the path is absolute, false otherwise
   */
  boolean isAbsolute();

  /**
   * Resolves the given path against this path.
   *
   * @see #resolve(Path)
   * @param other The path to be resolved
   *
   * @return The resolved path
   */
  Path resolve(String other);

  /**
   * Resolves the given path against this path.
   *
   * If the other parameter is an absolute path then this method trivially returns other.
   * If other is an empty path then this method trivially returns this path.
   * Otherwise this method considers this path to be a directory
   * and resolves the given path against this path.
   * In the simplest case, the given path does not have a root component,
   * in which case this method joins the given path to this path and returns a resulting path
   * that ends with the given path.
   * Where the given path has a root component then resolution is highly implementation dependent
   * and therefore unspecified.
   *
   * @param other The Path to be resolved
   *
   * @return The resolved path
   */
  Path resolve(Path other);

  /**
   * Returns a CharSource instance that is backed by the underlying path.
   *
   * @see CharSource
   *
   * @param charset The charset to use.
   *
   * @return A CharSource
   */
  CharSource asCharSource(Charset charset);

  /**
   * Returns a CharSink instance that is backed by the underlying path.
   *
   * @see CharSink
   *
   * @param charset The charset to use.
   * @param writeModes The write mode to use
   *
   * @return A CharSink
   */
  CharSink asCharSink(Charset charset, FileWriteMode... writeModes);

  /**
   * Returns a ByteSource instance that is backed by the underlying path.
   *
   * @see ByteSource
   *
   * @return A ByteSource
   */
  ByteSource asByteSource();

  /**
   * Returns a ByteSink instance that is backed by the underlying path.
   *
   * @see ByteSink
   *
   * @param writeModes The write mode to use
   * @return A ByteSink
   */
  ByteSink asByteSink(FileWriteMode... writeModes);

  /**
   * Indicates whether the path is empty.
   *
   * @return True, if the path is empty, false otherwise.
   */
  boolean isEmpty();

  /**
   * Tests whether the file or directory denoted by this abstract pathname
   * exists.
   *
   * @return  <code>true</code> if and only if the file or directory denoted
   *          by this abstract pathname exists; <code>false</code> otherwise
   */
  boolean exists();

  /**
   * Indicates whether the path is a directory.
   *
   * @return True, if the path is a directory, false otherwise
   */
  boolean isDirectory();

  /**
   * Indicates whether the path is a file.
   *
   * @return True, if the path is a file, false otherwise
   */
  boolean isFile();

  /**
   * Indicates whether the path can be read.
   *
   * @return True, if the path can be read, false otherwise
   */
  boolean canRead();

  /**
   * Deletes the path.
   *
   * @return True, if the path could be deleted, false otherwise
   */
  boolean delete();

  /**
   * Deletes the path when the virtual machine is terminated.
   */
  void deleteOnExit();

  /**
   * Creates all parent directories of the path.
   *
   * Might return false though some directories were created.
   *
   * @return True, if the directories could be created, otherwise false.
   */
  boolean mkdirs();

  /**
   * @see #getPath()
   */
  @Override
  String toString();
}
