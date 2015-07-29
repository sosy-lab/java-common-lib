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
package org.sosy_lab.common.io;

import static com.google.common.truth.Truth.assertThat;

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

    assertThat(resolvedNioPath.toString()).isEqualTo(resolvedCommonPath.toString());
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

    assertThat(resolvedNioPath.toString()).isEqualTo(resolvedCommonPath.toString());
  }

  @Test
  public void resolveTwoFiles() throws Exception {
    java.nio.file.Path nioOther = Paths.get("PathTest.java");
    FileSystemPath commonOther = new FileSystemPath("PathTest.java");
    assertThat(nioPath.resolve(nioOther).toString()).isEqualTo(commonPath.resolve(commonOther).toString());
  }

  @Test
  public void constructorWithEmptyPath() throws Exception {
    FileSystemPath path = new FileSystemPath("");
    assertThat(path.getOriginalPath()).isEqualTo("");
  }

  @Test
  public void constructorWithSinglePath() throws Exception {
    FileSystemPath path = new FileSystemPath("foo");
    assertThat(path.getOriginalPath()).isEqualTo("foo");
  }

  @Test
  public void constructorWithMultiplePaths() throws Exception {
    FileSystemPath path = new FileSystemPath("foo", "bar", "baz");

    assertThat(path.getOriginalPath()).isEqualTo("foo/bar/baz");
  }

  @Test
  public void constructorWithEmptyPathButMorePaths() throws Exception {
    FileSystemPath path = new FileSystemPath("", "bar", "baz");

    assertThat(path.getOriginalPath()).isEqualTo("bar/baz");
  }

  @Test
  public void constructorWithNullPath() {
    FileSystemPath path = new FileSystemPath(null);
    path.toFile();

    assertThat(path.getOriginalPath()).isEmpty();
  }

  @Test
  public void getParent() throws Exception {
    assertThat(nioPath.getParent().toString()).isEqualTo(commonPath.getParent().toString());
  }

  @Test
  public void toAbsolutePath() throws Exception {
    assertThat(nioPath.toAbsolutePath().toString()).isEqualTo(commonPath.toAbsolutePath().toString());
  }

  @Test
  public void getFileName() throws Exception {
    assertThat(nioPath.getFileName().toString()).isEqualTo(commonPath.getName().toString());
  }

  @Test
  public void isAbsolute() throws Exception {
    assertThat(nioPath.isAbsolute()).isEqualTo(commonPath.isAbsolute());
  }

  @Test
  public void toStringTest() throws Exception {
    assertThat(nioPath.toString()).isEqualTo(commonPath.toString());
  }

  @Test
  public void testEquals() throws Exception {
    Path pathA = new FileSystemPath("1", (String[])null);
    Path pathB = new FileSystemPath(null, "1");
    assertThat(pathA).isEqualTo(pathB);

    pathA = new FileSystemPath("1", "1");
    pathB = new FileSystemPath(null, "1", "1");
    assertThat(pathA).isEqualTo(pathB);

    pathA = new FileSystemPath("");
    pathB = new FileSystemPath(null, "");
    assertThat(pathA).isEqualTo(pathB);

    pathA = new FileSystemPath("pathA");
    pathB = new FileSystemPath("pathB");
    assertThat(pathA).isNotEqualTo(pathB);

    pathA = new FileSystemPath("pathA");
    pathB = pathA;
    assertThat(pathA).isEqualTo(pathB);
  }

}
