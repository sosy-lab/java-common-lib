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
package org.sosy_lab.common.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Helper methods for concurrency related things.
 */
public class Concurrency {

  private Concurrency() {}

  /**
   * Wait uninterruptibly until an ExecutorService has shutdown.
   * It also ensures full memory visibility of everything that was done in
   * the callables.
   *
   * Interrupting the thread will have no effect, but this method
   * will set the thread's interrupted flag in this case.
   * @deprecated use {@link org.sosy_lab.common.Concurrency#waitForTermination(ExecutorService)}
   */
  @Deprecated
  public static void waitForTermination(ExecutorService executor) {
    org.sosy_lab.common.Concurrency.waitForTermination(executor);
  }

  /**
   * Creates a thread pool of fixed size. Size is determined by processors
   * available to the JVM.
   *
   * @return thread pool
   * @deprecated use {@link org.sosy_lab.common.Concurrency#createThreadPool()}
   */
  @Deprecated
  public static ExecutorService createThreadPool() {
    return org.sosy_lab.common.Concurrency.createThreadPool();
  }

  /**
   * Creates a thread pool of fixed size. Size is determined by processors
   * available to the JVM.
   *
   * @param threadFactory
   *            The thread factory to be used.
   * @return thread pool
   * @deprecated use {@link org.sosy_lab.common.Concurrency#createThreadPool(ThreadFactory)}
   */
  @Deprecated
  public static ExecutorService createThreadPool(ThreadFactory threadFactory) {
    return org.sosy_lab.common.Concurrency.createThreadPool(threadFactory);
  }
}
