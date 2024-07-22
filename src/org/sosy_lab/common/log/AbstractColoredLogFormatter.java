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
import java.io.Console;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Abstract class for creating {@link Formatter}s that color {@link LogRecord}s with {@link
 * Level#SEVERE} and {@link Level#WARNING} red.
 */
abstract class AbstractColoredLogFormatter extends Formatter {

  /**
   * Check whether {@link System#console()} represents a terminal. Java <= 21 and Java >= 22
   * represent this differently.
   *
   * <p>Cf. the <a href="https://errorprone.info/bugpattern/SystemConsoleNull">Error Prone docs</a>,
   * where this code is taken from.
   */
  @SuppressWarnings("SystemConsoleNull")
  private static boolean systemConsoleIsTerminal() {
    Console systemConsole = System.console();
    if (Runtime.version().feature() < 22) {
      return systemConsole != null;
    }
    try {
      return (Boolean) Console.class.getMethod("isTerminal").invoke(systemConsole);
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }

  private final boolean useColors;

  protected AbstractColoredLogFormatter(@Var boolean pUseColors) {
    if (pUseColors) {
      // Using colors is only good if stderr is connected to a terminal and not
      // redirected into a file.
      // AFAIK there is no way to determine this from Java, but at least there
      // is a way to determine whether stdout is connected to a terminal.
      // We assume that most users only redirect stderr if they also redirect
      // stdout, so this should be ok.
      if (systemConsoleIsTerminal()
          // Windows terminal does not support colors
          || System.getProperty("os.name", "").startsWith("Windows")
          // https://no-color.org/
          || System.getenv("NO_COLOR") != null) {
        pUseColors = false;
      }
    }
    useColors = pUseColors;
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
