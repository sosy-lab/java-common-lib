/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.sosy_lab.common.AbstractMBean;
import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;


/**
 * Default implementation of {@link LogManager}.
 */
public class BasicLogManager implements org.sosy_lab.common.log.LogManager {

  // Number of characters taken from the start of the original output strings when truncating
  private static final int TRUNCATE_REMAINING_SIZE = 100;

  private static final Level EXCEPTION_DEBUG_LEVEL = Level.ALL;
  private static final Joiner MESSAGE_FORMAT = Joiner.on(' ').useForNull("null");

  protected final Logger logger;
  private final int truncateSize;
  private final @Nullable LogManagerBean mxBean;
  private final String componentName;

  public interface LogManagerMXBean {

    String getConsoleLevel();
    void setConsoleLevel(String newLevel);
  }

  private class LogManagerBean extends AbstractMBean implements LogManagerMXBean {

    private final Level fileLevel;
    private final Handler consoleHandler;

    public LogManagerBean(Handler pConsoleHandler, Level pFileLevel) {
      super("org.sosy_lab.common.log:type=LogManager", BasicLogManager.this);
      consoleHandler = checkNotNull(pConsoleHandler);
      fileLevel = checkNotNull(pFileLevel);
    }

    @Override
    public String getConsoleLevel() {
      return consoleHandler.getLevel().toString();
    }

    @Override
    public void setConsoleLevel(String pNewLevel) throws IllegalArgumentException {
      Level newLevel = Level.parse(pNewLevel.toUpperCase());

      try {
        consoleHandler.setLevel(newLevel);
        logger.setLevel(getMinimumLevel(fileLevel, newLevel));
      } catch (SecurityException e) {
        // on Google App Engine calling setLevel() is forbidden.
      }
    }
  }

  /**
   * @see #BasicLogManager(Configuration, Handler)
   */
  public BasicLogManager(Configuration config) throws InvalidConfigurationException {
    this(config, null, null);
  }

  /**
   * Constructor which allows to customize where the console output of the
   * LogManager is written to. Suggestions for the consoleOutputHandler are
   * StringHandler or OutputStreamHandler.
   *
   * The level, filter and formatter of that handler are set by this class.
   *
   * @param consoleOutputHandler A handler. If null a {@link ConsoleHandler}
   * instance will be used.
   *
   * @see #BasicLogManager(Configuration, Handler, Handler)
   */
  public BasicLogManager(Configuration config, @Nullable Handler consoleOutputHandler)
      throws InvalidConfigurationException {
    this(config, consoleOutputHandler, null);
  }

  /**
   * Constructor which allows to customize where the console output of the
   * LogManager is written to and also allows to customize where the file output
   * of the LogManager is written to.
   *
   * If fileOutputHandler is set to null a handler of type {@link FileHandler}
   * will be used. If the instantiation of this handler fails log messages will be automatically
   * redirected to the consoleOutputHandler.
   *
   * The level, filter and formatter of the handlers are set by this class.
   *
   * @param consoleOutputHandler A handler. If null a {@link ConsoleHandler}
   * instance will be used.
   * @param fileOutputHandler A handler, if null a {@link FileHandler} instance will be used
   */
  public BasicLogManager(Configuration config, @Nullable Handler consoleOutputHandler,
      @Nullable Handler fileOutputHandler) throws InvalidConfigurationException {
    LoggingOptions options = new LoggingOptions(config);
    Level fileLevel = options.getFileLevel();
    Level consoleLevel = options.getConsoleLevel();
    truncateSize = options.getTruncateSize();

    componentName = "";

    logger = Logger.getAnonymousLogger();
    logger.setLevel(getMinimumLevel(fileLevel, consoleLevel));
    logger.setUseParentHandlers(false);

    // create console logger
    if (!consoleLevel.equals(Level.OFF)) {
      if (consoleOutputHandler == null) {
        consoleOutputHandler = new ConsoleHandler();
      }
      setupHandler(consoleOutputHandler, new ConsoleLogFormatter(config),
          consoleLevel, options.getConsoleExclude());
    }

    // create file logger
    if (fileOutputHandler == null) {
      Path outputFile = options.getOutputFile();
      if (!fileLevel.equals(Level.OFF) && outputFile != null) {
        try {
          Files.createParentDirs(outputFile);

          Handler outfileHandler = new FileHandler(outputFile.getAbsolutePath(), false);
          setupHandler(outfileHandler, new FileLogFormatter(), fileLevel, options.getFileExclude());
        } catch (IOException e) {
          // redirect log messages to console
          if (consoleLevel.intValue() > fileLevel.intValue()) {
            logger.getHandlers()[0].setLevel(fileLevel);
          }

          logger.log(Level.WARNING, "Could not open log file " + e.getMessage()
              + ", redirecting log output to console");
        }
      }
    } else {
      setupHandler(fileOutputHandler, new FileLogFormatter(), fileLevel, options.getFileExclude());
    }

    // setup MXBean at the end (this might already log something!)
    if (consoleOutputHandler != null) {
      mxBean = new LogManagerBean(consoleOutputHandler, fileLevel);
      mxBean.register();
    } else {
      mxBean = null;
    }
  }

  /**
   * Sets up the given handler.
   *
   * @param handler The handler to set up.
   * @param formatter The formatter to use with the handler.
   * @param level The level to use with the handler.
   * @param excludeLevels Levels to exclude from the handler via a {@link LogLevelFilter}
   */
  protected void setupHandler(Handler handler, Formatter formatter, Level level,
      List<Level> excludeLevels) {
    //build up list of Levels to exclude from logging
    if (excludeLevels.size() > 0) {
      handler.setFilter(new LogLevelFilter(excludeLevels));
    } else {
      handler.setFilter(null);
    }

    //handler with format for the console logger
    handler.setFormatter(formatter);

    //log only records of priority equal to or greater than the level defined in the configuration
    handler.setLevel(level);

    logger.addHandler(handler);
  }

  private BasicLogManager(BasicLogManager originalLogger, String pComponentName) {
    logger = originalLogger.logger;
    truncateSize = originalLogger.truncateSize;
    componentName = pComponentName;
    mxBean = null;
  }

  @Override
  public LogManager withComponentName(String pName) {
    checkArgument(!pName.isEmpty());

    String name = componentName.isEmpty()
        ? pName
        : componentName + ":" + pName;
    return new BasicLogManager(this, name);
  }

  /**
   * Returns true if a message with the given log level would be logged.
   * @param priority the log level
   * @return whether this log level is enabled
   */
  @Override
  public boolean wouldBeLogged(Level priority) {
    return (logger.isLoggable(priority));
  }

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
  @Override
  public void log(Level priority, Object... args) {
    checkNotNull(args);

    //Since some toString() methods may be rather costly, only log if the level is
    //sufficiently high.
    if (wouldBeLogged(priority))  {

      log0(priority, findCallingMethod(), buildMessageText(args));
    }
  }

  /**
   * Logs any message occurring during program execution.
   * The message is constructed lazily from <code>String.format(format, args)</code>.
   *
   * @param priority the log level for the message
   * @param format The format string.
   * @param args The arguments for the format string.
   */
  @Override
  public void logf(Level priority, String format, Object... args) {
    checkNotNull(format);
    checkNotNull(args);
    if (wouldBeLogged(priority)) {
      log0(priority, findCallingMethod(), String.format(format, args));
    }
  }

  /**
   * Find the first interesting method in the current stack trace.
   * We assume that methods starting with "log" are helper methods for logging
   * and exclude them.
   * Synthetic accessor methods are also excluded.
   */
  private StackTraceElement findCallingMethod() {
    StackTraceElement[] trace = Thread.currentThread().getStackTrace();

    // First method in stacktrace is Thread#getStackTrace(),
    // second is this method, third is the log() method.
    // So we can skip 3 stack trace elements in any case.
    int traceIndex = 3;

    String methodName = trace[traceIndex].getMethodName();
    while (methodName.startsWith("log")
        || methodName.startsWith("access$")) {
      traceIndex++;
      methodName = trace[traceIndex].getMethodName();
    }

    return trace[traceIndex];
  }

  /**
   * Log a message as if it occurred in a given stack trace and with a given
   * priority.
   *
   * For performance reasons, callers should check if
   * <code>wouldBeLogged(priority)</code> returns true before calling this message.
   *
   * @param priority the log level for the message
   * @param stackElement the stack trace frame to use
   * @param msg the message
   */
  private void log0(Level priority, StackTraceElement stackElement, String msg) {

    ExtendedLogRecord record = new ExtendedLogRecord(priority, msg);

    record.setSourceClassName(stackElement.getClassName());
    record.setSourceMethodName(stackElement.getMethodName());
    record.setSourceComponentName(componentName);

    logger.log(record);
  }

  private String buildMessageText(Object... args) {
    String[] argsStr = new String[args.length];
    for (int i = 0; i < args.length; i++) {
      Object o = firstNonNull(args[i], "null");
      String arg;
      if (o instanceof Appender && (truncateSize > 0)) {
        arg = Appenders.toStringWithTruncation((Appender)o, truncateSize + 1);
      } else {
        arg = o.toString();
      }
      arg = firstNonNull(arg, "null"); // may happen if toString() returns null
      if ((truncateSize > 0) && (arg.length() > truncateSize)) {
        String length = (o instanceof Appender) ? ">= " + truncateSize : arg.length() + "";
        argsStr[i] = arg.substring(0, TRUNCATE_REMAINING_SIZE)
                   + "... <REMAINING ARGUMENT OMITTED BECAUSE " + length + " CHARACTERS LONG>";
      } else {
        argsStr[i] = arg;
      }
    }

    String messageText = MESSAGE_FORMAT.join(argsStr);
    return messageText;
  }

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
  @Override
  public void logUserException(Level priority, Throwable e, @Nullable String additionalMessage) {
    if (wouldBeLogged(priority)) {
      String logMessage = "";
      if (priority.equals(Level.SEVERE)) {
        logMessage = "Error: ";
      } else if (priority.equals(Level.WARNING)) {
        logMessage = "Warning: ";
      }

      String exceptionMessage = Strings.nullToEmpty(e.getMessage());

      if (Strings.isNullOrEmpty(additionalMessage)) {

        if (!exceptionMessage.isEmpty()) {
          logMessage += exceptionMessage;
        } else {
          // No message at all, this shoudn't happen as its not nice for the user
          // Create a default message
          logMessage += e.getClass().getSimpleName() + " in " + e.getStackTrace()[0];
        }

      } else {
        logMessage += additionalMessage;

        if (!exceptionMessage.isEmpty()) {
          if ((e instanceof IOException) && logMessage.endsWith("file")
              && exceptionMessage.charAt(exceptionMessage.length() - 1) == ')') {
            // nicer error message, so that we have something like
            // "could not write to file /FOO.txt (Permission denied)"
            logMessage += " " + exceptionMessage;
          } else {
            logMessage += " (" + exceptionMessage + ")";
          }
        }
      }

      // use exception stack trace here so that the location where the exception
      // occurred appears in the message
      StackTraceElement[] trace = e.getStackTrace();
      int traceIndex = 0;

      if (e instanceof InvalidConfigurationException) {
        // find first method outside of the Configuration class,
        // this is probably the most interesting trace element
        String confPackage = Configuration.class.getPackage().getName();
        while (trace[traceIndex].getClassName().startsWith(confPackage)) {
          traceIndex++;
        }
      }

      log0(priority, trace[traceIndex], logMessage);
    }

    logDebugException(e, additionalMessage);
  }

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
  @Override
  public void logDebugException(Throwable e, @Nullable String additionalMessage) {
    logException(EXCEPTION_DEBUG_LEVEL, e, additionalMessage);
  }

  /**
   * Log an exception solely for the purpose of debugging.
   * In default configuration, this exception is not shown to the user!
   *
   * Use this method when you want to log an exception that was handled by the
   * catching site, but you don't want to forget the information completely.
   *
   * @param e the occurred exception
   */
  @Override
  public void logDebugException(Throwable e) {
    logDebugException(e, null);
  }

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
  @Override
  public void logException(Level priority, Throwable e, @Nullable String additionalMessage) {
    checkNotNull(e);
    if (wouldBeLogged(priority)) {
      String logMessage = "";

      if (!Strings.isNullOrEmpty(additionalMessage)) {
        logMessage = additionalMessage + "\n";
      }

      logMessage += Throwables.getStackTraceAsString(e);

      StackTraceElement[] trace = Thread.currentThread().getStackTrace();
      // first method in stacktrace is Thread#getStackTrace(), second is this method
      int traceIndex = 2;

      // find the first interesting method in the stack trace
      // (we assume that methods starting with "log" are helper methods for logging
      while (trace[traceIndex].getMethodName().startsWith("log")) {
        traceIndex++;
      }

      LogRecord record = new LogRecord(priority, logMessage);
      record.setSourceClassName(trace[traceIndex].getClassName());
      record.setSourceMethodName(trace[traceIndex].getMethodName());

      logger.log(record);
    }
  }

  @Override
  public void flush() {
    for (Handler handler : logger.getHandlers()) {
      handler.flush();
    }
  }
  @Override
  public void close() {
    if (mxBean != null) {
      mxBean.unregister();
    }
    for (Handler handler : logger.getHandlers()) {
      handler.close();
    }
  }

  private static Level getMinimumLevel(Level level1, Level level2) {
    if (level1.intValue() > level2.intValue()) {
      return level2; // smaller level is more detailed logging
    } else {
      return level1;
    }
  }
}
