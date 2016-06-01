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
package org.sosy_lab.common;

import com.google.common.base.Joiner;
import com.google.common.reflect.Invokable;
import com.google.common.testing.AbstractPackageSanityTests;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PackageSanityTest extends AbstractPackageSanityTests {

  {
    setDefault(String[].class, new String[] {"test"});
    setDefault(Joiner.MapJoiner.class, Joiner.on(",").withKeyValueSeparator("="));
    setDefault(ClassLoader.class, new URLClassLoader(new URL[0]));
    setDefault(Path.class, Paths.get(""));
    try {
      setDefault(Constructor.class, PackageSanityTest.class.getConstructor());
      setDefault(Method.class, PackageSanityTest.class.getDeclaredMethod("defaultMethod"));
      setDefault(Executable.class, PackageSanityTest.class.getConstructor());
      setDefault(Invokable.class, Invokable.from(PackageSanityTest.class.getConstructor()));
    } catch (NoSuchMethodException | SecurityException e) {
      throw new AssertionError(e);
    }
  }

  @SuppressWarnings("unused")
  private void defaultMethod() {}
}
