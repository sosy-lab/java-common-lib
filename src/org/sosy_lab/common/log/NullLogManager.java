// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.log;

import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * {@link LogManager} implementation that does not log anything.
 *
 * <p>Note: Do not use this implementation for unit tests, use {@link TestLogManager} instead.
 *
 * @deprecated Use {@link LogManager#createNullLogManager()} instead. This class will be made
 *     package-private.
 */
@Deprecated
public enum NullLogManager implements LogManager {
  INSTANCE;

  public static LogManager getInstance() {
    return INSTANCE;
  }

  @Override
  public LogManager withComponentName(String pName) {
    return this;
  }

  @Override
  public boolean wouldBeLogged(Level pPriority) {
    return false;
  }

  @Override
  public void log(Level pPriority, Object... pArgs) {}

  @Override
  public void log(Level pPriority, Supplier<String> pMsgSupplier) {}

  @Override
  public void logf(Level pPriority, String pFormat, Object... pArgs) {}

  @Override
  public void logUserException(Level pPriority, Throwable pE, String pAdditionalMessage) {}

  @Override
  public void logfUserException(Level pPriority, Throwable pE, String pFormat, Object... pArgs) {}

  @Override
  public void logDebugException(Throwable pE, String pAdditionalMessage) {}

  @Override
  public void logfDebugException(Throwable pE, String pFormat, Object... pArgs) {}

  @Override
  public void logDebugException(Throwable pE) {}

  @Override
  public void logException(Level pPriority, Throwable pE, String pAdditionalMessage) {}

  @Override
  public void logfException(Level pPriority, Throwable pE, String pFormat, Object... pArgs) {}

  @Override
  public void flush() {}
}
