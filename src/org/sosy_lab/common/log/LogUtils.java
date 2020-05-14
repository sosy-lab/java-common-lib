// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.log;

import java.util.logging.LogRecord;
import org.checkerframework.checker.nullness.qual.Nullable;

final class LogUtils {

  private LogUtils() {}

  /** Get the simple name of the source class of a log record. */
  static @Nullable String extractSimpleClassName(LogRecord lr) {
    String fullClassName = lr.getSourceClassName();
    if (fullClassName == null) {
      return null;
    }
    int dotIndex = fullClassName.lastIndexOf('.');
    assert dotIndex < fullClassName.length() - 1 : "Last character in a class name cannot be a dot";

    // if no dot is contained, dotIndex is -1 so we get the substring from 0,
    // i.e., the whole string (which is what we want)

    String className = fullClassName.substring(dotIndex + 1);
    return className;
  }
}
