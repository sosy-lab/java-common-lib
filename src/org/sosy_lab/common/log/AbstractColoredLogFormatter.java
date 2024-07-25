// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.log;

import com.google.common.base.MoreObjects;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.sosy_lab.common.io.IO;

/**
 * Abstract class for creating {@link Formatter}s that color {@link LogRecord}s with {@link
 * Level#SEVERE} and {@link Level#WARNING} red.
 */
abstract class AbstractColoredLogFormatter extends Formatter {

  private final boolean useColors;

  protected AbstractColoredLogFormatter(boolean pUseColors) {
    useColors = pUseColors && IO.mayUseColorForOutput();
  }

  @Override
  public final String format(LogRecord lr) {
    StringBuilder sb = new StringBuilder(200);

    if (useColors) {
      if (lr.getLevel().equals(Level.WARNING)) {
        sb.append("\033[1m"); // bold normal color
      } else if (lr.getLevel().equals(Level.SEVERE)) {
        sb.append("\033[31;1m"); // bold red color
      }
    }
    format(lr, sb);
    if (useColors) {
      sb.append("\033[m");
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("useColors", useColors).toString();
  }

  /**
   * Formats {@link LogRecord} using the provided {@link StringBuilder}.<br>
   * This method corresponds to {@link Formatter#format(LogRecord)} in a template method pattern.
   * The coloring behaviour is provided by superclass.
   *
   * @see Formatter#format(LogRecord)
   * @param lr the {@link LogRecord} to format.
   * @param sb the {@link StringBuilder} for {@link LogRecord} formatting.
   */
  protected abstract void format(LogRecord lr, StringBuilder sb);
}
