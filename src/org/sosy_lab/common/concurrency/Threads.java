/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.common.concurrency;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.Nullable;


public class Threads {

  private static ThreadFactory factory;

  /**
   * Prevent instantiation.
   */
  private Threads() {}

  /**
   * Returns a new Thread instance created by the underlying ThreadFactory.
   *
   * @param r The Runnable to be executed by the Thread
   * @return A new Thread instance.
   */
  public static Thread newThread(Runnable r) {
    checkNotNull(r);
    return Threads.threadFactory().newThread(r);
  }

  /**
   * Returns a new Thread instance created by the underlying ThreadFactory.
   *
   * @param r The Runnable to be executed by the Thread
   * @param name The name of the new thread
   * @return A new Thread instance.
   */
  public static Thread newThread(Runnable r, String name) {
    checkNotNull(r);
    checkNotNull(name);
    Thread thread = Threads.threadFactory().newThread(r);
//    thread.setName(name);
    return thread;
  }

  /**
   * Returns the ThreadFactory responsible for creating Thread instances.
   * If the factory has not been set before via {@link #setThreadFactory(ThreadFactory)}
   * or it has been set to null the factory {@link Executors#defaultThreadFactory()} will be used.
   *
   * @return The thread factory.
   */
  public static ThreadFactory threadFactory() {
    if (factory == null) {
      return Executors.defaultThreadFactory();
    }

    return factory;
  }

  /**
   * Sets the thread factory to be used to create Thread instances.
   *
   * @param threadFactory The thread factory to be used.
   */
  public static void setThreadFactory(@Nullable ThreadFactory threadFactory) {
    factory = threadFactory;
  }
}
