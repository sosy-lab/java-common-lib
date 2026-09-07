// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import com.google.errorprone.annotations.Var;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.Test;

public class PersistentLinkedStackTest {

  @Test
  public void testEmptyFactory() {
    PersistentStack<String> stack = PersistentLinkedStack.of();

    assertThat(stack.isEmpty()).isTrue();
  }

  @Test
  public void testSingletonFactory() {
    PersistentStack<String> stack = PersistentLinkedStack.of("value");

    assertThat(stack.isEmpty()).isFalse();
    assertThat(stack).containsExactly("value");
  }

  @Test
  public void testPushAndCopy() {
    PersistentStack<String> empty = PersistentLinkedStack.of();
    PersistentStack<String> stack = empty.pushAndCopy("value");

    assertThat(stack.peek()).isEqualTo("value");
    assertThat(empty).isEmpty();
  }

  @Test
  public void testDuplicateEmptyStrings() {
    PersistentStack<String> stack =
        PersistentLinkedStack.<String>of().pushAndCopy("").pushAndCopy("");

    assertThat(stack.size()).isEqualTo(2);
    assertThat(stack.peek()).isEmpty();
    PersistentStack<String> popped = stack.popAndCopy();
    assertThat(popped.size()).isEqualTo(1);
    assertThat(popped.peek()).isEqualTo(stack.peek());
  }

  @Test
  public void testIntegerValues() {
    PersistentStack<Integer> stack =
        PersistentLinkedStack.<Integer>of().pushAndCopy(1).pushAndCopy(2);

    assertThat(stack.size()).isEqualTo(2);
    assertThat(stack.peek()).isEqualTo(2);
    PersistentStack<Integer> popped = stack.popAndCopy();
    assertThat(popped.size()).isEqualTo(1);
    assertThat(popped.peek()).isEqualTo(1);
  }

  @Test
  public void testBigIntegerValueEqualityAndIdentity() {
    BigInteger sharedValue = new BigInteger("123456789012345678901234567890");
    BigInteger equalValue = new BigInteger("123456789012345678901234567890");
    PersistentStack<BigInteger> first = PersistentLinkedStack.of(sharedValue);
    PersistentStack<BigInteger> sameReference = PersistentLinkedStack.of(sharedValue);
    PersistentStack<BigInteger> equalReference = PersistentLinkedStack.of(equalValue);

    assertThat(first).isNotSameInstanceAs(sameReference);
    assertThat(first).isEqualTo(sameReference);
    assertThat(first.peek()).isSameInstanceAs(sharedValue);
    assertThat(sameReference.peek()).isSameInstanceAs(sharedValue);

    assertThat(equalValue).isNotSameInstanceAs(sharedValue);
    assertThat(equalValue).isEqualTo(sharedValue);
    assertThat(first).isEqualTo(equalReference);
    assertThat(equalReference.peek()).isSameInstanceAs(equalValue);
    assertThat(equalReference.peek()).isNotSameInstanceAs(sharedValue);
  }

  @Test
  public void testPopReturnsSamePredecessor() {
    PersistentStack<String> predecessor =
        PersistentLinkedStack.<String>of().pushAndCopy("bottom").pushAndCopy("middle");
    PersistentStack<String> stack = predecessor.pushAndCopy("top");

    assertThat(stack.popAndCopy()).isSameInstanceAs(predecessor);
  }

  @Test
  public void testPeekEmptyThrows() {
    PersistentStack<String> empty = PersistentLinkedStack.of();

    assertThrows(NoSuchElementException.class, empty::peek);
  }

  @Test
  public void testPopEmptyThrows() {
    PersistentStack<String> empty = PersistentLinkedStack.of();

    assertThrows(NoSuchElementException.class, empty::popAndCopy);
  }

  @Test
  public void testSizeAcrossPersistentVersions() {
    PersistentStack<String> empty = PersistentLinkedStack.of();
    PersistentStack<String> one = empty.pushAndCopy("one");
    PersistentStack<String> two = one.pushAndCopy("two");

    assertThat(empty.size()).isEqualTo(0);
    assertThat(one.size()).isEqualTo(1);
    assertThat(two.size()).isEqualTo(2);
    assertThat(two.popAndCopy().size()).isEqualTo(1);
    assertThat(empty.size()).isEqualTo(0);
    assertThat(one.size()).isEqualTo(1);
    assertThat(two.size()).isEqualTo(2);
  }

  @Test
  public void testCanonicalEmpty() {
    PersistentStack<String> empty = PersistentLinkedStack.of();
    PersistentStack<String> singleton = PersistentLinkedStack.of("value");

    assertThat(PersistentLinkedStack.<String>of()).isSameInstanceAs(empty);
    assertThat(singleton.empty()).isSameInstanceAs(empty);
    assertThat(singleton.popAndCopy()).isSameInstanceAs(empty);
  }

  @Test
  public void testRejectsNull() {
    PersistentStack<String> empty = PersistentLinkedStack.of();

    assertThrows(NullPointerException.class, () -> PersistentLinkedStack.of((String) null));
    assertThrows(NullPointerException.class, () -> empty.pushAndCopy(null));
    assertThrows(
        NullPointerException.class, () -> PersistentLinkedStack.of("value").pushAndCopy(null));
  }

  @Test
  public void testIteratorOrderIsTopToBottom() {
    PersistentStack<String> stack =
        PersistentLinkedStack.<String>of()
            .pushAndCopy("bottom")
            .pushAndCopy("middle")
            .pushAndCopy("top");

    assertThat(stack).containsExactly("top", "middle", "bottom").inOrder();
  }

  @Test
  public void testIteratorExhaustion() {
    Iterator<String> iterator = PersistentLinkedStack.of("value").iterator();

    assertThat(iterator.next()).isEqualTo("value");
    assertThrows(NoSuchElementException.class, iterator::next);
  }

  @Test
  public void testIteratorRemoveRejected() {
    Iterator<String> iterator = PersistentLinkedStack.of("value").iterator();

    assertThrows(UnsupportedOperationException.class, iterator::remove);
  }

  @Test
  public void testEquality() {
    PersistentStack<String> stack =
        PersistentLinkedStack.of("bottom").pushAndCopy("middle").pushAndCopy("top");
    PersistentStack<String> independentlyBuilt =
        PersistentLinkedStack.<String>of()
            .pushAndCopy("bottom")
            .pushAndCopy("middle")
            .pushAndCopy("top");
    PersistentStack<String> differentOrder =
        PersistentLinkedStack.of("top").pushAndCopy("middle").pushAndCopy("bottom");
    PersistentStack<String> differentMiddle =
        PersistentLinkedStack.of("bottom").pushAndCopy("other").pushAndCopy("top");
    PersistentStack<String> differentBottom =
        PersistentLinkedStack.of("other").pushAndCopy("middle").pushAndCopy("top");
    PersistentStack<String> shorter = PersistentLinkedStack.of("middle").pushAndCopy("top");

    new EqualsTester()
        .addEqualityGroup(stack, independentlyBuilt)
        .addEqualityGroup(differentOrder)
        .addEqualityGroup(differentMiddle)
        .addEqualityGroup(differentBottom)
        .addEqualityGroup(shorter)
        .testEquals();
  }

  @Test
  public void testEqualityWithSharedTail() {
    PersistentLinkedStack<String> sharedTail =
        PersistentLinkedStack.of("bottom").pushAndCopy("shared");
    PersistentStack<String> stack = sharedTail.pushAndCopy("middle").pushAndCopy("top");
    PersistentStack<String> equal = sharedTail.pushAndCopy("middle").pushAndCopy("top");
    PersistentStack<String> different = sharedTail.pushAndCopy("other").pushAndCopy("top");

    new EqualsTester().addEqualityGroup(stack, equal).addEqualityGroup(different).testEquals();
  }

  @Test
  public void testToString() {
    PersistentStack<String> empty = PersistentLinkedStack.of();
    PersistentStack<String> stack =
        PersistentLinkedStack.of("bottom").pushAndCopy("middle").pushAndCopy("top");

    assertThat(empty.toString()).isEqualTo("[]");
    assertThat(stack.toString()).isEqualTo("[top, middle, bottom]");
  }

  @Test
  public void testSerializationRoundTrip() {
    PersistentStack<String> stack =
        PersistentLinkedStack.of("bottom").pushAndCopy("middle").pushAndCopy("top");

    SerializableTester.reserializeAndAssert(stack);
  }

  @Test
  public void testEmptySerializationReturnsCanonicalInstance() {
    PersistentStack<String> empty = PersistentLinkedStack.of();

    assertThat(SerializableTester.reserialize(empty)).isSameInstanceAs(empty);
  }

  @Test
  public void testLongStackSerializationRoundTrip() {
    int length = 10_000;
    @Var PersistentStack<Integer> stack = PersistentLinkedStack.of();
    for (int i = 0; i < length; i++) {
      stack = stack.pushAndCopy(i);
    }

    assertThat(SerializableTester.reserialize(stack)).isEqualTo(stack);
  }
}
