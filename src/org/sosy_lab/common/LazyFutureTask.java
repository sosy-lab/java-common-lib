// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Future implementation that can be used when a task should be executed only lazily at the first
 * time {@link #get()} is called. I.e., it is not guaranteed that the task is run at all, but it is
 * called at most once.
 *
 * <p>Execution of the task happens in the caller's thread, a little bit similar to the use of
 * {@link com.google.common.util.concurrent.MoreExecutors#newDirectExecutorService()}, however, it
 * is executed on the thread calling {@link #get()} and not on the thread calling {@link
 * java.util.concurrent.ExecutorService#submit(Callable)}.
 *
 * <p>Important: Calling {@link #get(long, TimeUnit)} is not supported and will always throw {@link
 * UnsupportedOperationException}.
 *
 * <p>Canceling this future works as expected.
 */
public class LazyFutureTask<V extends @Nullable Object> extends FutureTask<V> {

  public LazyFutureTask(Callable<V> pCallable) {
    super(pCallable);
  }

  public LazyFutureTask(Runnable pRunnable, V pResult) {
    super(pRunnable, pResult);
  }

  @Override
  public void run() {
    // Do nothing here, we execute the task only lazily in get().
  }

  @Override
  public V get() throws InterruptedException, ExecutionException {
    if (!isDone()) {
      // Note that two threads calling this method at the same time is safe
      // (the task won't actually be executed twice)
      // because super.run() checks whether the future is currently in state
      // RUNNING and does nothing in this case.
      // (This is an advantage over using Guava's AbstractFuture,
      // where we would have to do this ourselves.)
      super.run();
    }

    return super.get();
  }

  /**
   * @throws UnsupportedOperationException Always
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  public V get(long pTimeout, TimeUnit pUnit) {
    throw new UnsupportedOperationException();
  }
}
