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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ShutdownNotifierTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  private static final String REASON = "Shutdown Request Reason";

  private ShutdownManager manager = null;
  private ShutdownNotifier instance = null;

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
    assertFalse(instance.shouldShutdown());
    instance.shutdownIfNecessary();
  }

  @Test
  public void testNotRequestedReason() {
    thrown.expect(IllegalStateException.class);
    instance.getReason();
  }

  @Test
  public void testRequested() {
    instance.requestShutdown(REASON);
    assertTrue(instance.shouldShutdown());
    assertEquals(REASON, instance.getReason());
  }

  @Test
  public void testRequestedException() throws InterruptedException {
    instance.requestShutdown(REASON);

    thrown.expect(InterruptedException.class);
    thrown.expectMessage(REASON);
    instance.shutdownIfNecessary();
  }

  @Test
  public void testRegisterListenerTwice() {
    ShutdownRequestListener l =
        new ShutdownRequestListener() {
          @Override
          public void shutdownRequested(String reason) {}
        };

    instance.register(l);
    thrown.expect(IllegalArgumentException.class);
    instance.register(l);
  }

  @Test
  public void testListenerNotification() {
    final AtomicBoolean flag = new AtomicBoolean(false);

    instance.register(
        new ShutdownRequestListener() {
          @Override
          public void shutdownRequested(String reason) {
            flag.set(true);
          }
        });

    instance.requestShutdown(REASON);
    assertTrue(flag.get());
  }

  @Test
  public void testListenerNotificationReason() {
    final AtomicReference<String> reasonReference = new AtomicReference<>();

    instance.register(
        new ShutdownRequestListener() {
          @Override
          public void shutdownRequested(String pReason) {
            reasonReference.set(pReason);
          }
        });

    instance.requestShutdown(REASON);
    assertEquals(REASON, reasonReference.get());
  }

  @Test
  public void testListenerNotification10() {
    final int count = 10;
    final AtomicInteger i = new AtomicInteger(0);

    for (int j = 0; j < count; j++) {
      instance.register(
          new ShutdownRequestListener() {
            @Override
            public void shutdownRequested(String reason) {
              i.incrementAndGet();
            }
          });
    }

    instance.requestShutdown(REASON);
    assertEquals(count, i.get());
  }

  @Test
  public void testUnregisterListener() {
    final AtomicBoolean flag = new AtomicBoolean(false);

    ShutdownRequestListener l =
        new ShutdownRequestListener() {
          @Override
          public void shutdownRequested(String reason) {
            flag.set(true);
          }
        };
    instance.register(l);
    instance.unregister(l);

    instance.requestShutdown(REASON);
    assertFalse(flag.get());
  }

  @Test
  public void testListenerRegisterAndCheck() {
    final AtomicBoolean flag = new AtomicBoolean(false);

    instance.registerAndCheckImmediately(
        new ShutdownRequestListener() {
          @Override
          public void shutdownRequested(String reason) {
            flag.set(true);
          }
        });

    assertFalse(flag.get());
    instance.requestShutdown(REASON);
    assertTrue(flag.get());
  }

  @Test
  public void testListenerNotificationOnRegister() {
    final AtomicBoolean flag = new AtomicBoolean(false);

    instance.requestShutdown(REASON);
    instance.registerAndCheckImmediately(
        new ShutdownRequestListener() {
          @Override
          public void shutdownRequested(String reason) {
            flag.set(true);
          }
        });

    assertTrue(flag.get());
  }

  @Test
  public void testListenerNotificationReasonOnRegister() {
    final AtomicReference<String> reasonReference = new AtomicReference<>();

    instance.requestShutdown(REASON);
    instance.registerAndCheckImmediately(
        new ShutdownRequestListener() {
          @Override
          public void shutdownRequested(String pReason) {
            reasonReference.set(pReason);
          }
        });

    assertEquals(REASON, reasonReference.get());
  }

  @Test
  public void testParentChild() {
    ShutdownNotifier child = ShutdownManager.createWithParent(instance).getNotifier();

    assertFalse(instance.shouldShutdown());
    assertFalse(child.shouldShutdown());

    instance.requestShutdown(REASON);

    assertTrue(child.shouldShutdown());
    assertEquals(REASON, child.getReason());
  }

  @Test
  public void testChildParent() {
    ShutdownNotifier child = ShutdownManager.createWithParent(instance).getNotifier();

    assertFalse(instance.shouldShutdown());
    assertFalse(child.shouldShutdown());

    child.requestShutdown(REASON);

    assertFalse(instance.shouldShutdown());
    assertTrue(child.shouldShutdown());
    assertEquals(REASON, child.getReason());
  }

  @Test
  public void testParentChildListenerNotification() {
    final AtomicBoolean flag = new AtomicBoolean(false);

    ShutdownNotifier child = ShutdownManager.createWithParent(instance).getNotifier();

    child.register(
        new ShutdownRequestListener() {
          @Override
          public void shutdownRequested(String reason) {
            flag.set(true);
          }
        });

    instance.requestShutdown(REASON);
    assertTrue(flag.get());
  }

  @Test
  public void testParentChildListenerNotificationReason() {
    final AtomicReference<String> reasonReference = new AtomicReference<>();

    ShutdownNotifier child = ShutdownManager.createWithParent(instance).getNotifier();

    child.register(
        new ShutdownRequestListener() {
          @Override
          public void shutdownRequested(String pReason) {
            reasonReference.set(pReason);
          }
        });

    instance.requestShutdown(REASON);
    assertEquals(REASON, reasonReference.get());
  }
}
