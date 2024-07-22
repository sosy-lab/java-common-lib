// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.io;

import com.google.common.base.Joiner;
import com.google.common.testing.AbstractPackageSanityTests;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

@SuppressWarnings({"BanClassLoader", "resource"})
public class PackageSanityTest extends AbstractPackageSanityTests {

  {
    setDefault(String.class, "test");
    setDefault(String[].class, new String[] {"test"});
    setDefault(Joiner.MapJoiner.class, Joiner.on(",").withKeyValueSeparator("="));
    setDefault(ClassLoader.class, new URLClassLoader(new URL[0]));
    setDefault(Path.class, Path.of(""));
    try {
      setDefault(Constructor.class, PackageSanityTest.class.getConstructor());
      setDefault(Method.class, PackageSanityTest.class.getDeclaredMethod("defaultMethod"));
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }
  }

  @SuppressWarnings("unused")
  private static void defaultMethod() {}
}
