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
package org.sosy_lab.common;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.regex.Pattern;

/**
 * This is a {@link URLClassLoader} that behaves like a normal class loader
 * except that it loads some classes always by itself, even if the parent
 * class loader would also have been available to load them.
 *
 * Normal class loaders follow the parent-first strategy, so they never load
 * classes which their parent could also load. This class loader follows the
 * child-first strategy for a specific set of classes (given by a pattern)
 * and the parent-first strategy for the rest.
 *
 * This class loader can be used if you want to load a component with its own
 * class loader (so that it can be garbage collected independently, for example),
 * but the parent class loader also sees the classes.
 */
public class ChildFirstPatternClassLoader extends URLClassLoader {

  private final Pattern classPattern;

  /**
   * Create a new class loader.
   * @param pClassPattern The pattern telling which classes should never be loaded by the parent.
   * @param pUrls The sources where this class loader should load classes from.
   * @param pParent The parent class loader.
   */
  public ChildFirstPatternClassLoader(Pattern pClassPattern, URL[] pUrls, ClassLoader pParent) {
    super(pUrls, checkNotNull(pParent));
    classPattern = checkNotNull(pClassPattern);
  }

  @Override
  protected Class<?> loadClass(String name, boolean pResolve) throws ClassNotFoundException {
    if (!classPattern.matcher(name).matches()) {
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
}
