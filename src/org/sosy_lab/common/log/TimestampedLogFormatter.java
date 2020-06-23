// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.log;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Log formatter that produces output containing a timestamp. Each log message will look like this:
 * <pre>
 * timestamp {@link Level} (component:class.method) message
 * </pre>
 */
public class TimestampedLogFormatter extends AbstractColoredLogFormatter {

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")
          .withLocale(Locale.getDefault(Locale.Category.FORMAT))
          .withZone(ZoneId.systemDefault());

  protected TimestampedLogFormatter(boolean useColors) {
    super(useColors);
  }

  @Override
  public void format(LogRecord lr, StringBuilder sb) {
    DATE_FORMAT.formatTo(lr.getInstant(), sb);
    sb.append('\t').append(lr.getLevel()).append('\t');

    if (lr instanceof ExtendedLogRecord) {
      String component = ((ExtendedLogRecord) lr).getSourceComponentName();
      if (!component.isEmpty()) {
        sb.append(component).append(':');
      }
    }
    sb.append(Objects.requireNonNullElse(LogUtils.extractSimpleClassName(lr), "$Unknown$"))
        .append('.')
        .append(Objects.requireNonNullElse(lr.getSourceMethodName(), "$unknown$"))
        .append('\t')
        .append(lr.getMessage())
        .append("\n\n");
  }

  public static Formatter withoutColors() {
    return new TimestampedLogFormatter(/*useColors=*/ false);
  }

  public static Formatter withColorsIfPossible() {
    return new TimestampedLogFormatter(/*useColors=*/ true);
  }
}
