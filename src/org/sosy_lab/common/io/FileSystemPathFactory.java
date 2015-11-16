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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.StandardSystemProperty;
import com.google.common.base.Strings;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

public class FileSystemPathFactory implements AbstractPathFactory {

  @Override
  public Path getPath(@Nullable String pathName, @Nullable String... more) {
    return new FileSystemPath(pathName, more);
  }

  @Override
  public Path getTempPath(String prefix, @Nullable String suffix) throws IOException {
    checkNotNull(prefix);
    if (prefix.length() < 3) {
      throw new IllegalArgumentException("The prefix must at least be three characters long.");
    }

    if (suffix == null) {
      suffix = ".tmp";
    }

    //    String fileName = prefix + suffix;
    //    SecurityManager securityManager = new SecurityManager();
    //    securityManager.checkWrite(fileName);

    try {
      return getPath(File.createTempFile(prefix, suffix).getPath());
    } catch (IOException e) {
      // The message of this exception is often quite unhelpful,
      // improve it by adding the path were we attempted to write.
      String tmpDir = StandardSystemProperty.JAVA_IO_TMPDIR.value();
      if (e.getMessage() != null && e.getMessage().contains(tmpDir)) {
        throw e;
      }

      String fileName = Paths.get(tmpDir, prefix + "*" + suffix).getPath();
      if (Strings.nullToEmpty(e.getMessage()).isEmpty()) {
        throw new IOException(fileName, e);
      } else {
        throw new IOException(fileName + " (" + e.getMessage() + ")", e);
      }
    }
  }
}
