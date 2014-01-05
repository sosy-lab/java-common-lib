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

import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;


public class FileSystemPathTest {

  private java.nio.file.Path nioPath;
  private FileSystemPath commonPath;

  @Before
  public void before() {
    String filePath = "/src/org.sosy_lab.common/Path.java";
    nioPath = Paths.get(filePath);
    commonPath = new FileSystemPath(filePath);
  }

  @Test
  public void resolveDirectoryEndingInSeparator() throws Exception {
    String dirPathA = "/some/directory/with/separator/";
    String dirPathB = "some/directory";

    java.nio.file.Path nioDirA = Paths.get(dirPathA);
    java.nio.file.Path nioDirB = Paths.get(dirPathB);
    java.nio.file.Path resolvedNioPath = nioDirA.resolve(nioDirB);

    FileSystemPath commonDirA = new FileSystemPath(dirPathA);
    FileSystemPath commonDirB = new FileSystemPath(dirPathB);
    Path resolvedCommonPath = commonDirA.resolve(commonDirB);

    assertEquals(resolvedNioPath.toString(), resolvedCommonPath.toString());
  }

  @Test
  public void resolveDirectoryNotEndingInSeparator() throws Exception {
    String dirPathA = "/some/directory/without/separator";
    String dirPathB = "some/directory";

    java.nio.file.Path nioDirA = Paths.get(dirPathA);
    java.nio.file.Path nioDirB = Paths.get(dirPathB);
    java.nio.file.Path resolvedNioPath = nioDirA.resolve(nioDirB);

    FileSystemPath commonDirA = new FileSystemPath(dirPathA);
    FileSystemPath commonDirB = new FileSystemPath(dirPathB);
    Path resolvedCommonPath = commonDirA.resolve(commonDirB);

    assertEquals(resolvedNioPath.toString(), resolvedCommonPath.toString());
  }

  @Test
  public void resolveTwoFiles() throws Exception {
    java.nio.file.Path nioOther = Paths.get("PathTest.java");
    FileSystemPath commonOther = new FileSystemPath("PathTest.java");
    assertEquals(nioPath.resolve(nioOther).toString(), commonPath.resolve(commonOther).toString());
  }

  @Test
  public void constructorWithEmptyPath() throws Exception {
    FileSystemPath path = new FileSystemPath("");
    assertEquals("", path.getOriginalPath());
  }

  @Test
  public void constructorWithSinglePath() throws Exception {
    FileSystemPath path = new FileSystemPath("foo");
    assertEquals("foo", path.getOriginalPath());
  }

  @Test
  public void constructorWithMultiplePaths() throws Exception {
    FileSystemPath path = new FileSystemPath("foo", "bar", "baz");

    assertEquals("foo/bar/baz", path.getOriginalPath());
  }

  @Test
  public void constructorWithEmptyPathButMorePaths() throws Exception {
    FileSystemPath path = new FileSystemPath("", "bar", "baz");

    assertEquals("bar/baz", path.getOriginalPath());
  }

  @Test
  public void constructorWithNullPath() {
    FileSystemPath path = new FileSystemPath(null);
    path.toFile();

    assertEquals("", path.getOriginalPath());
  }

  @Test
  public void getParent() throws Exception {
    assertEquals(nioPath.getParent().toString(), commonPath.getParent().toString());
  }

  @Test
  public void toAbsolutePath() throws Exception {
    assertEquals(nioPath.toAbsolutePath().toString(), commonPath.toAbsolutePath().toString());
  }

  @Test
  public void getFileName() throws Exception {
    assertEquals(nioPath.getFileName().toString(), commonPath.getName().toString());
  }

  @Test
  public void isAbsolute() throws Exception {
    assertEquals(nioPath.isAbsolute(), commonPath.isAbsolute());
  }

  @Test
  public void toStringTest() throws Exception {
    assertEquals(nioPath.toString(), commonPath.toString());
  }

}
