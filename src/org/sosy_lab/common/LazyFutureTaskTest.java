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
import static org.hamcrest.CoreMatchers.is;

import com.google.common.util.concurrent.Callables;
import com.google.common.util.concurrent.Runnables;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class LazyFutureTaskTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void testRunnable() throws InterruptedException, ExecutionException {
    final AtomicBoolean test = new AtomicBoolean(false);

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
  public void testException() throws InterruptedException, ExecutionException {
    final NullPointerException testException = new NullPointerException();

    thrown.expect(ExecutionException.class);
    thrown.expectCause(is(testException));

    Future<Boolean> f =
        new LazyFutureTask<>(
            () -> {
              throw testException;
            });

    f.get();
  }

  @Test
  @SuppressWarnings("unused")
  public void testNoExecution() {
    final AtomicBoolean test = new AtomicBoolean(true);

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
  public void testCancel() throws InterruptedException, ExecutionException {
    thrown.expect(CancellationException.class);

    Future<Void> f = new LazyFutureTask<>(Runnables.doNothing(), null);

    f.cancel(false);
    f.get();
  }
}
