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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * LogManager implementation intended for testing when nothing should actually be logged.
 *
 * <p>However, it does check all the parameters for validity, i.e. non-nullness and correct string
 * format.
 *
 * @deprecated Use {@link LogManager#createTestLogManager()} instead. This class will be made
 *     package-private.
 */
@Deprecated
public enum TestLogManager implements LogManager {
  INSTANCE;

  public static LogManager getInstance() {
    return INSTANCE;
  }

  @Override
  public LogManager withComponentName(String pName) {
    checkArgument(!pName.isEmpty());
    return this;
  }

  @Override
  public boolean wouldBeLogged(Level pPriority) {
    checkNotNull(pPriority);

    // Return true such that no calls to log() will be omitted
    // (they are important for checking parameter validity).
    return true;
  }

  @Override
  public void log(Level pPriority, Object... pArgs) {
    checkLogBaseParam(pPriority);
    checkObjectArgsConcatenationParams(pArgs);
  }

  @Override
  public void log(Level pPriority, Supplier<String> pMsgSupplier) {
    checkLogBaseParam(pPriority);
    checkNotNull(pMsgSupplier.get());
  }

  @Override
  public void logf(Level pPriority, String pFormat, Object... pArgs) {
    checkLogBaseParam(pPriority);
    checkFormatParamsNotNull(pFormat, pArgs);
  }

  private static void checkLogBaseParam(Level pPriority) {
    checkNotNull(pPriority);
  }

  @Override
  public void logUserException(Level pPriority, Throwable pE, @Nullable String pAdditionalMessage) {
    checkUserExceptionBaseParams(pPriority, pE);
  }

  @Override
  public void logfUserException(Level pPriority, Throwable pE, String pFormat, Object... pArgs) {
    checkUserExceptionBaseParams(pPriority, pE);
    checkFormatParamsNotNull(pFormat, pArgs);
  }

  private static void checkUserExceptionBaseParams(Level pPriority, Throwable pE) {
    checkNotNull(pPriority);
    checkNotNull(pE);
  }

  @Override
  public void logDebugException(Throwable pE, @Nullable String pAdditionalMessage) {
    checkLogDebugExceptionBaseParams(pE);
  }

  @Override
  public void logfDebugException(Throwable pE, String pFormat, Object... pArgs) {
    checkLogDebugExceptionBaseParams(pE);
    checkFormatParamsNotNull(pFormat, pArgs);
  }

  @Override
  public void logDebugException(Throwable pE) {
    checkLogDebugExceptionBaseParams(pE);
  }

  private static void checkLogDebugExceptionBaseParams(Throwable pE) {
    checkNotNull(pE);
  }

  @Override
  public void logException(Level pPriority, Throwable pE, @Nullable String pAdditionalMessage) {
    checkLogExceptionBaseParams(pPriority, pE);
  }

  @Override
  public void logfException(Level pPriority, Throwable pE, String pFormat, Object... pArgs) {
    checkLogExceptionBaseParams(pPriority, pE);
    checkFormatParamsNotNull(pFormat, pArgs);
  }

  private static void checkLogExceptionBaseParams(Level pPriority, Throwable pE) {
    checkNotNull(pPriority);
    checkNotNull(pE);
  }

  private static void checkFormatParamsNotNull(String pFormat, Object... pArgs) {
    checkNotNull(pArgs);
    checkArgument(!String.format(pFormat, pArgs).isEmpty());
  }

  private static void checkObjectArgsConcatenationParams(Object... pArgs) {
    checkNotNull(pArgs);
    checkArgument(pArgs.length != 0);
    // Convert arguments array to string to check that no toString() method throws an exception.
    checkArgument(!Arrays.deepToString(pArgs).isEmpty());
  }

  @Override
  public void flush() {}
}
