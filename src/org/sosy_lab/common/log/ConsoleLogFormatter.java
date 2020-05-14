// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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

    if (pUseColors) {
      // Using colors is only good if stderr is connected to a terminal and not
      // redirected into a file.
      // AFAIK there is no way to determine this from Java, but at least there
      // is a way to determine whether stdout is connected to a terminal.
      // We assume that most users only redirect stderr if they also redirect
      // stdout, so this should be ok.
      if ((System.console() == null)
          // Windows terminal does not support colors
          || System.getProperty("os.name", "").startsWith("Windows")
          // https://no-color.org/
          || System.getenv("NO_COLOR") != null) {
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
