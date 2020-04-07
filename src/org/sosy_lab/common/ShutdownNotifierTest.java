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
import static org.junit.Assert.assertThrows;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;

public class ShutdownNotifierTest {

  private static final String REASON = "Shutdown Request Reason";

  @SuppressWarnings("FieldCanBeLocal") // must not be garbage collected during test
  private @Nullable ShutdownManager manager;

  private @Nullable ShutdownNotifier instance;

  @Before
  public void setUp() {
    manager = ShutdownManager.create();
    instance = manager.getNotifier();
  }

  @After
  public void tearDown() {
    instance = null;
  }

  @Test
  public void testNotRequested() throws InterruptedException {
    assertThat(instance.shouldShutdown()).isFalse();
    instance.shutdownIfNecessary();
  }

  @Test
  public void testNotRequestedReason() {
    assertThrows(IllegalStateException.class, () -> instance.getReason());
  }

  @Test
  public void testRequested() {
    instance.requestShutdown(REASON);
    assertThat(instance.shouldShutdown()).isTrue();
    assertThat(instance.getReason()).isEqualTo(REASON);
  }

  @Test
  public void testRequestedException() {
    instance.requestShutdown(REASON);

    InterruptedException thrown =
        assertThrows(InterruptedException.class, () -> instance.shutdownIfNecessary());
    assertThat(thrown).hasMessageThat().isEqualTo(REASON);
  }

  @Test
  public void testRegisterListenerTwice() {
    ShutdownRequestListener l = reason -> {};

    instance.register(l);
    assertThrows(IllegalArgumentException.class, () -> instance.register(l));
  }

  @Test
  public void testListenerNotification() {
    AtomicBoolean flag = new AtomicBoolean(false);

    instance.register(reason -> flag.set(true));

    instance.requestShutdown(REASON);
    assertThat(flag.get()).isTrue();
  }

  @Test
  public void testListenerNotificationReason() {
    AtomicReference<String> reasonReference = new AtomicReference<>();

    instance.register(reasonReference::set);

    instance.requestShutdown(REASON);
    assertThat(reasonReference.get()).isEqualTo(REASON);
  }

  @Test
  public void testListenerNotification10() {
    int count = 10;
    AtomicInteger i = new AtomicInteger(0);

    for (int j = 0; j < count; j++) {
      instance.register(reason -> i.incrementAndGet());
    }

    instance.requestShutdown(REASON);
    assertThat(i.get()).isEqualTo(count);
  }

  @Test
  public void testUnregisterListener() {
    AtomicBoolean flag = new AtomicBoolean(false);

    ShutdownRequestListener l = reason -> flag.set(true);
    instance.register(l);
    instance.unregister(l);

    instance.requestShutdown(REASON);
    assertThat(flag.get()).isFalse();
  }

  @Test
  public void testListenerRegisterAndCheck() {
    AtomicBoolean flag = new AtomicBoolean(false);

    instance.registerAndCheckImmediately(reason -> flag.set(true));

    assertThat(flag.get()).isFalse();
    instance.requestShutdown(REASON);
    assertThat(flag.get()).isTrue();
  }

  @Test
  public void testListenerNotificationOnRegister() {
    AtomicBoolean flag = new AtomicBoolean(false);

    instance.requestShutdown(REASON);
    instance.registerAndCheckImmediately(reason -> flag.set(true));

    assertThat(flag.get()).isTrue();
  }

  @Test
  public void testListenerNotificationReasonOnRegister() {
    AtomicReference<String> reasonReference = new AtomicReference<>();

    instance.requestShutdown(REASON);
    instance.registerAndCheckImmediately(reasonReference::set);

    assertThat(reasonReference.get()).isEqualTo(REASON);
  }

  @Test
  public void testParentChild() {
    ShutdownNotifier child = ShutdownManager.createWithParent(instance).getNotifier();

    assertThat(instance.shouldShutdown()).isFalse();
    assertThat(child.shouldShutdown()).isFalse();

    instance.requestShutdown(REASON);

    assertThat(child.shouldShutdown()).isTrue();
    assertThat(child.getReason()).isEqualTo(REASON);
  }

  @Test
  public void testChildParent() {
    ShutdownNotifier child = ShutdownManager.createWithParent(instance).getNotifier();

    assertThat(instance.shouldShutdown()).isFalse();
    assertThat(child.shouldShutdown()).isFalse();

    child.requestShutdown(REASON);

    assertThat(instance.shouldShutdown()).isFalse();
    assertThat(child.shouldShutdown()).isTrue();
    assertThat(child.getReason()).isEqualTo(REASON);
  }

  @Test
  public void testParentChildListenerNotification() {
    AtomicBoolean flag = new AtomicBoolean(false);

    ShutdownNotifier child = ShutdownManager.createWithParent(instance).getNotifier();

    child.register(reason -> flag.set(true));

    instance.requestShutdown(REASON);
    assertThat(flag.get()).isTrue();
  }

  @Test
  public void testParentChildListenerNotificationReason() {
    AtomicReference<String> reasonReference = new AtomicReference<>();

    ShutdownNotifier child = ShutdownManager.createWithParent(instance).getNotifier();

    child.register(reasonReference::set);

    instance.requestShutdown(REASON);
    assertThat(reasonReference.get()).isEqualTo(REASON);
  }
}
