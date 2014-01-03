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
package org.sosy_lab.common.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.annotation.Nullable;


public class Paths {

  private static AbstractPathFactory factory = null;

  /**
   * Prevent instantiation.
   */
  private Paths() {}

  /**
   * Returns a Path instance created from a file.
   *
   * @param file The file to use
   * @return A Path instance created from a file
   */
  public static Path get(File file) {
    return get(file.getPath());
  }

  /**
   * Returns a Path instance created from a URI.
   *
   * @param uri The URI to use
   * @return A Path instance created from a URI
   */
  public static Path get(URI uri) {
    return get(new File(uri));
  }

  /**
   * Returns a Path instance created from one or more path names.
   *
   * @param pathName The path name
   * @param more Additional path strings to use when creating the Path object
   * @return A Path instance created from a path name
   */
  public static Path get(@Nullable String pathName, @Nullable String... more) {
    return getFactory().getPath(pathName, more);
  }

  /**
   * Sets the factory to be used for creating Path instances.
   *
   * @param pathFactory The path factory. If null {@link FileSystemPathFactory} will be used.
   */
  public static void setFactory(@Nullable AbstractPathFactory pathFactory) {
    factory = pathFactory;
  }

  /**
   * Returns the factory used to create instances.
   * If none was set a {@link FileSystemPathFactory} instance will be returned.
   *
   * @return The path factory
   */
  public static AbstractPathFactory getFactory() {
    if (factory == null) {
      factory = new FileSystemPathFactory();
    }

    return factory;
  }

  /**
   * @see AbstractPathFactory#getTempPath(String, String)
   */
  public static Path createTempPath(String prefix, @Nullable String suffix) throws IOException {
    return getFactory().getTempPath(prefix, suffix);
  }
}
