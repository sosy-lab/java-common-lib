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
package org.sosy_lab.common.log;

import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.annotation.concurrent.GuardedBy;

/**
 * This class may be used to read the log into a String.
 */
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
  public synchronized void publish(LogRecord record) {
    // code copied from java.util.logging.StreamHandler#publish(LogRecord)
    if (!isLoggable(record)) {
      return;
    }
    String msg;
    try {
      msg = getFormatter().format(record);
    } catch (Exception ex) {
      // We don't want to throw an exception here, but we
      // report the exception to any registered ErrorManager.
      reportError(null, ex, ErrorManager.FORMAT_FAILURE);
      return;
    }

    try {
      sb.append(msg);
    } catch (Exception ex) {
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