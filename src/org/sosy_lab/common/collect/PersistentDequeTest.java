// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.util.NoSuchElementException;
import org.junit.Test;

public class PersistentDequeTest {

  @Test
  public void testEmptyDequeCreationOnString() {
    PersistentDeque<String> emptyStringDeque = PersistentBalancingDoubleListDeque.of();

    assertThat(emptyStringDeque.isEmpty()).isTrue();

    assertThat(emptyStringDeque.size()).isEqualTo(0);
  }

  @Test
  public void testAllInsertionOperationsOnString() {
    PersistentDeque<String> testStringDeque = PersistentBalancingDoubleListDeque.of();

    testStringDeque = testStringDeque.copyAndPush("c");
    testStringDeque = testStringDeque.copyAndAddFirst("b");
    testStringDeque = testStringDeque.copyAndOfferFirst("a");
    testStringDeque = testStringDeque.copyAndAdd("d");
    testStringDeque = testStringDeque.copyAndOffer("e");
    testStringDeque = testStringDeque.copyAndAddLast("f");
    testStringDeque = testStringDeque.copyAndOfferLast("g");

    assertThat(testStringDeque.size()).isEqualTo(7);
    assertThat(testStringDeque).containsExactly("a", "b", "c", "d", "e", "f", "g").inOrder();
  }

  @Test
  public void testAllRemoveOperationsOnString() {
    PersistentDeque<String> testStringDeque = PersistentBalancingDoubleListDeque.of();

    testStringDeque = testStringDeque.copyAndAdd("a");
    testStringDeque = testStringDeque.copyAndAdd("b");
    testStringDeque = testStringDeque.copyAndAdd("c");
    testStringDeque = testStringDeque.copyAndAdd("d");
    testStringDeque = testStringDeque.copyAndAdd("e");
    testStringDeque = testStringDeque.copyAndAdd("f");
    testStringDeque = testStringDeque.copyAndAdd("g");

    testStringDeque = testStringDeque.copyAndRemove();
    assertThat(testStringDeque).containsExactly("b", "c", "d", "e", "f", "g").inOrder();

    testStringDeque = testStringDeque.copyAndRemoveFirst();
    assertThat(testStringDeque).containsExactly("c", "d", "e", "f", "g").inOrder();

    testStringDeque = testStringDeque.copyAndRemoveLast();
    assertThat(testStringDeque).containsExactly("c", "d", "e", "f").inOrder();

    testStringDeque = testStringDeque.copyAndPop();
    assertThat(testStringDeque).containsExactly("d", "e", "f").inOrder();

    testStringDeque = testStringDeque.copyAndPoll();
    assertThat(testStringDeque).containsExactly("e", "f").inOrder();

    testStringDeque = testStringDeque.copyAndPollFirst();
    assertThat(testStringDeque).containsExactly("f").inOrder();

    testStringDeque = testStringDeque.copyAndAdd("g");
    testStringDeque = testStringDeque.copyAndPollLast();
    assertThat(testStringDeque).containsExactly("f").inOrder();

    testStringDeque = PersistentBalancingDoubleListDeque.of();
    testStringDeque = testStringDeque.copyAndAdd("a");
    testStringDeque = testStringDeque.copyAndAdd("b");
    testStringDeque = testStringDeque.copyAndAdd("a");
    testStringDeque = testStringDeque.copyAndAdd("b");
    testStringDeque = testStringDeque.copyAndAdd("a");
    testStringDeque = testStringDeque.copyAndAdd("b");

    testStringDeque = testStringDeque.copyAndRemoveFirstOccurrence("b");
    assertThat(testStringDeque).containsExactly("a", "a", "b", "a", "b").inOrder();

    testStringDeque = testStringDeque.copyAndRemoveLastOccurrence("a");
    assertThat(testStringDeque).containsExactly("a", "a", "b", "b").inOrder();
  }

  @Test
  public void testAllGetOperationsOnString() {
    final PersistentDeque<String> emptyStringDeque = PersistentBalancingDoubleListDeque.of();
    PersistentDeque<String> testStringDeque = PersistentBalancingDoubleListDeque.of();

    // check correct behaviour (exception vs. returning null) on empty deque
    assertThrows(NoSuchElementException.class, emptyStringDeque::getFirst);
    assertThrows(NoSuchElementException.class, emptyStringDeque::element);
    assertThrows(NoSuchElementException.class, emptyStringDeque::getLast);
    assertThat(emptyStringDeque.peek()).isEqualTo(null);
    assertThat(emptyStringDeque.peekFirst()).isEqualTo(null);
    assertThat(emptyStringDeque.peekLast()).isEqualTo(null);

    // ensures that single element is correctly identified as both head and tail in deque of size 1
    testStringDeque = testStringDeque.copyAndAdd("a");
    assertThat(testStringDeque.getFirst()).isEqualTo("a");
    assertThat(testStringDeque.element()).isEqualTo("a");
    assertThat(testStringDeque.peek()).isEqualTo("a");
    assertThat(testStringDeque.peekFirst()).isEqualTo("a");
    assertThat(testStringDeque.getLast()).isEqualTo("a");
    assertThat(testStringDeque.peekLast()).isEqualTo("a");

    // ensures that head and tail are correctly identified in deque of size > 1
    testStringDeque = testStringDeque.copyAndAdd("b");
    assertThat(testStringDeque.getFirst()).isEqualTo("a");
    assertThat(testStringDeque.element()).isEqualTo("a");
    assertThat(testStringDeque.peek()).isEqualTo("a");
    assertThat(testStringDeque.peekFirst()).isEqualTo("a");
    assertThat(testStringDeque.getLast()).isEqualTo("b");
    assertThat(testStringDeque.peekLast()).isEqualTo("b");
  }

  @Test
  public void testEqualsOnString() {
    PersistentDeque<String> testStringDeque1 = PersistentBalancingDoubleListDeque.of();
    PersistentDeque<String> testStringDeque2 = PersistentBalancingDoubleListDeque.of();

    // test on empty deque
    assertThat(testStringDeque1.equals(testStringDeque2)).isTrue();

    // deques should be equal due to same order despite elements potentially being distributed
    // differently between top and bottom lists
    testStringDeque1 = testStringDeque1.copyAndAddFirst("c");
    testStringDeque1 = testStringDeque1.copyAndAddFirst("b");
    testStringDeque1 = testStringDeque1.copyAndAddFirst("a");

    testStringDeque2 = testStringDeque2.copyAndAddLast("a");
    testStringDeque2 = testStringDeque2.copyAndAddLast("b");
    testStringDeque2 = testStringDeque2.copyAndAddLast("c");

    assertThat(testStringDeque1.equals(testStringDeque2)).isTrue();
  }

  @Test
  public void testEmptyDequeCreationOnInteger() {
    PersistentDeque<Integer> emptyIntegerDeque = PersistentBalancingDoubleListDeque.of();

    assertThat(emptyIntegerDeque.isEmpty()).isTrue();

    assertThat(emptyIntegerDeque.size()).isEqualTo(0);
  }

  @Test
  public void testAllInsertionOperationsOnInteger() {
    PersistentDeque<Integer> testIntegerDeque = PersistentBalancingDoubleListDeque.of();

    testIntegerDeque = testIntegerDeque.copyAndPush(3);
    testIntegerDeque = testIntegerDeque.copyAndAddFirst(2);
    testIntegerDeque = testIntegerDeque.copyAndOfferFirst(1);
    testIntegerDeque = testIntegerDeque.copyAndAdd(4);
    testIntegerDeque = testIntegerDeque.copyAndOffer(5);
    testIntegerDeque = testIntegerDeque.copyAndAddLast(6);
    testIntegerDeque = testIntegerDeque.copyAndOfferLast(7);

    assertThat(testIntegerDeque.size()).isEqualTo(7);
    assertThat(testIntegerDeque).containsExactly(1, 2, 3, 4, 5, 6, 7).inOrder();
  }

  @Test
  public void testAllRemoveOperationsOnInteger() {
    PersistentDeque<Integer> testIntegerDeque = PersistentBalancingDoubleListDeque.of();

    testIntegerDeque = testIntegerDeque.copyAndAdd(1);
    testIntegerDeque = testIntegerDeque.copyAndAdd(2);
    testIntegerDeque = testIntegerDeque.copyAndAdd(3);
    testIntegerDeque = testIntegerDeque.copyAndAdd(4);
    testIntegerDeque = testIntegerDeque.copyAndAdd(5);
    testIntegerDeque = testIntegerDeque.copyAndAdd(6);
    testIntegerDeque = testIntegerDeque.copyAndAdd(7);

    testIntegerDeque = testIntegerDeque.copyAndRemove();
    assertThat(testIntegerDeque).containsExactly(2, 3, 4, 5, 6, 7).inOrder();

    testIntegerDeque = testIntegerDeque.copyAndRemoveFirst();
    assertThat(testIntegerDeque).containsExactly(3, 4, 5, 6, 7).inOrder();

    testIntegerDeque = testIntegerDeque.copyAndRemoveLast();
    assertThat(testIntegerDeque).containsExactly(3, 4, 5, 6).inOrder();

    testIntegerDeque = testIntegerDeque.copyAndPop();
    assertThat(testIntegerDeque).containsExactly(4, 5, 6).inOrder();

    testIntegerDeque = testIntegerDeque.copyAndPoll();
    assertThat(testIntegerDeque).containsExactly(5, 6).inOrder();

    testIntegerDeque = testIntegerDeque.copyAndPollFirst();
    assertThat(testIntegerDeque).containsExactly(6).inOrder();

    testIntegerDeque = testIntegerDeque.copyAndAdd(7);
    testIntegerDeque = testIntegerDeque.copyAndPollLast();
    assertThat(testIntegerDeque).containsExactly(6).inOrder();

    testIntegerDeque = PersistentBalancingDoubleListDeque.of();
    testIntegerDeque = testIntegerDeque.copyAndAdd(1);
    testIntegerDeque = testIntegerDeque.copyAndAdd(2);
    testIntegerDeque = testIntegerDeque.copyAndAdd(1);
    testIntegerDeque = testIntegerDeque.copyAndAdd(2);
    testIntegerDeque = testIntegerDeque.copyAndAdd(1);
    testIntegerDeque = testIntegerDeque.copyAndAdd(2);

    testIntegerDeque = testIntegerDeque.copyAndRemoveFirstOccurrence(2);
    assertThat(testIntegerDeque).containsExactly(1, 1, 2, 1, 2).inOrder();

    testIntegerDeque = testIntegerDeque.copyAndRemoveLastOccurrence(1);
    assertThat(testIntegerDeque).containsExactly(1, 1, 2, 2).inOrder();
  }

  @Test
  public void testAllGetOperationsOnInteger() {
    final PersistentDeque<Integer> emptyIntegerDeque = PersistentBalancingDoubleListDeque.of();
    PersistentDeque<Integer> testIntegerDeque = PersistentBalancingDoubleListDeque.of();

    // check correct behaviour (exception vs. returning null) on empty deque
    assertThrows(NoSuchElementException.class, emptyIntegerDeque::getFirst);
    assertThrows(NoSuchElementException.class, emptyIntegerDeque::element);
    assertThrows(NoSuchElementException.class, emptyIntegerDeque::getLast);
    assertThat(emptyIntegerDeque.peek()).isEqualTo(null);
    assertThat(emptyIntegerDeque.peekFirst()).isEqualTo(null);
    assertThat(emptyIntegerDeque.peekLast()).isEqualTo(null);

    // ensures that single element is correctly identified as both head and tail in deque of size 1
    testIntegerDeque = testIntegerDeque.copyAndAdd(1);
    assertThat(testIntegerDeque.getFirst()).isEqualTo(1);
    assertThat(testIntegerDeque.element()).isEqualTo(1);
    assertThat(testIntegerDeque.peek()).isEqualTo(1);
    assertThat(testIntegerDeque.peekFirst()).isEqualTo(1);
    assertThat(testIntegerDeque.getLast()).isEqualTo(1);
    assertThat(testIntegerDeque.peekLast()).isEqualTo(1);

    // ensures that head and tail are correctly identified in deque of size > 1
    testIntegerDeque = testIntegerDeque.copyAndAdd(2);
    assertThat(testIntegerDeque.getFirst()).isEqualTo(1);
    assertThat(testIntegerDeque.element()).isEqualTo(1);
    assertThat(testIntegerDeque.peek()).isEqualTo(1);
    assertThat(testIntegerDeque.peekFirst()).isEqualTo(1);
    assertThat(testIntegerDeque.getLast()).isEqualTo(2);
    assertThat(testIntegerDeque.peekLast()).isEqualTo(2);
  }

  @Test
  public void testEqualsOnInteger() {
    PersistentDeque<Integer> testIntegerDeque1 = PersistentBalancingDoubleListDeque.of();
    PersistentDeque<Integer> testIntegerDeque2 = PersistentBalancingDoubleListDeque.of();

    // test on empty deque
    assertThat(testIntegerDeque1.equals(testIntegerDeque2)).isTrue();

    // deques should be equal due to same order despite elements potentially being distributed
    // differently between top and bottom lists
    testIntegerDeque1 = testIntegerDeque1.copyAndAddFirst(3);
    testIntegerDeque1 = testIntegerDeque1.copyAndAddFirst(2);
    testIntegerDeque1 = testIntegerDeque1.copyAndAddFirst(1);

    testIntegerDeque2 = testIntegerDeque2.copyAndAddLast(1);
    testIntegerDeque2 = testIntegerDeque2.copyAndAddLast(2);
    testIntegerDeque2 = testIntegerDeque2.copyAndAddLast(3);

    assertThat(testIntegerDeque1.equals(testIntegerDeque2)).isTrue();
  }
}
