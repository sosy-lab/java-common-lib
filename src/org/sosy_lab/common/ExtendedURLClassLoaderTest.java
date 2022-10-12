// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.regex.Pattern;
import org.junit.Test;
import org.sosy_lab.common.Classes.ClassLoaderBuilder;

public class ExtendedURLClassLoaderTest {

  private static final Pattern TEST_DUMMY_PATTERN = Pattern.compile("dummy pattern");
  private static final Class<String> TEST_CLASS = String.class;

  private static ClassLoaderBuilder<?> newDefaultBuilder() {
    return Classes.makeExtendedURLClassLoader().setUrls();
  }

  @Test
  public void testCreateNoParent() throws IOException {
    try (URLClassLoader cl = newDefaultBuilder().build()) {
      assertThat(cl.getParent()).isEqualTo(ClassLoader.getSystemClassLoader());
    }
  }

  @Test
  public void testCreateWithParent() throws IOException {
    ClassLoader parent = new ClassLoader() {};
    try (URLClassLoader cl = newDefaultBuilder().setParent(parent).build()) {
      assertThat(cl.getParent()).isEqualTo(parent);
    }
  }

  @Test
  public void testCreateWithURLs() throws IOException {
    URL testUrl = new URL("file:///");
    try (URLClassLoader cl = newDefaultBuilder().setUrls(testUrl).build()) {
      assertThat(cl.getURLs()).asList().containsExactly(testUrl);
    }
  }

  @Test
  public void testDelegationNonMatching() throws IOException, ClassNotFoundException {
    try (URLClassLoader cl =
        newDefaultBuilder().setDirectLoadClasses(TEST_DUMMY_PATTERN).build()) {
      assertThat(cl.loadClass(TEST_CLASS.getName())).isEqualTo(TEST_CLASS);
    }
  }

  @Test
  public void testDelegationMatchingNotFound() throws IOException {
    String testClassName = TEST_CLASS.getName();
    try (URLClassLoader cl =
        newDefaultBuilder()
            .setDirectLoadClasses(Pattern.compile(Pattern.quote(testClassName)))
            .build()) {

      assertThrows(ClassNotFoundException.class, () -> cl.loadClass(testClassName));
    }
  }
}
