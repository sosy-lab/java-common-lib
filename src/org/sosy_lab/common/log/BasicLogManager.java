/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import static com.google.common.base.Objects.firstNonNull;
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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;


/**
 * Default implementation of {@link LogManager}
 */
@Options(prefix = "log",
    description = "Possible log levels in descending order "
    + "\n(lower levels include higher ones):"
    + "\nOFF:      no logs published"
    + "\nSEVERE:   error messages"
    + "\nWARNING:  warnings"
    + "\nINFO:     messages"
    + "\nFINE:     logs on main application level"
    + "\nFINER:    logs on central CPA algorithm level"
    + "\nFINEST:   logs published by specific CPAs"
    + "\nALL:      debugging information"
    + "\nCare must be taken with levels of FINER or lower, as output files may "
    + "become quite large and memory usage might become an issue.")
public class BasicLogManager implements org.sosy_lab.common.LogManager {

  @Option(name="level", toUppercase=true, description="log level of file output")
  private Level fileLevel = Level.OFF;

  @Option(toUppercase=true, description="log level of console output")
  private Level consoleLevel = Level.INFO;

  @Option(toUppercase=true, description="single levels to be excluded from being logged")
  private List<Level> fileExclude = ImmutableList.of();

  @Option(toUppercase=true, description="single levels to be excluded from being logged")
  private List<Level> consoleExclude = ImmutableList.of();

  @Option(name="file",
      description="name of the log file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path outputFile = Paths.get("CPALog.txt");

  @Option(description="maximum size of log output strings before they will be truncated")
  private int truncateSize = 10000;

  // Number of characters taken from the start of the original output strings when truncating
  private final int truncateRemainingSize = 100;

  private static final Level exceptionDebugLevel = Level.ALL;
  private static final Joiner messageFormat = Joiner.on(' ').useForNull("null");
  private final Logger logger;
  private final LogManagerBean mxBean;

  public static interface LogManagerMXBean {

    String getConsoleLevel();
    void setConsoleLevel(String newLevel);
  }

  private class LogManagerBean extends AbstractMBean implements LogManagerMXBean {

    private final Handler consoleHandler;

    public LogManagerBean(Handler pConsoleHandler) {
      super("org.sosy_lab.common.log:type=LogManager", BasicLogManager.this);
      consoleHandler = pConsoleHandler;
    }

    @Override
    public String getConsoleLevel() {
      return consoleHandler.getLevel().toString();
    }

    @Override
    public void setConsoleLevel(String pNewLevel) throws IllegalArgumentException {
      Level newLevel = Level.parse(pNewLevel.toUpperCase());

      consoleHandler.setLevel(newLevel);
      logger.setLevel(getMinimumLevel(fileLevel, newLevel));
    }
  }

  public BasicLogManager(Configuration config) throws InvalidConfigurationException {
    this(config, new ConsoleHandler());
  }

  /**
   * Constructor which allows to customize where the console output of the
   * LogManager is written to. Suggestions for the consoleOutputHandler are
   * StringHandler or OutputStreamHandler.
   *
   * The level, filter and formatter of that handler are set by this class.
   *
   * @param consoleOutputHandler A handler, may not be null.
   */
  public BasicLogManager(Configuration config, Handler consoleOutputHandler) throws InvalidConfigurationException {
    Preconditions.checkNotNull(consoleOutputHandler);
    config.inject(this);

    logger = Logger.getAnonymousLogger();
    logger.setLevel(getMinimumLevel(fileLevel, consoleLevel));
    logger.setUseParentHandlers(false);

    // create console logger
    setupHandler(consoleOutputHandler, new ConsoleLogFormatter(config), consoleLevel, consoleExclude);

    // create file logger
    if (!fileLevel.equals(Level.OFF) && outputFile != null) {
      try {
        org.sosy_lab.common.io.Files.createParentDirs(outputFile);

        Handler outfileHandler = new FileHandler(outputFile.getAbsolutePath(), false);

        setupHandler(outfileHandler, new FileLogFormatter(), fileLevel, fileExclude);

      } catch (IOException e) {
        // redirect log messages to console
        if (consoleLevel.intValue() > fileLevel.intValue()) {
          logger.getHandlers()[0].setLevel(fileLevel);
        }

        logger.log(Level.WARNING, "Could not open log file " + e.getMessage() + ", redirecting log output to console");
      }
    }

    // setup MXBean at the end (this might already log something!)
    mxBean = new LogManagerBean(consoleOutputHandler);
    mxBean.register();
  }

  private void setupHandler(Handler handler, Formatter formatter, Level level, List<Level> excludeLevels) {
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
   * @param args the parts of the message (can be an arbitrary number of objects whose {@link Object#toString()} method is called)
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
   * @param trace the stack trace frame to use
   * @param msg the message
   */
  private void log0(Level priority, StackTraceElement stackElement, String msg) {

    LogRecord record = new LogRecord(priority, msg);

    record.setSourceClassName(stackElement.getClassName());
    record.setSourceMethodName(stackElement.getMethodName());

    logger.log(record);
  }

  private String buildMessageText(Object... args) {
    String[] argsStr = new String[args.length];
    for (int i = 0; i < args.length; i++) {
      String arg = firstNonNull(args[i], "null").toString();
      arg = firstNonNull(arg, "null"); // may happen if toString() returns null
      if ((truncateSize > 0) && (arg.length() > truncateSize)) {
        argsStr[i] = arg.substring(0, truncateRemainingSize)
                   + "... <REMAINING ARGUMENT OMITTED BECAUSE " + arg.length() + " CHARACTERS LONG>";
      } else {
        argsStr[i] = arg;
      }
    }

    String messageText = messageFormat.join(argsStr);
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
          if ((e instanceof IOException) && logMessage.endsWith("file")) {
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
    logException(exceptionDebugLevel, e, additionalMessage);
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
      int traceIndex = 2; // first method in stacktrace is Thread#getStackTrace(), second is this method

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
    mxBean.unregister();
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
