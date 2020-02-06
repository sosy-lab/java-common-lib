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

import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.Var;
import java.util.Objects;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/** Class to handle formatting for console output. */
public class ConsoleLogFormatter extends Formatter {

  private final boolean useColors;

  public ConsoleLogFormatter(LoggingOptions options) {
    this(options.useColors());
  }

  private ConsoleLogFormatter(@Var boolean pUseColors) {
    // Using colors is only good if stderr is connected to a terminal and not
    // redirected into a file.
    // AFAIK there is no way to determine this from Java, but at least there
    // is a way to determine whether stdout is connected to a terminal.
    // We assume that most users only redirect stderr if they also redirect
    // stdout, so this should be ok.
    if (pUseColors) {
      if ((System.console() == null) || System.getProperty("os.name", "").startsWith("Windows")) {
        pUseColors = false;
      }
    }
    useColors = pUseColors;
  }

  public static Formatter withoutColors() {
    return new ConsoleLogFormatter(/*pUseColors=*/ false);
  }

  public static Formatter withColorsIfPossible() {
    return new ConsoleLogFormatter(/*pUseColors=*/ true);
  }

  @Override
  public String format(LogRecord lr) {
    StringBuilder sb = new StringBuilder(200);

    if (useColors) {
      if (lr.getLevel().equals(Level.WARNING)) {
        sb.append("\033[1m"); // bold normal color
      } else if (lr.getLevel().equals(Level.SEVERE)) {
        sb.append("\033[31;1m"); // bold red color
      }
    }
    sb.append(lr.getMessage()).append(" (");
    if (lr instanceof ExtendedLogRecord) {
      String component = ((ExtendedLogRecord) lr).getSourceComponentName();
      if (!component.isEmpty()) {
        sb.append(component).append(':');
      }
    }

    sb.append(Objects.requireNonNullElse(LogUtils.extractSimpleClassName(lr), "$Unknown$"))
        .append('.')
        .append(Objects.requireNonNullElse(lr.getSourceMethodName(), "$unknown$"))
        .append(", ")
        .append(lr.getLevel().toString())
        .append(')');
    if (useColors) {
      sb.append("\033[m");
    }
    sb.append("\n\n");

    return sb.toString();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("useColors", useColors).toString();
  }
}
