// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common;

import static com.google.common.base.Preconditions.checkNotNull;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;

/**
 * Together with {@link ShutdownNotifier}, this class implements a service for distributing shutdown
 * requests throughout an application's component, potentially in a hierarchy.
 *
 * <p>Possible use cases are for example to implement timeouts or respond to Ctrl+C by terminating
 * the application in a graceful manner, i.e., letting the running code terminate itself and do
 * cleanup. It works passively, i.e., the running code is not forcibly interrupted, but needs to
 * check the {@link ShutdownNotifier} instance that is associated with this instance regularly. This
 * ensures that the running code is not left in an unclean state.
 *
 * <p>A {@link ShutdownManager} can only be used for one shutdown request: After creation it starts
 * in the fresh "no shutdown requested" state. After the first call to {@link
 * #requestShutdown(String)} is in the "shutdown requested" state and remains there forever. All
 * subsequent calls to <code>getNotifier().shouldShutdown()</code> will return true, and further
 * calls to {@link #requestShutdown(String)} are ignored.
 *
 * <p>This class is the entry point and allows issuing shutdown requests with {@link
 * #requestShutdown(String)}. All components that need to get these requests or check whether the
 * should terminate should get the {@link ShutdownNotifier} instance that is returned by {@link
 * #getNotifier()}. By handing out only {@link ShutdownNotifier} instances instead of {@link
 * ShutdownManager} instances, it can be controlled which components have the possibility to issue
 * shutdown requests, and which components may only respond to them.
 *
 * <p>This class supports a hierarchy of instances. Setting the shutdown request on a higher-level
 * instance will do the same in all children instances (recursively), but not vice-versa. This can
 * be used for example to implement global and component-specific timeouts at the same time, with
 * the former overriding the latter if necessary.
 *
 * <p>This class does not implement any timeout by itself. A separate component needs to be used
 * that implements the timeout and calls {{@link #requestShutdown(String)}} in case it is reached.
 *
 * <p>This class and {@link ShutdownNotifier} are completely thread safe.
 */
public final class ShutdownManager {

  // Do not remove this field, otherwise the listener will be garbage collected
  // and we could miss notifications.
  private final ShutdownRequestListener ourListener = ShutdownManager.this::requestShutdown;

  private final ShutdownNotifier notifier = new ShutdownNotifier(this);

  // Do not remove this field, otherwise in a cascade of ShutdownManagers some intermediate
  // ShutdownManagers could potentially be garbage collected
  // and we would miss shutdown notifications
  // (in such a cascade we need references from child ShutdownManagers to parent ShutdownManagers,
  // and this field is part of this).
  @SuppressWarnings("unused")
  private final @Nullable ShutdownNotifier parent;

  private ShutdownManager(@Nullable ShutdownNotifier pParent) {
    parent = pParent;
  }

  /**
   * Create a fresh new instance of this class. The associated {@link ShutdownNotifier} has no
   * listeners and shutdown has not been requested yet.
   */
  public static ShutdownManager create() {
    return new ShutdownManager(null);
  }

  /**
   * Create a fresh new instance of this class in a hierarchy.
   *
   * <p>The new instance is considered to be a child of the given {@link ShutdownNotifier}, this
   * means as soon as the parent has a shutdown requested, the same is true for the child instance
   * (but not vice-versa). Note that if the parent instance already has shutdown requested, the new
   * instance is also immediately in the same state.
   *
   * @param parent A non-null ShutdownNotifier instance.
   */
  public static ShutdownManager createWithParent(ShutdownNotifier parent) {
    ShutdownManager child = new ShutdownManager(checkNotNull(parent));
    parent.registerAndCheckImmediately(child.ourListener);
    return child;
  }

  public ShutdownNotifier getNotifier() {
    return notifier;
  }

  /**
   * Request a shutdown of all components that check the associated {@link ShutdownNotifier}, by
   * letting {@link ShutdownNotifier#shouldShutdown()} return true in the future, and by notifying
   * all registered listeners. Only the first call to this method has an effect. When this method
   * returns, it is guaranteed that all currently registered listeners where notified and have been
   * unregistered.
   *
   * @param pReason A non-null human-readable string that tells the user why a shutdown was
   *     requested.
   */
  public void requestShutdown(String pReason) {
    notifier.requestShutdown(pReason);
  }
}
