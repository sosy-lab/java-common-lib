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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.FileWriteMode;


/**
 * This interface provides operations on a file path.
 * It mimics the behavior of {@link java.nio.file.Path}.
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
  public File toFile();

  /**
   * Returns the path that was used to create the instance.
   *
   * @return The path that was used to create the instance
   */
  public String getOriginalPath();

  /**
   * Returns the name of the file or directory represented by this path.
   *
   * @return The name of the file or directory.
   */
  public String getName();

  /**
   * Returns the absolute path.
   *
   * @return The absolute path
   */
  public Path toAbsolutePath();

  /**
   * Returns the absolute path.
   *
   * @return The absolute path
   */
  public String getAbsolutePath();

  /**
   * Returns the path represented by this instance.
   *
   * @return The path
   */
  public String getPath();

  /**
   * Returns the path as canonical path.
   *
   * @return The canonical path.
   * @throws IOException
   */
  public String getCanonicalPath() throws IOException;

  /**
   * Returns a list containing the names of paths and directories contained within the current path.
   *
   * @return The contained files and directories
   */
  public String[] list();

  /**
   * Returns the path's parent.
   *
   * @return The parent
   */
  public Path getParent();

  /**
   * Indicates whether the path is absolute or not.
   *
   * @return True, if the path is absolute, false otherwise
   */
  public boolean isAbsolute();

  /**
   * Resolves the given path against this path.
   *
   * @see #resolve(Path)
   * @param other The path to be resolved
   *
   * @return The resolved path
   */
  public Path resolve(String other);

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
   * @param other The Path to be resolved
   *
   * @return The resolved path
   */
  public Path resolve(Path other);

  /**
   * Returns a CharSource instance that is backed by the underlying path.
   *
   * @see com.google.common.io.CharSource
   *
   * @param charset The charset to use.
   *
   * @return A CharSource
   */
  public CharSource asCharSource(Charset charset);

  /**
   * Returns a CharSink instance that is backed by the underlying path.
   *
   * @see com.google.common.io.CharSink
   *
   * @param charset The charset to use.
   * @param writeMode The write mode to use
   *
   * @return A CharSink
   */
  public CharSink asCharSink(Charset charset, FileWriteMode... writeModes);

  /**
   * Returns a ByteSource instance that is backed by the underlying path.
   *
   * @see com.google.io.ByteSource
   *
   * @return A ByteSource
   */
  public ByteSource asByteSource();

  /**
   * Returns a ByteSink instance that is backed by the underlying path.
   *
   * @see com.google.io.ByteSink
   *
   * @param writeModes The write mode to use
   * @return A ByteSink
   */
  public ByteSink asByteSink(FileWriteMode... writeModes);

  /**
   * Indicates whether the path is empty.
   *
   * @return True, if the path is empty, false otherwise.
   */
  public boolean isEmpty();

  /**
   * Indicates whether the path exists.
   *
   * @return True, if the path exists, false otherwise
   */
  public boolean exists();

  /**
   * Indicates whether the path is a directory.
   *
   * @return True, if the path is a directory, false otherwise
   */
  public boolean isDirectory();

  /**
   * Indicates whether the path is a file.
   *
   * @return True, if the path is a file, false otherwise
   */
  public boolean isFile();

  /**
   * Indicates whether the path can be read.
   *
   * @return True, if the path can be read, false otherwise
   */
  public boolean canRead();

  /**
   * Deletes the path.
   *
   * @return True, if the path could be deleted, false otherwise
   */
  public boolean delete();

  /**
   * Deletes the path when the virtual machine is terminated.
   */
  public void deleteOnExit();

  /**
   * Creates all parent directories of the path.
   *
   * Might return false though some directories were created.
   *
   * @return True, if the directories could be created, otherwise false.
   */
  public boolean mkdirs();

  /**
   * @see #getPath()
   */
  @Override
  public String toString();

}