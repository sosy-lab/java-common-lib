// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.errorprone.annotations.Var;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/** Helper methods for concurrency related things. */
public final class Concurrency {

  private Concurrency() {}

  /**
   * Wait uninterruptibly until an ExecutorService has shutdown. It also ensures full memory
   * visibility of everything that was done in the callables.
   *
   * <p>Interrupting the thread will have no effect, but this method will set the thread's
   * interrupted flag in this case.
   */
  public static void waitForTermination(ExecutorService executor) {
    @Var boolean interrupted = Thread.interrupted();

    while (!executor.isTerminated()) {
      try {
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
      } catch (InterruptedException ignored) {
        interrupted = true;
      }
    }

    // now all tasks have terminated

    // restore interrupted flag
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Creates a thread pool of fixed size. Size is determined by processors available to the JVM.
   *
   * @return thread pool
   */
  public static ExecutorService createThreadPool() {
    int processors = Runtime.getRuntime().availableProcessors();
    return Executors.newFixedThreadPool(processors);
  }

  /**
   * Creates a thread pool of fixed size. Size is determined by processors available to the JVM.
   *
   * @param threadFactory The thread factory to be used.
   * @return thread pool
   */
  public static ExecutorService createThreadPool(ThreadFactory threadFactory) {
    int processors = Runtime.getRuntime().availableProcessors();
    return Executors.newFixedThreadPool(processors, threadFactory);
  }

  /**
   * Create a new non-daemon thread with a name. Compared to creating threads manually, this has the
   * advantage that the thread will be created with the default settings. By default, Java lets
   * threads inherit some settings from the creating thread.
   *
   * @param name The name of the new thread.
   * @param r The {@link Runnable} to execute in the new thread.
   * @return A new thread, not yet started.
   */
  public static Thread newThread(String name, Runnable r) {
    checkNotNull(r);
    return new ThreadFactoryBuilder().setNameFormat(name).build().newThread(r);
  }

  /**
   * Create a new daemon thread with a name. Compared to creating threads manually, this has the
   * advantage that the thread will be created with the default settings. By default, Java lets
   * threads inherit some settings from the creating thread.
   *
   * @param name The name of the new thread.
   * @param r The {@link Runnable} to execute in the new thread.
   * @return A new daemon thread, not yet started.
   */
  public static Thread newDaemonThread(String name, Runnable r) {
    checkNotNull(r);
    return new ThreadFactoryBuilder().setNameFormat(name).setDaemon(true).build().newThread(r);
  }
}
