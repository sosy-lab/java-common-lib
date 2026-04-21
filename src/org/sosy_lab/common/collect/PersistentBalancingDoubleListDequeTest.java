// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;

public class PersistentBalancingDoubleListDequeTest {

  PersistentBalancingDoubleListDeque<Object> emptyDeque;
  PersistentBalancingDoubleListDeque<Object> size1Deque;
  PersistentBalancingDoubleListDeque<Object> fullDeque;
  Object o1;
  Object o2;
  Object o3;
  Object o4;
  Object o5;

  @Before
  public void setup() {

    emptyDeque = new PersistentBalancingDoubleListDeque<>();

    size1Deque = new PersistentBalancingDoubleListDeque<>();
    o5 = new Object();
    size1Deque = size1Deque.insertTop(o5);

    fullDeque = new PersistentBalancingDoubleListDeque<>();
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
    assertThat(emptyDeque.isEmpty()).isTrue();

    assertThat(emptyDeque.getTop()).isNull();
    assertThat(emptyDeque.getBottom()).isNull();
  }

  @Test
  public void testInsert() {
    assertThat(emptyDeque.isEmpty()).isTrue();

    emptyDeque = emptyDeque.insertTop(o2);
    emptyDeque = emptyDeque.insertTop(o1);
    emptyDeque = emptyDeque.insertBottom(o3);
    emptyDeque = emptyDeque.insertBottom(o4);

    assertThat(emptyDeque.top).containsExactly(o1, o2).inOrder();
    assertThat(emptyDeque.bottom).containsExactly(o4, o3).inOrder();
  }

  @Test
  public void testFullDeque() {
    assertThat(fullDeque.isEmpty()).isFalse();

    assertThat(fullDeque.getTop()).isEqualTo(o1);
    assertThat(fullDeque.getBottom()).isEqualTo(o4);
  }

  @Test
  public void testRemove() {
    assertThat(fullDeque.isEmpty()).isFalse();

    fullDeque = fullDeque.deleteTop();
    assertThat(fullDeque.top).containsExactly(o2);
    assertThat(fullDeque.bottom).containsExactly(o4, o3).inOrder();

    fullDeque = fullDeque.deleteTop();
    assertThat(fullDeque.top).containsExactly(o3);
    assertThat(fullDeque.bottom).containsExactly(o4);

    fullDeque = fullDeque.deleteBottom();
    assertThat(fullDeque.top).containsExactly(o3);

    fullDeque = fullDeque.deleteBottom();
    assertThat(fullDeque.isEmpty()).isTrue();
  }

  @Test
  public void testDequeOfSize1() {
    assertThat(size1Deque.isEmpty()).isFalse();

    assertThat(size1Deque.getTop()).isEqualTo(o5);
    assertThat(size1Deque.getBottom()).isEqualTo(o5);
  }
}
