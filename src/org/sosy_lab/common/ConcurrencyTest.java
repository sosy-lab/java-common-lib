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
package org.sosy_lab.common;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class ConcurrencyTest {

  @Rule public MockitoRule mockito = MockitoJUnit.rule();

  @Mock private Runnable mockRunnable;

  @Test
  public void shouldSetDaemon() throws Exception {
    Thread t = Concurrency.newDaemonThread("t", mockRunnable);
    assertThat(t.isDaemon()).isTrue();
  }

  @Test
  public void shouldUseNameFormat() throws Exception {
    Thread t1 = Concurrency.newThread("test-%d", mockRunnable);
    assertThat(t1.getName()).isEqualTo("test-0");
  }
}
