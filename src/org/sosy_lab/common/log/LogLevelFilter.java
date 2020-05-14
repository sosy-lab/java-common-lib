// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.log;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/** {@link Filter} implementation for blacklisting log levels. */
class LogLevelFilter implements Filter {

  private final Set<Level> excludeLevels;

  LogLevelFilter(List<Level> excludeLevels) {
    this.excludeLevels = ImmutableSet.copyOf(excludeLevels);
  }

  @Override
  public boolean isLoggable(LogRecord pRecord) {
    return !excludeLevels.contains(pRecord.getLevel());
  }
}
