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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.regex.Pattern;
import org.junit.Test;
import org.sosy_lab.common.Classes.ClassLoaderBuilder;

public class ExtendedURLClassLoaderTest {

  private static final Class<String> TEST_CLASS = String.class;

  private ClassLoaderBuilder<?> newDefaultBuilder() {
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
    final URL testUrl = new URL("file:///");
    try (URLClassLoader cl = newDefaultBuilder().setUrls(testUrl).build()) {
      assertThat(cl.getURLs()).asList().containsExactly(testUrl);
    }
  }

  @Test
  public void testDelegationNonMatching() throws IOException, ClassNotFoundException {
    try (URLClassLoader cl =
        newDefaultBuilder().setDirectLoadClasses(Pattern.compile("dummy pattern")).build()) {
      assertThat(cl.loadClass(TEST_CLASS.getName())).isEqualTo(TEST_CLASS);
    }
  }

  @Test
  public void testDelegationMatchingNotFound() throws IOException {
    try (URLClassLoader cl =
        newDefaultBuilder()
            .setDirectLoadClasses(Pattern.compile(Pattern.quote(TEST_CLASS.getName())))
            .build()) {
      try {
        cl.loadClass(TEST_CLASS.getName());
        fail();
      } catch (ClassNotFoundException e) {
      }
    }
  }
}
