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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * LogManager implementation which helps to get log messages printed only once,
 * and avoid duplicate messages.
 */
public class LogManagerWithoutDuplicates extends ForwardingLogManager
                                         implements LogManager {

  private final LogManager delegate;

  private final Set<ImmutableList<Object>> seenMessages = new HashSet<>();

  public LogManagerWithoutDuplicates(LogManager pDelegate) {
    delegate = checkNotNull(pDelegate);
  }

  @Override
  protected LogManager delegate() {
    return delegate;
  }

  /**
   * @see LogManager#withComponentName(String)
   *
   * This method returns a new LogManagerWithoutDuplicates,
   * which does not share state with the current instance
   * (i.e., it is possible to log the same message both through the old
   * and the new instance once).
   */
  @Override
  public LogManagerWithoutDuplicates withComponentName(String pName) {
    return new LogManagerWithoutDuplicates(delegate.withComponentName(pName));
  }

  /**
   * Logging method similar to {@link #log(Level, Object...)},
   * however, subsequent calls to this method with the same arguments
   * will be silently ignored.
   * Direct calls to {@link #log(Level, Object...)} are not affected.
   *
   * Make sure to call this method only with immutable parameters,
   * such as Strings!
   * If objects are changed after being passed to this method,
   * detecting duplicate log messages may not work,
   * or too many log messages may be ignored.
   */
  public void logOnce(Level pPriority, Object... pArgs) {
    checkNotNull(pArgs);
    if (wouldBeLogged(pPriority)) {

      if (seenMessages.add(ImmutableList.copyOf(pArgs))) {
        // log only if not already seen
        log(pPriority, pArgs);
      }
    }
  }

  /**
   * Logging method similar to {@link #logf(Level, String, Object...)},
   * however, subsequent calls to this method with the same arguments
   * will be silently ignored.
   * Direct calls to {@link #logf(Level, String, Object...)} are not affected.
   *
   * Make sure to call this method only with immutable parameters,
   * such as Strings!
   * If objects are changed after being passed to this method,
   * detecting duplicate log messages may not work,
   * or too many log messages may be ignored.
   */
  public void logfOnce(Level pPriority, String pFormat, Object... pArgs) {
    checkNotNull(pFormat);
    checkNotNull(pArgs);
    if (wouldBeLogged(pPriority)) {

      ImmutableList.Builder<Object> args = ImmutableList.builder();
      args.add(pFormat);
      args.add(pArgs);

      if (seenMessages.add(args.build())) {
        // log only if not already seen
        logf(pPriority, pFormat, pArgs);
      }
    }
  }

  /**
   * Reset all seen log messages, such that {@link #logfOnce(Level, String, Object...)}
   * and {@link #logfOnce(Level, String, Object...)} will be guaranteed
   * to behave exactly like in a fresh instance of this class.
   */
  public void resetSeenMessages() {
    seenMessages.clear();
  }
}
