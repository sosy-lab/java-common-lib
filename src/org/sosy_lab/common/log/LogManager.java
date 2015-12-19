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

import java.util.logging.Level;

import javax.annotation.Nullable;

/**
 * Main interface for basic logging framework.
 *
 * The log levels used are the ones from java.util.logging.
 * SEVERE, WARNING and INFO are used normally, the first two denoting (among other things)
 * exceptions. FINE, FINER, FINEST, and ALL correspond to main application, central algorithm,
 * component level, and debug level respectively.
 *
 * The main advantage of this interface is that the arguments to the log methods
 * are only converted to strings, if the message is really logged.
 */
public interface LogManager {

  /**
   * Returns a new LogManager instance which may use the given name
   * as an indicator from which component a log message comes from.
   * @param name A non-empty string.
   * @return A LogManager instance.
   */
  LogManager withComponentName(String name);

  /**
   * Returns true if a message with the given log level would be logged.
   * @param priority the log level
   * @return whether this log level is enabled
   */
  boolean wouldBeLogged(Level priority);

  /**
   * Logs any message occurring during program execution.
   * The message is constructed lazily by concatenating the parts with " ".
   * The caller should not use string concatenation to create the message
   * in order to increase performance if the message is never logged.
   *
   * @param priority the log level for the message
   * @param args the parts of the message
   * (can be an arbitrary number of objects whose {@link Object#toString()} method is called)
   */
  void log(Level priority, Object... args);

  /**
   * Logs any message occurring during program execution.
   * The message is constructed lazily from <code>String.format(format, args)</code>.
   *
   * @param priority the log level for the message
   * @param format The format string.
   * @param args The arguments for the format string.
   */
  void logf(Level priority, String format, Object... args);

  /**
   * Log a message by printing its message to the user.
   * The details (e.g., stack trace) are hidden from the user and logged with
   * a lower log level.
   *
   * Use this method in cases where an expected exception with a useful error
   * message is thrown, e.g. an InvalidConfigurationException.
   *
   * If you want to log an IOException because of a write error, it is recommended
   * to write the message like "Could not write FOO to file". The final message
   * will then be "Could not write FOO to file FOO.txt (REASON)".
   *
   * @param priority the log level for the message
   * @param e the occurred exception
   * @param additionalMessage an optional message
   */
  void logUserException(Level priority, Throwable e, @Nullable String additionalMessage);

  /**
   * Log an exception solely for the purpose of debugging.
   * In default configuration, this exception is not shown to the user!
   *
   * Use this method when you want to log an exception that was handled by the
   * catching site, but you don't want to forget the information completely.
   *
   * @param e the occurred exception
   * @param additionalMessage an optional message
   */
  void logDebugException(Throwable e, @Nullable String additionalMessage);

  /**
   * Log an exception solely for the purpose of debugging.
   * In default configuration, this exception is not shown to the user!
   *
   * Use this method when you want to log an exception that was handled by the
   * catching site, but you don't want to forget the information completely.
   *
   * @param e the occurred exception
   */
  void logDebugException(Throwable e);

  /**
   * Log an exception by printing the full details to the user.
   *
   * This method should only be used in cases where logUserException and
   * logDebugException are not acceptable.
   *
   * @param priority the log level for the message
   * @param e the occurred exception
   * @param additionalMessage an optional message
   */
  void logException(Level priority, Throwable e, @Nullable String additionalMessage);

  void flush();
}
