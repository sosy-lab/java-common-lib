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

import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;

/**
 * Together with {@link ShutdownNotifier}, this class implements a service
 * for distributing shutdown requests throughout an application's component,
 * potentially in a hierarchy.
 *
 * Possible use cases are for example to implement timeouts or respond to
 * Ctrl+C by terminating the application in a graceful manner,
 * i.e., letting the running code terminate itself and do cleanup.
 * It works passively, i.e., the running code is not forcibly interrupted,
 * but needs to check the {@link ShutdownNotifier} instance that is associated
 * with this instance regularly.
 * This ensures that the running code is not left in an unclean state.
 *
 * This class is the entry point and allows issuing shutdown requests with
 * {@link #requestShutdown(String)}. All components that need to get these requests
 * or check whether the should terminate should get the {@link ShutdownNotifier} instance
 * that is returned by {@link #getNotifier()}.
 * By handing out only {@link ShutdownNotifier} instances instead of
 * {@link ShutdownManager} instances, it can be controlled which components
 * have the possibility to issue shutdown requests,
 * and which components may only respond to them.
 *
 * This class supports a hierarchy of instances.
 * Setting the shutdown request on a higher-level instance will do the same in all
 * children instances (recursively), but not vice-versa.
 * This can be used for example to implement global and component-specific timeouts
 * at the same time, with the former overriding the latter if necessary.
 *
 * This class does not implement any timeout by itself.
 * A separate component needs to be used that implements the timeout
 * and calls {{@link #requestShutdown(String)}} in case it is reached.
 *
 * This class and {@link ShutdownNotifier} are completely thread safe.
 */
public final class ShutdownManager {

  // Do not remove this field, otherwise the listener will be garbage collected
  // and we could miss notifications.
  private final ShutdownRequestListener ourListener = ShutdownManager.this::requestShutdown;

  private final ShutdownNotifier notifier = new ShutdownNotifier();

  private ShutdownManager() {}

  /**
   * Create a fresh new instance of this class.
   * The associated {@link ShutdownNotifier} has no listeners
   * and shutdown has not been requested yet.
   */
  public static ShutdownManager create() {
    return new ShutdownManager();
  }

  /**
   * Create a fresh new instance of this class in a hierarchy.
   *
   * The new instance is considered to be a child of the given {@link ShutdownNotifier},
   * this means as soon as the parent has a shutdown requested,
   * the same is true for the child instance (but not vice-versa).
   * Note that if the parent instance already has shutdown requested,
   * the new instance is also immediately in the same state.
   *
   * @param parent A non-null ShutdownNotifier instance.
   */
  public static ShutdownManager createWithParent(final ShutdownNotifier parent) {
    final ShutdownManager child = create();
    parent.registerAndCheckImmediately(child.ourListener);
    return child;
  }

  public ShutdownNotifier getNotifier() {
    return notifier;
  }

  /**
   * Request a shutdown of all components that check the associated {@link ShutdownNotifier},
   * by letting {@link ShutdownNotifier#shouldShutdown()} return true in the future,
   * and by notifying all registered listeners.
   * Only the first call to this method has an effect.
   * When this method returns, it is guaranteed that all currently registered
   * listeners where notified and have been unregistered.
   *
   * @param pReason A non-null human-readable string that tells the user
   * why a shutdown was requested.
   */
  public void requestShutdown(final String pReason) {
    notifier.requestShutdown(pReason);
  }
}
