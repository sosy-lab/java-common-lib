// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.util.concurrent.Callables;
import com.google.common.util.concurrent.Runnables;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

public class LazyFutureTaskTest {

  @Test
  public void testRunnable() throws InterruptedException, ExecutionException {
    AtomicBoolean test = new AtomicBoolean(false);

    Future<Boolean> f = new LazyFutureTask<>(() -> test.set(true), true);

    assertThat(f.get()).isTrue();
    assertThat(test.get()).isTrue();
  }

  @Test
  public void testCallable() throws InterruptedException, ExecutionException {

    Future<Boolean> f = new LazyFutureTask<>(Callables.returning(true));

    assertThat(f.get()).isTrue();
  }

  @Test
  public void testException() {
    NullPointerException testException = new NullPointerException();

    Future<Boolean> f =
        new LazyFutureTask<>(
            () -> {
              throw testException;
            });

    ExecutionException thrown = assertThrows(ExecutionException.class, () -> f.get());
    assertThat(thrown).hasCauseThat().isSameInstanceAs(testException);
  }

  @Test
  @SuppressWarnings("unused")
  public void testNoExecution() {
    AtomicBoolean test = new AtomicBoolean(true);

    new LazyFutureTask<Void>(() -> test.set(false), null);

    // no call to f.get()
    assertThat(test.get()).isTrue();
  }

  @Test
  @SuppressWarnings("unused")
  public void testExceptionNoExecution() {
    new LazyFutureTask<>(
        () -> {
          throw new NullPointerException();
        });

    // no call to f.get()
  }

  @Test
  public void testCancel() {
    Future<Void> f = new LazyFutureTask<>(Runnables.doNothing(), null);

    f.cancel(false);

    assertThrows(CancellationException.class, () -> f.get());
  }
}
