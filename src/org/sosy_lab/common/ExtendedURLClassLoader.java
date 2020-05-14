// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Var;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Extension of {@link URLClassLoader} with an optional child-first strategy and optional overriding
 * of the search path for native libraries.
 */
final class ExtendedURLClassLoader extends URLClassLoader {

  private final ExtendedURLClassLoaderConfiguration config;

  private ExtendedURLClassLoader(ExtendedURLClassLoaderConfiguration pConfig) {
    super(
        pConfig.urls().toArray(new URL[0]),
        pConfig.parent().orElseGet(ClassLoader::getSystemClassLoader));
    config = pConfig;
  }

  @AutoValue
  @SuppressWarnings("NoFunctionalReturnType")
  abstract static class ExtendedURLClassLoaderConfiguration {
    abstract Optional<ClassLoader> parent();

    abstract ImmutableList<URL> urls();

    abstract Predicate<String> directLoadClasses();

    abstract Predicate<String> customLookupNativeLibraries();

    @AutoValue.Builder
    abstract static class AutoBuilder extends Classes.ClassLoaderBuilder<AutoBuilder> {
      AutoBuilder() {}

      @SuppressFBWarnings("DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED")
      @Override
      public final URLClassLoader build() {
        return new ExtendedURLClassLoader(autoBuild());
      }
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
      @Var Class<?> c = findLoadedClass(name);
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
        return path.orElseThrow().toAbsolutePath().toString();
      }
    }
    return super.findLibrary(libname);
  }
}
