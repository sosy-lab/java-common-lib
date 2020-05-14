// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.log;

import com.google.errorprone.annotations.ForOverride;
import com.google.errorprone.annotations.FormatMethod;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class ForwardingLogManager implements LogManager {

  @ForOverride
  protected abstract LogManager delegate();

  @Override
  public boolean wouldBeLogged(Level pPriority) {
    return delegate().wouldBeLogged(pPriority);
  }

  @Override
  public void log(Level pPriority, Object... pArgs) {
    delegate().log(pPriority, pArgs);
  }

  @Override
  public void log(Level pPriority, Supplier<String> pMsgSupplier) {
    delegate().log(pPriority, pMsgSupplier);
  }

  @Override
  @FormatMethod
  public void logf(Level pPriority, String pFormat, Object... pArgs) {
    delegate().logf(pPriority, pFormat, pArgs);
  }

  @Override
  public void logUserException(Level pPriority, Throwable pE, @Nullable String pAdditionalMessage) {
    delegate().logUserException(pPriority, pE, pAdditionalMessage);
  }

  @Override
  @FormatMethod
  public void logfUserException(Level pPriority, Throwable pE, String pFormat, Object... pArgs) {
    delegate().logfUserException(pPriority, pE, pFormat, pArgs);
  }

  @Override
  public void logDebugException(Throwable pE, @Nullable String pAdditionalMessage) {
    delegate().logDebugException(pE, pAdditionalMessage);
  }

  @Override
  @FormatMethod
  public void logfDebugException(Throwable pE, String pFormat, Object... pArgs) {
    delegate().logfDebugException(pE, pFormat, pArgs);
  }

  @Override
  public void logDebugException(Throwable pE) {
    delegate().logDebugException(pE);
  }

  @Override
  public void logException(Level pPriority, Throwable pE, @Nullable String pAdditionalMessage) {
    delegate().logException(pPriority, pE, pAdditionalMessage);
  }

  @Override
  @FormatMethod
  public void logfException(Level pPriority, Throwable pE, String pFormat, Object... pArgs) {
    delegate().logfException(pPriority, pE, pFormat, pArgs);
  }

  @Override
  public void flush() {
    delegate().flush();
  }
}
