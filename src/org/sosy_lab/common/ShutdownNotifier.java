// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.MapMaker;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class allows code to check whether it should terminate for some reason, and to be notified
 * of such requests.
 *
 * <p>It works passively, the running analysis will not be interrupted directly, but instead it has
 * to check every then and now whether it should shutdown. This ensures that the running code is not
 * left in an unclean state.
 *
 * <p>The check whether a shutdown was requested is cheap and should be done quite often in order to
 * ensure a timely response to a shutdown request. As a rule of thumb, all operations that may take
 * longer than 1s should take care of calling {@link #shouldShutdown()} or {@link
 * #shutdownIfNecessary()} from time to time.
 *
 * <p>Shutdown requests cannot be issued via this class, but only via {@link ShutdownManager} to
 * allow restricting which code is allowed to request shutdowns.
 *
 * <p>Instances of this class cannot be created directly, instead create a {@link ShutdownManager}
 * and call {@link ShutdownManager#getNotifier()}.
 *
 * <p>This class is completely thread safe.
 */
public final class ShutdownNotifier {

  /*
   * Implementation details:
   * The check whether a shutdown was requested needs to be cheap and thread safe.
   * The cheapest way to do this in Java is to read from a volatile field,
   * and accessing an AtomicReference is the same.
   * We use an AtomicReference instead of a volatile field to be sure
   * that we get the correct reason for the shutdown.
   * The semantics of this field is that there was no shutdown requested
   * as long as reference is null.
   */
  private final AtomicReference<String> shutdownRequested = new AtomicReference<>();

  /**
   * For maintaining the list of listeners, the AtomicReference shutdownRequest is not enough, so we
   * synchronize on the list and have a second flag whether they were already notified.
   */
  private final Set<ShutdownRequestListener> listeners =
      // This creates a set which is backed by a thread-safe map
      // with identity comparison (== instead of equals())
      // and weak references for the keys
      Collections.newSetFromMap(
          new MapMaker()
              .concurrencyLevel(1)
              .weakKeys()
              .<ShutdownRequestListener, Boolean>makeMap());

  // Separate flag for notification of listeners
  // in order to prevent a race condition when registering a listener
  // and calling requestStop() at the same time.
  // This variable is not volatile and always needs to accessed from within
  // a synchronized (listeners) { } block!
  @GuardedBy("listeners")
  private boolean listenersNotified = false;

  // Do not remove this field, otherwise in a cascade of ShutdownManagers some intermediate
  // ShutdownManagers could potentially be garbage collected
  // and we would miss shutdown notifications
  // (in such a cascade we need references from child ShutdownManagers to parent ShutdownManagers,
  // and this field is part of this).
  @SuppressWarnings("unused")
  private final @Nullable ShutdownManager manager;

  ShutdownNotifier(@Nullable ShutdownManager pManager) {
    manager = pManager;
  }

  /**
   * Create an instance that will never return true for {@link #shouldShutdown()} and will never
   * notify its listeners. This may be handy for tests.
   *
   * <p>To create a real usable ShutdownNotifier, use {@link ShutdownManager#create()}.
   */
  public static ShutdownNotifier createDummy() {
    return new ShutdownNotifier(null);
  }

  /**
   * Request a shutdown of all components that check this instance, by letting {@link
   * #shouldShutdown()} return true in the future, and by notifying all registered listeners. Only
   * the first call to this method has an effect. When this method returns, it is guaranteed that
   * all currently registered listeners where notified and have been unregistered.
   *
   * <p>This method is implemented here but only called from {@link
   * ShutdownManager#requestShutdown(String)}, which exposes it publicly.
   *
   * @param pReason A non-null human-readable string that tells the user why a shutdown was
   *     requested.
   */
  void requestShutdown(String pReason) {
    checkNotNull(pReason);

    if (shutdownRequested.compareAndSet(null, pReason)) {
      // Shutdown was not requested before, only one thread ever enters this block.

      // Notify listeners
      // Additional synchronization necessary for registerAndCheckImmediately()
      // and for iterating over the list.
      synchronized (listeners) {
        assert !listenersNotified;
        listenersNotified = true;

        for (ShutdownRequestListener listener : listeners) {
          // TODO exception safety
          listener.shutdownRequested(pReason);
        }
        listeners.clear();
      }
    }
  }

  /**
   * Check whether a shutdown was previously requested. This method returns false immediately after
   * this instance was constructed, and may return true later on. After it returned true once it
   * will always keep returning true, and never return false again. Calling this method is very
   * cheap.
   */
  public boolean shouldShutdown() {
    return shutdownRequested.get() != null;
  }

  /**
   * Check whether a shutdown was previously requested, and throw an {@link InterruptedException} in
   * this case. Once a shutdown was requested, every call to this method will throw an exception. In
   * the common case that no shutdown was yet requested, calling this method is very cheap.
   *
   * @throws InterruptedException If a shutdown was requested.
   */
  public void shutdownIfNecessary() throws InterruptedException {
    if (shouldShutdown()) {
      throw new InterruptedException(getReason());
    }
  }

  /**
   * Return the reason for the shutdown request on this instance.
   *
   * @return A non-null human-readable string.
   * @throws IllegalStateException If there was no shutdown request on this instance.
   */
  public String getReason() {
    String reason = shutdownRequested.get();
    checkState(reason != null, "Cannot call getReason() on an instance with no shutdown request.");
    return reason;
  }

  /**
   * Register a listener that will be notified once a shutdown is requested for the first time on
   * the associated {@link ShutdownManager} instance with {@link
   * ShutdownManager#requestShutdown(String)}.
   *
   * <p>Listeners registered when {@link #shouldShutdown()} already returns true will never be
   * notified (so calling this method at that time has no effect).
   *
   * <p>This class keeps only weak reference to the listener to allow the GC to collect them, so
   * make sure to keep a strong reference to your instance as long as you won't to be notified.
   *
   * @param listener A non-null and not already registered listener.
   */
  public void register(ShutdownRequestListener listener) {
    checkNotNull(listener);

    synchronized (listeners) {
      if (!listenersNotified) {
        boolean freshListener = listeners.add(listener);
        checkArgument(freshListener, "Not allowed to register listeners twice");
      }
      // else do nothing because its irrelevant
    }
  }

  /**
   * Register a listener that will be notified once a shutdown is requested for the first time on
   * the associated {@link ShutdownManager} instance with {@link
   * ShutdownManager#requestShutdown(String)}, or immediately if this was already the case.
   *
   * <p>Use this method to avoid a race condition when registering the listener and checking for a
   * requested shutdown at the same time (you could loose a notification).
   *
   * <p>This class keeps only weak reference to the listener to allow the GC to collect them, so
   * make sure to keep a strong reference to your instance as long as you won't to be notified.
   *
   * @param listener A non-null and not already registered listener.
   */
  public void registerAndCheckImmediately(ShutdownRequestListener listener) {
    synchronized (listeners) { // synchronized block to have atomic "register-or-notify"
      register(listener); // does nothing if listenersNotified==true

      if (listenersNotified) {
        String reason = getReason();
        // Listeners were already notified previously,
        // this listener would not get called.
        listener.shutdownRequested(reason);
      }
    }
  }

  /**
   * Unregister a listener. This listener will not be notified in the future. It is safe to call
   * this method twice with the same listener. It is not necessary to call this method for a
   * listener that was already notified.
   *
   * @param listener A previously registered listener.
   */
  public void unregister(ShutdownRequestListener listener) {
    checkNotNull(listener);

    // listeners is thread-safe
    listeners.remove(listener);
  }

  /**
   * Utility method for creating a {@link ShutdownRequestListener} that interrupts the current
   * thread (that calls this method) on a shutdown. Note that this method does not actually do
   * anything, you need to register the returned listener with an instance of this class.
   */
  public static ShutdownRequestListener interruptCurrentThreadOnShutdown() {
    Thread currentThread = Thread.currentThread();
    return pReason -> currentThread.interrupt();
  }

  @FunctionalInterface
  public interface ShutdownRequestListener {

    /**
     * This method is called on registered listeners the first time {@link
     * ShutdownManager#requestShutdown(String)} on the associated {@link ShutdownManager} instance
     * is called.
     *
     * <p>Implementations of this method should be reasonably quick and never throw an exception.
     *
     * <p>Note that it is usually not necessary to use a listener when all you want to do in this
     * method is to set some boolean flag. Instead, just call {@link
     * ShutdownNotifier#shouldShutdown()} whenever you would check the flag (this is similarly cheap
     * and thread-safe).
     *
     * @param reason A non-null human-readable string that tells the user why a shutdown was
     *     requested.
     */
    void shutdownRequested(String reason);
  }
}
