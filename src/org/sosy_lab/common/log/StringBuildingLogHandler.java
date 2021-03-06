// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.log;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import org.checkerframework.checker.nullness.qual.Nullable;

/** This class may be used to read the log into a String. */
public class StringBuildingLogHandler extends Handler {

  @GuardedBy("this")
  private final StringBuilder sb = new StringBuilder();

  @Override
  public void close() {
    // ignore
  }

  @Override
  public void flush() {
    // ignore
  }

  @Override
  public synchronized void publish(@Nullable LogRecord record) {
    // code copied from java.util.logging.StreamHandler#publish(LogRecord)
    if (!isLoggable(record)) {
      return;
    }
    String msg;
    try {
      msg = getFormatter().format(record);
    } catch (RuntimeException ex) {
      // We don't want to throw an exception here, but we
      // report the exception to any registered ErrorManager.
      reportError(null, ex, ErrorManager.FORMAT_FAILURE);
      return;
    }

    try {
      sb.append(msg);
    } catch (RuntimeException ex) {
      // We don't want to throw an exception here, but we
      // report the exception to any registered ErrorManager.
      reportError(null, ex, ErrorManager.WRITE_FAILURE);
    }
  }

  public synchronized String getLog() {
    return sb.toString();
  }

  public synchronized void clear() {
    sb.setLength(0);
    sb.trimToSize(); // free memory
  }
}
