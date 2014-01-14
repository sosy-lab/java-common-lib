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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.ThreadFactory;

import org.junit.Assert;
import org.junit.Test;


public class ThreadsTest {

  @Test
  public void shouldReturnDefaultThreadFactory() throws Exception {
    Threads.setThreadFactory(null);
    ThreadFactory factory = Threads.threadFactory();

    Assert.assertNotNull(factory);
  }

  @Test
  public void shouldUseCustomFactory() throws Exception {
    ThreadFactory stubFactory = mock(ThreadFactory.class);
    Thread mockThread = mock(Thread.class);
    when(stubFactory.newThread(any(Runnable.class))).thenReturn(mockThread);

    Threads.setThreadFactory(stubFactory);

    Assert.assertEquals(mockThread, Threads.newThread(mock(Runnable.class)));
  }

  @Test
  public void shouldUseCustomFactoryInThreadFactoryBuilder() throws Exception {
    ThreadFactory stubFactory = mock(ThreadFactory.class);
    Thread mockThread = mock(Thread.class);
    when(stubFactory.newThread(any(Runnable.class))).thenReturn(mockThread);

    Threads.setThreadFactory(stubFactory);

    Assert.assertEquals(mockThread, Threads.threadFactoryBuilder().build().newThread(mock(Runnable.class)));
  }
}
