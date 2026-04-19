// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.Var;
import java.util.Iterator;
import javax.annotation.Nullable;

/**
 * A persistent implementation of a deque on the basis of {@link PersistentLinkedList}.
 *
 * <p>To avoid O(n) runtime complexity when accessing the bottom of the deque, two separate {@link
 * PersistentLinkedList}s are used. {@code top} represents the top part of the deque, while {@code
 * bottom} forms the lower part of the deque. If one were to reverse {@code bottom} and then add it
 * to the bottom of {@code top}, one would receive one list representing the whole deque in correct
 * order.
 *
 * <p>It provides operations to show the top- and bottom-most elements of the deque, as well as ones
 * to remove them or add new items to the deque in either places. In most cases, these will complete
 * in O(1). Occasionally, these operations will require more time, as the deque might need to be
 * rebalanced (i.e. when one of the lists becomes empty, the other list is split up into top and
 * bottom to further guarantee access to both ends of the deque).
 *
 * <p>Currently, it is only possible to create an empty deque and then add new elements one at a
 * time.
 *
 * @param <T> type of elements to be stored in deque
 */
@Immutable(containerOf = "T")
public final class PersistentDeque<T> implements PersistentDequeInterface<T> {
  final PersistentLinkedList<T> top;
  final PersistentLinkedList<T> bottom;

  public PersistentDeque() {
    top = PersistentLinkedList.of();
    bottom = PersistentLinkedList.of();
  }

  private PersistentDeque(PersistentLinkedList<T> top, PersistentLinkedList<T> bottom) {
    this.top = top;
    this.bottom = bottom;
  }

  /**
   * Checks both sublists and returns true if both are empty, false if at least one is not.
   *
   * @return true if {@code top} and {@code bottom} are both empty, false if at least one is not
   */
  @Override
  public boolean isEmpty() {
    return top.isEmpty() && bottom.isEmpty();
  }

  /**
   * Returns element at the top of the deque.
   *
   * @return element at top of deque; null if deque empty
   */
  @Nullable
  @Override
  public T getTop() {
    // If deque contains only one element, one of the lists will be empty. Calling head() on an
    // empty list throws an exception, so this needs to be caught. Further, the one element in
    // the non-empty list should be returned, as it is both head and tail of the deque.
    try {
      return top.head();
    } catch (IllegalStateException e1) {
      try {
        return bottom.head();
      } catch (IllegalStateException e2) {
        return null;
      }
    }
  }

  /**
   * Returns element at the bottom of the deque.
   *
   * @return element at bottom of deque; null if deque empty
   */
  @Nullable
  @Override
  public T getBottom() {
    // If deque contains only one element, one of the lists will be empty. Calling head() on an
    // empty list throws an exception, so this needs to be caught. Further, the one element in
    // the non-empty list should be returned, as it is both head and tail of the deque.
    try {
      return bottom.head();
    } catch (IllegalStateException e1) {
      try {
        return top.head();
      } catch (IllegalStateException e2) {
        return null;
      }
    }
  }

  /**
   * Places new element on top of the deque.
   *
   * @param value element to be added to deque
   * @return deque instance with new element added on top
   */
  @Override
  public PersistentDeque<T> insertTop(T value) {
    return new PersistentDeque<>(top.with(value), bottom);
  }

  /**
   * Places new element at the bottom of the deque.
   *
   * @param value element to be added to deque
   * @return deque instance with new element added at the bottom
   */
  @Override
  public PersistentDeque<T> insertBottom(T value) {
    return new PersistentDeque<>(top, bottom.with(value));
  }

  /**
   * Removes element at the top of the deque from deque.
   *
   * @return deque instance after top element has been removed
   */
  @Override
  public PersistentDeque<T> deleteTop() {
    return new PersistentDeque<>(top.tail(), bottom).rebalanceDeque();
  }

  /**
   * Removes element at the bottom of the deque from deque.
   *
   * @return deque instance after bottom element has been removed
   */
  @Override
  public PersistentDeque<T> deleteBottom() {
    return new PersistentDeque<>(top, bottom.tail()).rebalanceDeque();
  }

  private PersistentDeque<T> rebalanceDeque() {
    boolean topEmpty = top.isEmpty();
    boolean bottomEmpty = bottom.isEmpty();

    if (topEmpty && bottomEmpty) {
      return this;
    } else if (topEmpty && !bottomEmpty) {
      return split(bottom.reversed());
    } else if (!topEmpty && bottomEmpty) {
      return split(top);
    }

    return this;
  }

  private PersistentDeque<T> split(PersistentLinkedList<T> list) {
    int size = list.size();
    int halfSize = size / 2;

    if (size <= 0) {
      throw new IllegalArgumentException("Cannot split empty list!");
    } else if (size == 1) {
      return this;
    }

    @Var PersistentLinkedList<T> newTop = PersistentLinkedList.of();
    @Var PersistentLinkedList<T> newBottom = PersistentLinkedList.of();
    Iterator<T> iterator = list.iterator();

    for (int i = 0; i < size; i++) {
      T element = iterator.next();
      if (i < halfSize) {
        newTop = newTop.with(element);
      } else {
        newBottom = newBottom.with(element);
      }
    }
    newTop = newTop.reversed();
    return new PersistentDeque<>(newTop, newBottom);
  }
}
