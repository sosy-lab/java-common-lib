/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Extension of {@link URLClassLoader} with an optional child-first strategy
 * and optional overriding of the search path for native libraries.
 */
final class ExtendedURLClassLoader extends URLClassLoader {

  private final ExtendedURLClassLoaderConfiguration config;

  ExtendedURLClassLoader(ExtendedURLClassLoaderConfiguration pConfig) {
    super(
        pConfig.urls().toArray(new URL[0]),
        pConfig.parent().orElseGet(ClassLoader::getSystemClassLoader));
    config = pConfig;
  }

  @AutoValue
  abstract static class ExtendedURLClassLoaderConfiguration {
    abstract Optional<ClassLoader> parent();

    abstract ImmutableList<URL> urls();

    abstract Predicate<String> directLoadClasses();

    abstract Predicate<String> customLookupNativeLibraries();

    @AutoValue.Builder
    abstract static class AutoBuilder extends Classes.ClassLoaderBuilder {
      AutoBuilder() {}

      @Override
      public abstract AutoBuilder setParent(ClassLoader parent);

      @Override
      public abstract AutoBuilder setUrls(Iterable<URL> urls);

      @Override
      public abstract AutoBuilder setDirectLoadClasses(Predicate<String> classes);

      @Override
      public abstract AutoBuilder setCustomLookupNativeLibraries(Predicate<String> nativeLibraries);
    }
  }

  @Override
  protected Class<?> loadClass(String name, boolean pResolve) throws ClassNotFoundException {
    if (!config.directLoadClasses().test(name)) {
      return super.loadClass(name, pResolve);
    }

    // This is the same code as in {@link URLClassLoader#loadClass(String, boolean)
    // except that it never asks the parent class loader
    synchronized (getClassLoadingLock(name)) {
      // First, check if the class has already been loaded
      Class<?> c = findLoadedClass(name);
      if (c == null) {
        // If still not found, then invoke findClass in order
        // to find the class.
        c = findClass(name);
      }
      if (pResolve) {
        resolveClass(c);
      }
      return c;
    }
  }

  @Override
  protected String findLibrary(String libname) {
    checkNotNull(libname);
    if (config.customLookupNativeLibraries().test(libname)) {
      @SuppressWarnings("deprecation")
      Optional<Path> path = NativeLibraries.findPathForLibrary(libname);
      if (path.isPresent()) {
        return path.get().toAbsolutePath().toString();
      }
    }
    return super.findLibrary(libname);
  }
}
