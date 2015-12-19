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

/**
 * {@link LogManager} implementation that does not log anything.
 *
 * Note: Do not use this implementation for unit tests,
 * use {@link TestLogManager} instead.
 */
public enum NullLogManager implements LogManager {
  @Deprecated
  // Use getInstance() instead, enum is just an implementation detail.
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
  public void logf(Level pPriority, String pFormat, Object... pArgs) {}

  @Override
  public void logUserException(Level pPriority, Throwable pE, String pAdditionalMessage) {}

  @Override
  public void logDebugException(Throwable pE, String pAdditionalMessage) {}

  @Override
  public void logDebugException(Throwable pE) {}

  @Override
  public void logException(Level pPriority, Throwable pE, String pAdditionalMessage) {}

  @Override
  public void flush() {}
}
