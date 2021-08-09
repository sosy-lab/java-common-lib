// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.log;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.FormatMethod;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * LogManager implementation which helps to get log messages printed only once, and avoid duplicate
 * messages.
 */
public class LogManagerWithoutDuplicates extends ForwardingLogManager implements LogManager {

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
   * This method returns a new LogManagerWithoutDuplicates, which does not share state with the
   * current instance (i.e., it is possible to log the same message both through the old and the new
   * instance once).
   *
   * @see LogManager#withComponentName(String)
   */
  @Override
  public LogManagerWithoutDuplicates withComponentName(String pName) {
    return new LogManagerWithoutDuplicates(delegate.withComponentName(pName));
  }

  /**
   * Logging method similar to {@link #log(Level, Object...)}, however, subsequent calls to this
   * method with the same arguments will be silently ignored. Direct calls to {@link #log(Level,
   * Object...)} are not affected.
   *
   * <p>Make sure to call this method only with immutable parameters, such as Strings! If objects
   * are changed after being passed to this method, detecting duplicate log messages may not work,
   * or too many log messages may be ignored.
   */
  public void logOnce(Level pPriority, Object... pArgs) {
    checkNotNull(pArgs);
    if (wouldBeLogged(pPriority) && seenMessages.add(ImmutableList.copyOf(pArgs))) {
      // log only if not already seen
      log(pPriority, pArgs);
    }
  }

  /**
   * Logging method similar to {@link #log(Level, Supplier)}, however, subsequent calls to this
   * method with the same arguments will be silently ignored. Direct calls to {@link #log(Level,
   * Supplier)} are not affected.
   *
   * <p>Make sure to call this method only with immutable parameters, such as Strings! If objects
   * are changed after being passed to this method, detecting duplicate log messages may not work,
   * or too many log messages may be ignored.
   */
  public void logOnce(Level pPriority, Supplier<String> pMsgSupplier) {
    checkNotNull(pMsgSupplier);
    if (wouldBeLogged(pPriority)) {

      String msg = pMsgSupplier.get();
      if (seenMessages.add(ImmutableList.of(msg))) {
        // log only if not already seen
        log(pPriority, msg);
      }
    }
  }

  /**
   * Logging method similar to {@link #logf(Level, String, Object...)}, however, subsequent calls to
   * this method with the same arguments will be silently ignored. Direct calls to {@link
   * #logf(Level, String, Object...)} are not affected.
   *
   * <p>Make sure to call this method only with immutable parameters, such as Strings! If objects
   * are changed after being passed to this method, detecting duplicate log messages may not work,
   * or too many log messages may be ignored.
   */
  @FormatMethod
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
   * Reset all seen log messages, such that {@link #logfOnce(Level, String, Object...)} and {@link
   * #logfOnce(Level, String, Object...)} will be guaranteed to behave exactly like in a fresh
   * instance of this class.
   */
  public void resetSeenMessages() {
    seenMessages.clear();
  }
}
