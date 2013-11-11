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
package org.sosy_lab.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class PathTest {

  @Test
  public void resolveDirectoryEndingInSeparator() throws Exception {
    Path dirA = new Path("/some/directory/with/separator/");
    Path dirB = new Path("some/directory");

    assertEquals("/some/directory/with/separator/some/directory", dirA.resolve(dirB).toFile().getPath());
  }

  @Test
  public void resolveDirectoryNotEndingInSeparator() throws Exception {
    Path dirA = new Path("/some/directory/without/separator");
    Path dirB = new Path("some/directory");

    assertEquals("/some/directory/without/separator/some/directory", dirA.resolve(dirB).toFile().getPath());
  }

  @Test
  public void constructorWithEmptyPath() throws Exception {
    Path path = new Path("");
    assertEquals("", path.getOriginalPath());
  }

  @Test
  public void constructorWithSinglePath() throws Exception {
    Path path = new Path("foo");
    assertEquals("foo", path.getOriginalPath());
  }

  @Test
  public void constructorWithMultiplePaths() throws Exception {
    Path path = new Path("foo", "bar", "baz");

    assertEquals("foo/bar/baz", path.getOriginalPath());
  }

  @Test
  public void constructorWithEmptyPathButMorePaths() throws Exception {
    Path path = new Path("", "bar", "baz");

    assertEquals("bar/baz", path.getOriginalPath());
  }

  @Test
  public void constructorWithNullPath() {
    Path path = new Path(null);
    path.toFile();

    assertEquals("", path.getOriginalPath());
  }

}
