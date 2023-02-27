// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.log;

import java.util.Objects;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/** Class to handle formatting for console output. */
public class ConsoleLogFormatter extends AbstractColoredLogFormatter {

  public ConsoleLogFormatter(LoggingOptions options) {
    this(options.useColors());
  }

  private ConsoleLogFormatter(boolean useColors) {
    super(useColors);
  }

  public static Formatter withoutColors() {
    return new ConsoleLogFormatter(/* useColors= */ false);
  }

  public static Formatter withColorsIfPossible() {
    return new ConsoleLogFormatter(/* useColors= */ true);
  }

  @Override
  protected void format(LogRecord lr, StringBuilder sb) {
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
    sb.append("\n\n");
  }
}
