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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;


public class Paths {

  /**
   * Represents the environment the application is running on.
   */
  public static enum Environment {
    /**
     * The application can use the file system.
     */
    FILE_SYSTEM,

    /**
     * The application is running on Google App Engine and can use the file system only for reads.
     */
    APP_ENGINE
  }

  private static Environment environment = null;
  private static Map<Environment, Class<? extends Path>> implementations = new HashMap<>();

  /**
   * Prevent instantiation.
   */
  private Paths() {}

  /**
   * Sets the environment the application is running on.
   * This affects which implementation of {@link Path} will be used.
   *
   * @param env The environment
   */
  public static void setEnvironment(@Nullable Environment env) {
    environment = env;
  }

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
   * Registers an implementation of {@link Path} with an {@link Environment}.
   *
   * @param env The environment
   * @param pathImpl The Path implementation.
   */
  public static void registerImplementation(Environment env, Class<? extends Path> pathImpl) {
    checkNotNull(env);
    checkNotNull(pathImpl);
    implementations.put(env, pathImpl);
  }

  /**
   * Returns a Path instance created from one or more path names.
   *
   * @param pathName The path name
   * @param more Additional path strings to use when creating the Path object
   * @return A Path instance created from a path name
   */
  public static Path get(@Nullable String pathName, @Nullable String... more) {
    // set FILE_SYSTEM as default environment
    if (environment == null) {
      setEnvironment(Environment.FILE_SYSTEM);
    }

    // register file system implementation if not yet done
    if (environment == Environment.FILE_SYSTEM && !implementations.containsKey(Environment.FILE_SYSTEM)) {
      registerImplementation(Environment.FILE_SYSTEM, FileSystemPath.class);
    }

    // TODO handle case if no fitting imp is registered

    Path path = null;
    try {
      Constructor<? extends Path> constructor = implementations.get(environment).getConstructor(String.class, String[].class);
      path = constructor.newInstance(pathName, more);
    } catch (NoSuchMethodException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SecurityException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InstantiationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return path;
  }

  /**
   *
   * @param prefix
   * @param suffix
   * @return
   * @throws IOException
   */
  public static Path createTempPath(String prefix, @Nullable String suffix) throws IOException {

    // TODO handle different environments

    checkNotNull(prefix);
    if (prefix.length() < 3) {
      throw new IllegalArgumentException("The prefix must at least be three characters long.");
    }

    if (suffix == null) {
      suffix = ".tmp";
    }

//    String fileName = prefix + suffix;
//    SecurityManager securityManager = new SecurityManager();
//    securityManager.checkWrite(fileName);

    return get(File.createTempFile(prefix, suffix));
  }
}
