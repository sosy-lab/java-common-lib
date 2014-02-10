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
package org.sosy_lab.common.io;

import java.io.IOException;

public interface AbstractPathFactory {

  /**
   * Returns a Path instance constructed from the given path name(s).
   *
   * @param pathName The path's name.
   * @param more Additional path names.
   *
   * @return A Path instance
   */
  public Path getPath(String pathName, String... more);

  /**
  * Creates a temporary path and returns the according Path instance.
  *
  * @param prefix The path's prefix. At least 3 characters long.
  * @param suffix The path's suffix. Will default to .tmp if null.
  *
  * @return The temporary Path.
  *
  * @throws IOException If the path could not be created.
  * @throws NullPointerException If the prefix is null.
  * @throws IllegalArgumentException if the prefix is shorter than 3 characters.
  */
  public Path getTempPath(String prefix, String suffix) throws IOException;
}
