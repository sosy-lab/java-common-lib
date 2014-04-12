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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class CatchSecurityViolationThreadFactoryBuilderTest {

  private CatchSecurityViolationThreadFactoryBuilder builder;
  private Runnable mockRunnable = mock(Runnable.class);

  @Before
  public void setUp() {
    builder = new CatchSecurityViolationThreadFactoryBuilder();
  }

  @Test
  public void shouldSetDaemon() throws Exception {
    Thread t = builder.setDaemon(true).build().newThread(mockRunnable);
    assertTrue(t.isDaemon());
  }

  @Test
  public void shouldUseNameFormat() throws Exception {
    ThreadFactory f = builder.setNameFormat("test-%d").build();
    Thread t1 = f.newThread(mockRunnable);
    Thread t2 = f.newThread(mockRunnable);

    assertEquals("test-0", t1.getName());
    assertEquals("test-1", t2.getName());
  }

  @Test
  public void shouldSetPriority() throws Exception {
    Thread t = builder.setPriority(Thread.MAX_PRIORITY).build().newThread(mockRunnable);
    assertEquals(Thread.MAX_PRIORITY, t.getPriority());
  }

  @Test
  public void shouldSetExceptionHandler() throws Exception {
    UncaughtExceptionHandler mockHandler = mock(UncaughtExceptionHandler.class);
    Thread t = builder.setUncaughtExceptionHandler(mockHandler).build().newThread(mockRunnable);
    assertEquals(mockHandler, t.getUncaughtExceptionHandler());
  }

  @Test
  public void shouldUseCustomThreadFactory() throws Exception {
    ThreadFactory stubFactory = mock(ThreadFactory.class);
    Thread mockThread = mock(Thread.class);
    when(stubFactory.newThread(any(Runnable.class))).thenReturn(mockThread);

    Thread t = builder.setThreadFactory(stubFactory).build().newThread(mockRunnable);

    Assert.assertEquals(mockThread, t);
  }
}
