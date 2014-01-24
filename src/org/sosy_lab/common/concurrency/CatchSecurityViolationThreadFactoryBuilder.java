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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * This class implements the builder pattern to create a {@link ThreadFactory} with certain
 * preset properties. The implementation behaves like {@link ThreadFactoryBuilder} but with
 * an important difference:
 * When trying to modify the thread (setting its name, priority,...) any occurring {@link SecurityException}
 * will be silently caught. This is important to make threading work on Google App Engine.
 *
 * Unfortunately the original ThreadFactoryBuilder class cannot be sub-classed because it declared as final.
 *
 * @see CatchSecurityViolationThreadFactoryBuilder
 */
public class CatchSecurityViolationThreadFactoryBuilder {
  private Boolean daemon;
  private String nameFormat;
  private Integer priority;
  private UncaughtExceptionHandler handler;
  private ThreadFactory backingFactory;

  /**
   * @see CatchSecurityViolationThreadFactoryBuilder#setThreadFactory(ThreadFactory)
   */
  public CatchSecurityViolationThreadFactoryBuilder setThreadFactory(ThreadFactory factory) {
    backingFactory = checkNotNull(factory);
    return this;
  }

  /**
   * @see ThreadFactoryBuilder#setUncaughtExceptionHandler(UncaughtExceptionHandler)
   */
  public CatchSecurityViolationThreadFactoryBuilder setUncaughtExceptionHandler(UncaughtExceptionHandler pHandler) {
    handler = checkNotNull(pHandler);
    return this;
  }

  /**
   * @see ThreadFactoryBuilder#setPriority(int)
   */
  public CatchSecurityViolationThreadFactoryBuilder setPriority(int pPriority) {
    priority =  pPriority;
    return this;
  }

  /**
   * @see ThreadFactoryBuilder#setNameFormat(String)
   */
  public CatchSecurityViolationThreadFactoryBuilder setNameFormat(String pNamingFormat) {
    nameFormat = checkNotNull(pNamingFormat);
    return this;
  }

  /**
   * @see ThreadFactoryBuilder#setDaemon(boolean)
   */
  public CatchSecurityViolationThreadFactoryBuilder setDaemon(boolean pDaemon) {
    daemon = pDaemon;
    return this;
  }

  /**
   * @see ThreadFactoryBuilder#build()
   */
  public ThreadFactory build() {
    return build(this);
  }

  private static ThreadFactory build(CatchSecurityViolationThreadFactoryBuilder builder) {
    final Boolean daemon = builder.daemon;
    final String nameFormat = builder.nameFormat;
    final Integer priority = builder.priority;
    final UncaughtExceptionHandler handler = builder.handler;
    final AtomicLong count = new AtomicLong(0);
    final ThreadFactory factory = (builder.backingFactory != null)
        ? builder.backingFactory
        : Executors.defaultThreadFactory();
    return new ThreadFactory() {
      @Override
      public Thread newThread(Runnable r) {
        Thread thread = factory.newThread(r);
        try {
          if (daemon != null) {
            thread.setDaemon(daemon);
          }
          if (nameFormat != null) {
            thread.setName(String.format(nameFormat, count.getAndIncrement()));
          }
          if (priority != null) {
            thread.setPriority(priority);
          }
          if (handler != null) {
            thread.setUncaughtExceptionHandler(handler);
          }
        } catch (SecurityException e) {
          // silently ignore
        }

        return thread;
      }
    };
  }
}