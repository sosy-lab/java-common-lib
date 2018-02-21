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

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.google.common.io.MoreFiles;
import com.google.errorprone.annotations.Var;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.sosy_lab.common.AbstractMBean;
import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

/** Default implementation of {@link LogManager}. */
public class BasicLogManager implements LogManager, AutoCloseable {

  // Number of characters taken from the start of the original output strings when truncating
  @VisibleForTesting static final int TRUNCATE_REMAINING_SIZE = 100;

  private static final Level EXCEPTION_DEBUG_LEVEL = Level.ALL;
  private static final Joiner MESSAGE_FORMAT = Joiner.on(' ').useForNull("null");

  private final Logger logger;
  private final int truncateSize;
  private @Nullable LogManagerBean mxBean = null;
  private final String componentName;

  public interface LogManagerMXBean {

    String getConsoleLevel();

    void setConsoleLevel(String newLevel);
  }

  private class LogManagerBean extends AbstractMBean implements LogManagerMXBean {

    private final Level fileLevel;
    private final Handler consoleHandler;

    private LogManagerBean(Handler pConsoleHandler, Level pFileLevel) {
      super("org.sosy_lab.common.log:type=LogManager", BasicLogManager.this);
      consoleHandler = checkNotNull(pConsoleHandler);
      fileLevel = checkNotNull(pFileLevel);
    }

    @Override
    public String getConsoleLevel() {
      return consoleHandler.getLevel().toString();
    }

    @Override
    public void setConsoleLevel(String pNewLevel) {
      Level newLevel = Level.parse(pNewLevel.toUpperCase(Locale.US));
      consoleHandler.setLevel(newLevel);
      logger.setLevel(getMinimumLevel(fileLevel, newLevel));
    }
  }

  private static class LimitingStringBuilderAppendable implements Appendable {

    private final int truncateSize;

    private final StringBuilder sb = new StringBuilder();

    LimitingStringBuilderAppendable(int pTruncateSize) {
      truncateSize = pTruncateSize;
    }

    @Override
    public Appendable append(CharSequence pCsq, int pStart, int pEnd) throws IOException {
      int length = pEnd - pStart;
      if (length - truncateSize > 0) {
        sb.append(pCsq, pStart, pStart + TRUNCATE_REMAINING_SIZE);
        appendTruncationMessage(sb, Integer.toString(length));
      } else {
        sb.append(pCsq, pStart, pEnd);
      }
      return this;
    }

    @Override
    public Appendable append(char pC) throws IOException {
      sb.append(pC);
      return this;
    }

    @Override
    public Appendable append(CharSequence pCsq) throws IOException {
      int length = pCsq.length();
      if (length > truncateSize) {
        sb.append(pCsq, 0, TRUNCATE_REMAINING_SIZE);
        appendTruncationMessage(sb, Integer.toString(length));
      } else {
        sb.append(pCsq);
      }
      return this;
    }

    @Override
    public String toString() {
      return sb.toString();
    }
  }

  private static void appendTruncationMessage(StringBuilder sb, String len) {
    sb.append("... <REMAINING ARGUMENT OMITTED BECAUSE ").append(len).append(" CHARACTERS LONG>");
  }

  /**
   * Constructor which allows to customize where this logger delegates to.
   *
   * <p>The feature of truncating long log messages is disabled.
   *
   * @param pLogger The Java logger where this logger delegates to.
   */
  public BasicLogManager(Logger pLogger) {
    this(pLogger, 0);
  }

  /**
   * Constructor which allows to customize where this logger delegates to.
   *
   * @param pLogger The Java logger where this logger delegates to.
   * @param pTruncateSize A positive integer threshold for truncating long log messages, or 0 to
   *     disable truncation.
   */
  public BasicLogManager(Logger pLogger, int pTruncateSize) {
    logger = checkNotNull(pLogger);
    componentName = "";
    if (pTruncateSize >= TRUNCATE_REMAINING_SIZE) {
      truncateSize = pTruncateSize;
    } else if (pTruncateSize > 0) {
      // truncate to something smaller than TRUNCATE_REMAINING_SIZE would have no effect
      truncateSize = TRUNCATE_REMAINING_SIZE;
    } else if (pTruncateSize == 0) {
      truncateSize = 0;
    } else {
      throw new IllegalArgumentException("Negative truncateSize not allowed.");
    }
  }

  /**
   * Create a {@link BasicLogManager} which delegates to a new logger with only the given {@link
   * Handler}.
   *
   * @param handler The target handler.
   */
  public static LogManager createWithHandler(Handler handler) {
    return createWithHandler(handler, 0);
  }

  @VisibleForTesting
  static LogManager createWithHandler(Handler handler, int truncateSize) {
    Logger logger = Logger.getAnonymousLogger();
    logger.setLevel(handler.getLevel());
    logger.setUseParentHandlers(false);
    logger.addHandler(handler);
    return new BasicLogManager(logger, truncateSize);
  }

  /**
   * Create a {@link BasicLogManager} which logs to a file and the console according to user
   * configuration.
   *
   * <p>This also adds an MXBean that allows runtime control of some logging options.
   */
  public static LogManager create(Configuration config) throws InvalidConfigurationException {
    return create(new LoggingOptions(config));
  }

  /**
   * Create a {@link BasicLogManager} which logs to a file and the console according to specified
   * options.
   *
   * <p>This also adds an MXBean that allows runtime control of some logging options.
   *
   * <p>Most users will want to use {@link #create(Configuration)} instead.
   */
  public static LogManager create(LoggingOptions options) {
    Level fileLevel = options.getFileLevel();
    Level consoleLevel = options.getConsoleLevel();

    Logger logger = Logger.getAnonymousLogger();
    logger.setLevel(BasicLogManager.getMinimumLevel(fileLevel, consoleLevel));
    logger.setUseParentHandlers(false);

    // create console logger
    Handler consoleOutputHandler = new ConsoleHandler();
    setupHandler(
        logger,
        consoleOutputHandler,
        new ConsoleLogFormatter(options),
        consoleLevel,
        options.getConsoleExclude());

    // create file logger
    Path outputFile = options.getOutputFile();
    if (!fileLevel.equals(Level.OFF) && outputFile != null) {
      try {
        MoreFiles.createParentDirectories(outputFile);

        Handler outfileHandler = new FileHandler(outputFile.toAbsolutePath().toString(), false);
        setupHandler(
            logger, outfileHandler, new FileLogFormatter(), fileLevel, options.getFileExclude());
      } catch (IOException e) {
        // redirect log messages to console
        if (consoleLevel.intValue() > fileLevel.intValue()) {
          consoleOutputHandler.setLevel(fileLevel);
        }

        logger.log(
            Level.WARNING,
            "Could not open log file (" + e.getMessage() + "), redirecting log output to console");
      }
    }

    BasicLogManager logManager = new BasicLogManager(logger, options.getTruncateSize());

    logManager.addMxBean(consoleOutputHandler, fileLevel);

    return logManager;
  }

  /**
   * Sets up the given handler.
   *
   * @param handler The handler to set up.
   * @param formatter The formatter to use with the handler.
   * @param level The level to use with the handler.
   * @param excludeLevels Levels to exclude from the handler via a {@link LogLevelFilter}
   */
  private static void setupHandler(
      Logger logger, Handler handler, Formatter formatter, Level level, List<Level> excludeLevels) {
    // build up list of Levels to exclude from logging
    if (!excludeLevels.isEmpty()) {
      handler.setFilter(new LogLevelFilter(excludeLevels));
    } else {
      handler.setFilter(null);
    }

    // handler with format for the console logger
    handler.setFormatter(formatter);

    // log only records of priority equal to or greater than the level defined in the configuration
    handler.setLevel(level);

    logger.addHandler(handler);
  }

  private void addMxBean(Handler pConsoleHandler, Level pFileLevel) {
    checkState(mxBean == null);
    mxBean = new LogManagerBean(pConsoleHandler, pFileLevel);
    mxBean.register();
  }

  private BasicLogManager(BasicLogManager originalLogger, String pComponentName) {
    logger = originalLogger.logger;
    truncateSize = originalLogger.truncateSize;
    componentName = pComponentName;
  }

  @Override
  public LogManager withComponentName(String pName) {
    checkArgument(!pName.isEmpty());

    String name = componentName.isEmpty() ? pName : componentName + ":" + pName;
    return new BasicLogManager(this, name);
  }

  /**
   * Returns true if a message with the given log level would be logged.
   *
   * @param priority the log level
   * @return whether this log level is enabled
   */
  @Override
  public boolean wouldBeLogged(Level priority) {
    return (logger.isLoggable(priority));
  }

  /**
   * Logs any message occurring during program execution. The message is constructed lazily by
   * concatenating the parts with " ". The caller should not use string concatenation to create the
   * message in order to increase performance if the message is never logged.
   *
   * @param priority the log level for the message
   * @param args the parts of the message (can be an arbitrary number of objects whose {@link
   *     Object#toString()} method is called)
   */
  @Override
  public void log(Level priority, Object... args) {
    checkBuildAdditionalMessageParams(args);
    // Since some toString() methods may be rather costly, only log if the level is
    // sufficiently high.
    if (wouldBeLogged(priority)) {

      log0(priority, findCallingMethod(), buildAdditionalMessageText(args));
    }
  }

  @Override
  public void log(Level priority, Supplier<String> msgSupplier) {
    checkNotNull(msgSupplier);

    if (wouldBeLogged(priority)) {
      log0(priority, findCallingMethod(), msgSupplier.get());
    }
  }

  /**
   * Logs any message occurring during program execution. The message is constructed lazily from
   * <code>String.format(format, args)</code>.
   *
   * @param priority the log level for the message
   * @param format The format string.
   * @param args The arguments for the format string.
   */
  @Override
  public void logf(Level priority, String format, Object... args) {
    checkFormatStringParameters(format, args);
    if (wouldBeLogged(priority)) {
      log0(priority, findCallingMethod(), formatAdditionalMessage(format, args));
    }
  }

  /**
   * Builds the additionalMessage using the format string parameters and the defined truncate size.
   *
   * @param format The format string.
   * @param args The arguments for the format string.
   * @return Additional message as string
   */
  private String formatAdditionalMessage(String format, Object... args) {
    checkFormatStringParameters(format, args);

    @SuppressWarnings("resource") // Nothing to close for StringBuilder
    java.util.Formatter formatter =
        new java.util.Formatter(
            truncateSize > 0
                ? new LimitingStringBuilderAppendable(truncateSize)
                : new StringBuilder());
    return formatter.format(format, args).toString();
  }

  private static void checkFormatStringParameters(String format, Object... args) {
    checkNotNull(format);
    checkNotNull(args);
  }

  /**
   * Builds the additionalMessage by concatenating the arguments with " ".
   *
   * @param args The arguments to get concatenated
   * @return The additonalMessage string, "" if <code>args.length == 0</code>
   */
  private String buildAdditionalMessageText(Object... args) {
    checkBuildAdditionalMessageParams(args);

    String[] argsStr = new String[args.length];
    for (int i = 0; i < args.length; i++) {
      Object o = firstNonNull(args[i], "null");
      @Var String arg;
      if (o instanceof Appender && (truncateSize > 0)) {
        arg = Appenders.toStringWithTruncation((Appender) o, truncateSize + 1);
      } else {
        arg = o.toString();
      }
      arg = firstNonNull(arg, "null"); // may happen if toString() returns null
      if ((truncateSize > 0) && (arg.length() > truncateSize)) {
        String length =
            (o instanceof Appender) ? ">= " + truncateSize : Integer.toString(arg.length());
        StringBuilder sb = new StringBuilder(TRUNCATE_REMAINING_SIZE + 70);
        sb.append(arg.substring(0, TRUNCATE_REMAINING_SIZE));
        appendTruncationMessage(sb, length);
        argsStr[i] = sb.toString();
      } else {
        argsStr[i] = arg;
      }
    }

    String messageText = MESSAGE_FORMAT.join(argsStr);
    return messageText;
  }

  private static void checkBuildAdditionalMessageParams(Object... args) {
    checkNotNull(args);
  }

  /**
   * Find the first interesting method in the current stack trace. We assume that methods starting
   * with "log" are helper methods for logging and exclude them. Synthetic accessor methods are also
   * excluded.
   */
  private static StackTraceElement findCallingMethod() {
    // We use lazy stack trace, because this exactly fits our use case:
    // Typically we need only one or two StackTraceElements from the top of the trace.
    List<StackTraceElement> trace = Throwables.lazyStackTrace(new Throwable());

    // First method in stacktrace is this method, second is the log() method.
    // So we can skip 2 stack trace elements in any case.
    @Var int traceIndex = 2;

    @Var StackTraceElement frame = trace.get(traceIndex);
    while (frame.getMethodName().startsWith("log") || frame.getMethodName().startsWith("access$")) {
      traceIndex++;
      frame = trace.get(traceIndex);
    }

    return frame;
  }

  /**
   * Log a message as if it occurred in a given stack trace and with a given priority.
   *
   * <p>For performance reasons, callers should check if <code>wouldBeLogged(priority)</code>
   * returns true before calling this message.
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

  /** {@inheritDoc} */
  @Override
  public void logUserException(Level priority, Throwable e, @Nullable String pAdditionalMessage) {
    checkNotNull(e);
    Supplier<String> additinalMessageSupplier = () -> Strings.nullToEmpty(pAdditionalMessage);
    log0UserException(priority, e, additinalMessageSupplier);
  }

  /** {@inheritDoc} */
  @Override
  public void logfUserException(Level priority, Throwable e, String format, Object... args) {
    checkNotNull(e);
    checkFormatStringParameters(format, args);
    Supplier<String> additionalMessageSupplier = () -> formatAdditionalMessage(format, args);
    log0UserException(priority, e, additionalMessageSupplier);
  }

  private void log0UserException(
      Level priority, Throwable e, Supplier<String> additionalMessageSupplier) {

    if (wouldBeLogged(priority) || wouldBeLogged(EXCEPTION_DEBUG_LEVEL)) {
      // build additionalMessage only if actually logged
      String additionalMessage = additionalMessageSupplier.get();
      if (wouldBeLogged(priority)) {
        StringBuilder logMessage = buildUserExceptionLogMessage(priority, e, additionalMessage);
        @Var StackTraceElement frame = locateStackTraceElement(e);
        log0(priority, frame, logMessage.toString());
      }
      logDebugException(e, additionalMessage);
    }
  }

  private static StringBuilder buildUserExceptionLogMessage(
      Level priority, Throwable e, String additionalMessage) {

    StringBuilder logMessage = new StringBuilder();
    if (priority.equals(Level.SEVERE)) {
      logMessage.append("Error: ");
    } else if (priority.equals(Level.WARNING)) {
      logMessage.append("Warning: ");
    }

    String exceptionMessage = Strings.nullToEmpty(e.getMessage());

    if (Strings.isNullOrEmpty(additionalMessage)) {

      if (!exceptionMessage.isEmpty()) {
        logMessage.append(exceptionMessage);
      } else {
        // No message at all, this shoudn't happen as its not nice for the user
        // Create a default message
        logMessage.append(e.getClass().getSimpleName()).append(" in ").append(e.getStackTrace()[0]);
      }

    } else {
      logMessage.append(additionalMessage);

      if (!exceptionMessage.isEmpty()) {
        if ((e instanceof IOException)
            && additionalMessage.endsWith("file")
            && exceptionMessage.charAt(exceptionMessage.length() - 1) == ')') {
          // nicer error message, so that we have something like
          // "could not write to file /FOO.txt (Permission denied)"
          logMessage.append(' ').append(exceptionMessage);
        } else {
          logMessage.append(" (").append(exceptionMessage).append(')');
        }
      }
    }
    return logMessage;
  }

  private static StackTraceElement locateStackTraceElement(Throwable e) {
    // use exception stack trace here so that the location where the exception
    // occurred appears in the message
    List<StackTraceElement> trace = Throwables.lazyStackTrace(e);
    @Var StackTraceElement frame = trace.get(0);

    if (e instanceof InvalidConfigurationException) {
      // find first method outside of the Configuration class,
      // this is probably the most interesting trace element
      String confPackage = Configuration.class.getPackage().getName();
      @Var int traceIndex = 0;
      while (frame.getClassName().startsWith(confPackage)) {
        traceIndex++;
        frame = trace.get(traceIndex);
      }
    }
    return frame;
  }

  /** {@inheritDoc} */
  @Override
  public void logDebugException(Throwable pE, @Nullable String pAdditionalMessage) {
    logException(EXCEPTION_DEBUG_LEVEL, pE, pAdditionalMessage);
  }

  /** {@inheritDoc} */
  @Override
  public void logDebugException(Throwable e) {
    logException(EXCEPTION_DEBUG_LEVEL, e, null);
  }

  /** {@inheritDoc} */
  @Override
  public void logfDebugException(Throwable e, String format, Object... args) {
    logfException(EXCEPTION_DEBUG_LEVEL, e, format, args);
  }

  @Override
  public void logException(Level pPriority, Throwable pE, @Nullable String pAdditionalMessage) {
    checkNotNull(pE);
    Supplier<String> additionalMessageSupplier = () -> Strings.emptyToNull(pAdditionalMessage);
    log0Exception(pPriority, pE, additionalMessageSupplier);
  }

  /** {@inheritDoc} */
  @Override
  public void logfException(Level priority, Throwable e, String format, Object... args) {
    checkNotNull(e);
    checkFormatStringParameters(format, args);
    Supplier<String> additionalMessageSupplier = () -> formatAdditionalMessage(format, args);
    log0Exception(priority, e, additionalMessageSupplier);
  }

  private void log0Exception(
      Level priority, Throwable e, Supplier<String> additionalMessageSupplier) {
    if (wouldBeLogged(priority)) {
      String additionalMessage = additionalMessageSupplier.get();
      StringBuilder logMessage = buildExceptionLogMessage(e, additionalMessage);
      log0(priority, findCallingMethod(), logMessage.toString());
    }
  }

  private static StringBuilder buildExceptionLogMessage(Throwable e, String additionalMessage) {
    StringBuilder logMessage = new StringBuilder();

    if (!Strings.isNullOrEmpty(additionalMessage)) {
      logMessage.append(additionalMessage).append('\n');
    }

    logMessage
        .append("Exception in thread \"")
        .append(Thread.currentThread().getName())
        .append("\" ");
    e.printStackTrace(new PrintWriter(CharStreams.asWriter(logMessage)));
    return logMessage;
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
