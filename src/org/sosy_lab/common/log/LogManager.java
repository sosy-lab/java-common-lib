// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.log;

import com.google.errorprone.annotations.FormatMethod;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.MoreStrings;

/**
 * Main interface for basic logging framework.
 *
 * <p>The log levels used are the ones from java.util.logging. SEVERE, WARNING and INFO are used
 * normally, the first two denoting (among other things) exceptions. FINE, FINER, FINEST, and ALL
 * correspond to main application, central algorithm, component level, and debug level respectively.
 *
 * <p>The main advantage of this interface is that the arguments to the log methods are only
 * converted to strings, if the message is really logged.
 */
public interface LogManager {

  /**
   * Returns a new LogManager instance which may use the given name as an indicator from which
   * component a log message comes from.
   *
   * @param name A non-empty string.
   * @return A LogManager instance.
   */
  LogManager withComponentName(String name);

  /**
   * Returns true if a message with the given log level would be logged.
   *
   * @param priority the log level
   * @return whether this log level is enabled
   */
  boolean wouldBeLogged(Level priority);

  /**
   * Logs any message occurring during program execution. The message is constructed lazily by
   * concatenating the parts with " ". The caller should not use string concatenation to create the
   * message in order to increase performance if the message is never logged. To make individual
   * arguments lazy, use {@link MoreStrings#lazyString(Supplier)}.
   *
   * @param priority the log level for the message
   * @param args the parts of the message (can be an arbitrary number of objects whose {@link
   *     Object#toString()} method is called)
   */
  void log(Level priority, Object... args);

  /**
   * Logs any message occurring during program execution. The message is constructed lazily by
   * asking the provided supplier if necessary.
   *
   * @param priority the log level for the message
   * @param msgSupplier a supplier for a non-null log message
   */
  void log(Level priority, Supplier<String> msgSupplier);

  /**
   * Logs any message occurring during program execution. The message is constructed lazily from
   * <code>String.format(format, args)</code>. To make individual arguments lazy, use {@link
   * MoreStrings#lazyString(Supplier)}.
   *
   * @param priority the log level for the message
   * @param format The format string.
   * @param args The arguments for the format string.
   */
  @FormatMethod
  void logf(Level priority, String format, Object... args);

  /**
   * Log a message by printing its message to the user. The details (e.g., stack trace) are hidden
   * from the user and logged with a lower log level.
   *
   * <p>Use this method in cases where an expected exception with a useful error message is thrown,
   * e.g. an InvalidConfigurationException.
   *
   * <p>If you want to log an IOException because of a write error, it is recommended to write the
   * message like "Could not write FOO to file". The final message will then be "Could not write FOO
   * to file FOO.txt (REASON)".
   *
   * @param priority the log level for the message
   * @param e the occurred exception
   * @param additionalMessage an optional message
   */
  void logUserException(Level priority, Throwable e, @Nullable String additionalMessage);

  /**
   * Log a message by printing its message to the user. The details (e.g., stack trace) are hidden
   * from the user and logged with a lower log level.
   *
   * <p>Use this method in cases where an expected exception with a useful error message is thrown,
   * e.g. an InvalidConfigurationException.
   *
   * <p>The message is constructed lazily from <code>String.format(format, args)</code>. To make
   * individual arguments lazy, use {@link MoreStrings#lazyString(Supplier)}.
   *
   * <p>If you want to log an IOException because of a write error, it is recommended to write the
   * message like "Could not write FOO to file". The final message will then be "Could not write FOO
   * to file FOO.txt (REASON)".
   *
   * @param priority the log level for the message
   * @param e the occurred exception
   * @param format The format string.
   * @param args The arguments for the format string.
   */
  @FormatMethod
  void logfUserException(Level priority, Throwable e, String format, Object... args);

  /**
   * Log an exception solely for the purpose of debugging. In default configuration, this exception
   * is not shown to the user!
   *
   * <p>Use this method when you want to log an exception that was handled by the catching site, but
   * you don't want to forget the information completely.
   *
   * @param e the occurred exception
   * @param additionalMessage an optional message
   */
  void logDebugException(Throwable e, @Nullable String additionalMessage);

  /**
   * Log an exception solely for the purpose of debugging. In default configuration, this exception
   * is not shown to the user!
   *
   * <p>Use this method when you want to log an exception that was handled by the catching site, but
   * you don't want to forget the information completely.
   *
   * <p>The message is constructed lazily from <code>String.format(format, args)</code>. To make
   * individual arguments lazy, use {@link MoreStrings#lazyString(Supplier)}.
   *
   * @param e the occurred exception
   * @param format The format string.
   * @param args The arguments for the format string.
   */
  @FormatMethod
  void logfDebugException(Throwable e, String format, Object... args);

  /**
   * Log an exception solely for the purpose of debugging. In default configuration, this exception
   * is not shown to the user!
   *
   * <p>Use this method when you want to log an exception that was handled by the catching site, but
   * you don't want to forget the information completely.
   *
   * @param e the occurred exception
   */
  void logDebugException(Throwable e);

  /**
   * Log an exception by printing the full details to the user.
   *
   * <p>This method should only be used in cases where logUserException and logDebugException are
   * not acceptable.
   *
   * @param priority the log level for the message
   * @param e the occurred exception
   * @param additionalMessage an optional message
   */
  void logException(Level priority, Throwable e, @Nullable String additionalMessage);

  /**
   * Log an exception by printing the full details to the user.
   *
   * <p>This method should only be used in cases where logUserException and logDebugException are
   * not acceptable.
   *
   * <p>The message is constructed lazily from <code>String.format(format, args)</code>. To make
   * individual arguments lazy, use {@link MoreStrings#lazyString(Supplier)}.
   *
   * @param priority the log level for the message
   * @param e the occurred exception
   * @param format The format string.
   * @param args The arguments for the format string.
   */
  @FormatMethod
  void logfException(Level priority, Throwable e, String format, Object... args);

  /** Flush all handlers of this logger. */
  void flush();

  /**
   * Return a LogManager that does not log anything.
   *
   * <p>Note: Do not use this implementation for unit tests, use {@link #createTestLogManager()}
   * instead.
   */
  @SuppressWarnings("deprecation")
  static LogManager createNullLogManager() {
    return NullLogManager.INSTANCE;
  }

  /**
   * Return a LogManager implementation intended for testing when nothing should actually be logged.
   *
   * <p>Compared to {@link #createTestLogManager()}, it does check all the parameters for validity,
   * i.e. non-nullness and correct string format.
   */
  @SuppressWarnings("deprecation")
  static LogManager createTestLogManager() {
    return TestLogManager.INSTANCE;
  }
}
