// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class PersistentDequeTest {

  PersistentDeque<Object> emptyDeque;
  PersistentDeque<Object> size1Deque;
  PersistentDeque<Object> fullDeque;
  Object o1;
  Object o2;
  Object o3;
  Object o4;
  Object o5;

  @Before
  public void setup() {

    emptyDeque = new PersistentDeque<>();

    size1Deque = new PersistentDeque<>();
    o5 = new Object();
    size1Deque = size1Deque.insertTop(o5);

    fullDeque = new PersistentDeque<>();
    o1 = new Object();
    o2 = new Object();
    o3 = new Object();
    o4 = new Object();
    fullDeque = fullDeque.insertTop(o2);
    fullDeque = fullDeque.insertTop(o1);
    fullDeque = fullDeque.insertBottom(o3);
    fullDeque = fullDeque.insertBottom(o4);
  }

  @Test
  public void testEmptyDeque() {
    assertTrue(emptyDeque.isEmpty());

    assertNull(emptyDeque.getTop());
    assertNull(emptyDeque.getBottom());
  }

  @Test
  public void testInsert() {
    assertTrue(emptyDeque.isEmpty());

    emptyDeque = emptyDeque.insertTop(o2);
    emptyDeque = emptyDeque.insertTop(o1);
    emptyDeque = emptyDeque.insertBottom(o3);
    emptyDeque = emptyDeque.insertBottom(o4);

    assertThat(emptyDeque.top).containsExactly(o1, o2).inOrder();
    assertThat(emptyDeque.bottom).containsExactly(o4, o3).inOrder();
  }

  @Test
  public void testFullDeque() {
    assertFalse(fullDeque.isEmpty());

    assertEquals(o1, fullDeque.getTop());
    assertEquals(o4, fullDeque.getBottom());
  }

  @Test
  public void testRemove() {
    assertFalse(fullDeque.isEmpty());

    fullDeque = fullDeque.deleteTop();
    assertThat(fullDeque.top).containsExactly(o2);
    assertThat(fullDeque.bottom).containsExactly(o4, o3).inOrder();

    fullDeque = fullDeque.deleteTop();
    assertThat(fullDeque.top).containsExactly(o3);
    assertThat(fullDeque.bottom).containsExactly(o4);

    fullDeque = fullDeque.deleteBottom();
    assertThat(fullDeque.top).containsExactly(o3);

    fullDeque = fullDeque.deleteBottom();
    assertTrue(fullDeque.isEmpty());
  }

  @Test
  public void testDequeOfSize1() {
    assertFalse(size1Deque.isEmpty());

    assertEquals(o5, size1Deque.getTop());
    assertEquals(o5, size1Deque.getBottom());
  }
}
