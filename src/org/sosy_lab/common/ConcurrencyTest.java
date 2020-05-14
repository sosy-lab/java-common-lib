// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
