/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
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
 */
package org.sosy_lab.common.concurrency;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.Nullable;

import com.google.common.util.concurrent.ThreadFactoryBuilder;


public class Threads {

  private static @Nullable ThreadFactory factory;

  /**
   * Prevent instantiation.
   */
  private Threads() {}

  /**
   * @see Threads#newThread(Runnable, String, Boolean, Integer)
   */
  public static Thread newThread(Runnable r) {
    return newThread(r, null, null, null);
  }

  /**
   * @see Threads#newThread(Runnable, String, Boolean, Integer)
   */
  public static Thread newThread(Runnable r, String name) {
    return newThread(r, checkNotNull(name), null, null);
  }

  /**
   * @see Threads#newThread(Runnable, String, Boolean, Integer)
   */
  public static Thread newThread(Runnable r, String name, boolean daemon) {
    return newThread(r, checkNotNull(name), daemon, null);
  }

  /**
   * Returns a new Thread instance created by the underlying ThreadFactory.
   *
   * @param r The Runnable to be executed by the Thread
   * @param name The name of the new thread
   * @param daemon True, if the new thread should be a daemon, false otherwise
   * @param priority The priority of the new thread.
   * @return A new Thread instance.
   */
  public static Thread newThread(Runnable r, @Nullable String name, @Nullable Boolean daemon, @Nullable Integer priority) {
    checkNotNull(r);
    CatchSecurityViolationThreadFactoryBuilder builder = threadFactoryBuilder();

    if (name != null) {
      builder.setNameFormat(name);
    }
    if (daemon != null) {
      builder.setDaemon(daemon);
    }
    if (priority != null) {
      builder.setPriority(priority);
    }

    return builder
        .build()
        .newThread(r);
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

  /**
   * Return a {@link CatchSecurityViolationThreadFactoryBuilder} that already has the correct
   * backing {@link ThreadFactory} set.
   * @see ThreadFactoryBuilder
   * @return A fresh {@link CatchSecurityViolationThreadFactoryBuilder} instance.
   */
  public static CatchSecurityViolationThreadFactoryBuilder threadFactoryBuilder() {
    return new CatchSecurityViolationThreadFactoryBuilder().setThreadFactory(threadFactory());
  }
}
