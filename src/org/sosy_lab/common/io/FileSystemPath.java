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


import com.google.common.base.Joiner;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.annotation.Nullable;


/**
 * This class provides operations on a file path by backing the path
 * with the {@link java.io.File} implementation.
 */
public class FileSystemPath implements Path {

  protected String path = "";
  private @Nullable File fileInstance = null;

  /**
   * Creates a new Path object from one or more path strings.
   *
   * @param path The path string to create the Path object from.
   * If null the empty string will be used.
   * @param more Additional path strings to use when creating the Path object.
   */
  public FileSystemPath(@Nullable String path, @Nullable String... more) {
    /*
     * new File() will throw a NullPointerException if the path is null.
     * Therefore use the empty path instead to prevent nasty exceptions.
     */
    if (path == null) {
      path = "";
    }

    if (more != null) {
      if (more.length == 0) {
        this.path = path;
      } else {
        char separatorChar = File.separatorChar;
        Joiner joiner = Joiner.on(separatorChar).skipNulls();

        if (path.isEmpty()) {
          this.path = joiner.join(more);
        } else {
          this.path = path + separatorChar + joiner.join(more);
        }
      }
    } else {
      this.path = path;
    }
  }

  @Override
  public File toFile() {
    // Reuse the File object since it is immutable anyway. And also the path cannot be changed.
    if (fileInstance == null) {
      fileInstance = new File(path);
    }

    return fileInstance;
  }

  @Override
  public String getOriginalPath() {
    return path;
  }

  @Override
  public String getName() {
    return toFile().getName();
  }

  @Override
  public Path toAbsolutePath() {
    return Paths.get(toFile().getAbsolutePath());
  }

  @Override
  public String getAbsolutePath() {
    return toFile().getAbsolutePath();
  }

  @Override
  public String getCanonicalPath() throws IOException {
    return toFile().getCanonicalPath();
  }

  @Override
  public @Nullable String[] list() {
    return toFile().list();
  }

  @Override
  public Path getParent() {
    return Paths.get(toFile().getParent());
  }

  @Override
  public String getPath() {
    return toFile().getPath();
  }

  @Override
  public boolean isAbsolute() {
    return toFile().isAbsolute();
  }

  @Override
  public Path resolve(@Nullable String other) {
    return resolve(Paths.get(other));
  }

  @Override
  public Path resolve(Path other) {
    if (other.isAbsolute()) {
      return other;
    }

    if (other.isEmpty()) {
      return this;
    }
    return Paths.get(this.getOriginalPath(), other.getOriginalPath());
  }

  @Override
  public CharSource asCharSource(Charset charset) {
    return Files.asCharSource(toFile(), charset);
  }

  @Override
  public CharSink asCharSink(Charset charset, FileWriteMode... writeModes) {
    return Files.asCharSink(toFile(), charset, writeModes);
  }

  @Override
  public ByteSource asByteSource() {
    return Files.asByteSource(toFile());
  }

  @Override
  public ByteSink asByteSink(FileWriteMode... writeModes) {
    return Files.asByteSink(toFile(), writeModes);
  }

  @Override
  public boolean isEmpty() {
    return path.isEmpty();
  }

  @Override
  public boolean exists() {
    return toFile().exists();
  }

  @Override
  public boolean isDirectory() {
    return toFile().isDirectory();
  }

  @Override
  public boolean isFile() {
    return toFile().isFile();
  }

  @Override
  public boolean canRead() {
    return toFile().canRead();
  }

  @Override
  public boolean delete() {
    return toFile().delete();
  }

  @Override
  public void deleteOnExit() {
    toFile().deleteOnExit();
  }

  @Override
  public boolean mkdirs() {
    return toFile().mkdirs();
  }

  @Override
  public String toString() {
    return toFile().toString();
  }

  /**
   * @see File#hashCode()
   */
  @Override
  public int hashCode() {
    return toFile().hashCode();
  }

  /**
   * @see File#equals(Object)
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    FileSystemPath other = (FileSystemPath) obj;
    return toFile().equals(other.toFile());
  }
}