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
import java.util.NoSuchElementException;
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
public final class PersistentBalancingDoubleListDeque<T> extends AbstractImmutableDeque<T>
    implements PersistentDeque<T> {
  final PersistentLinkedList<T> top;
  final PersistentLinkedList<T> bottom;

  public PersistentBalancingDoubleListDeque() {
    top = PersistentLinkedList.of();
    bottom = PersistentLinkedList.of();
  }

  private PersistentBalancingDoubleListDeque(
      PersistentLinkedList<T> top, PersistentLinkedList<T> bottom) {
    this.top = top;
    this.bottom = bottom;
  }

  /**
   * Creates a new deque instance that is empty.
   *
   * @return new, empty deque instance
   */
  @SuppressWarnings({"unchecked"})
  @Override
  public PersistentBalancingDoubleListDeque<T> empty() {
    return of();
  }

  /**
   * Creates a new deque instance that is empty.
   *
   * @return new, empty deque instance
   */
  @SuppressWarnings("rawtypes")
  public static PersistentBalancingDoubleListDeque of() {
    return new PersistentBalancingDoubleListDeque();
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
   * Retrieves element at top of deque or returns null in the case of an empty deque.
   *
   * @return element at top of deque; null if deque empty
   */
  public T peek() {
    return peekFirst();
  }

  /**
   * Returns element at the top of the deque.
   *
   * @return element at top of deque; null if deque empty
   */
  @Nullable
  @Override
  public T peekFirst() {
    // If deque contains only one element, one of the lists will be empty. Calling head() on an
    // empty list throws an exception, which is why we check first with isEmpty(). Further, the one
    // element in
    // the non-empty list should be returned, as it is both head and tail of the deque.

    if (!top.isEmpty()) {
      return top.head();
    } else if (!bottom.isEmpty()) {
      return bottom.head();
    }

    return null;
  }

  /**
   * Returns element at the bottom of the deque.
   *
   * @return element at bottom of deque; null if deque empty
   */
  @Nullable
  @Override
  public T peekLast() {
    // If deque contains only one element, one of the lists will be empty. Calling head() on an
    // empty list throws an exception, which is why we check first with isEmpty(). Further, the one
    // element in
    // the non-empty list should be returned, as it is both head and tail of the deque.

    if (!bottom.isEmpty()) {
      return bottom.head();
    } else if (!top.isEmpty()) {
      return top.head();
    }

    return null;
  }

  /**
   * Returns a copy of the deque to which the provided element has been added in last place.
   *
   * @return copy of existing deque extended by new element in last place
   */
  @Override
  public PersistentBalancingDoubleListDeque<T> copyAndAdd(T value) {
    return copyAndAddLast(value);
  }

  /**
   * Places new element on top of the deque.
   *
   * @param value element to be added to deque
   * @return deque instance with new element added on top
   */
  @Override
  public PersistentBalancingDoubleListDeque<T> copyAndAddFirst(T value) {
    return new PersistentBalancingDoubleListDeque<>(top.with(value), bottom);
  }

  /**
   * Places new element at the bottom of the deque.
   *
   * @param value element to be added to deque
   * @return deque instance with new element added at the bottom
   */
  @Override
  public PersistentBalancingDoubleListDeque<T> copyAndAddLast(T value) {
    return new PersistentBalancingDoubleListDeque<>(top, bottom.with(value));
  }

  /**
   * Returns a copy of the deque to which the provided element has been added in last place.
   *
   * @return copy of existing deque extended by new element in last place
   */
  @Override
  public PersistentBalancingDoubleListDeque<T> copyAndOffer(T value) {
    return copyAndOfferLast(value);
  }

  /**
   * Returns a copy of the deque to which the provided element has been added in first place.
   *
   * @return copy of existing deque extended by new element in first place
   */
  @Override
  public PersistentBalancingDoubleListDeque<T> copyAndOfferFirst(T value) {
    return new PersistentBalancingDoubleListDeque<>(top.with(value), bottom);
  }

  /**
   * Returns a copy of the deque to which the provided element has been added in last place.
   *
   * @return copy of existing deque extended by new element in last place
   */
  @Override
  public PersistentBalancingDoubleListDeque<T> copyAndOfferLast(T value) {
    return new PersistentBalancingDoubleListDeque<>(top, bottom.with(value));
  }

  /**
   * Returns a copy of this deque from which the first element has been removed.
   *
   * @throws NoSuchElementException if deque empty
   * @return copy of this deque from which the first element has been removed
   */
  @Override
  public PersistentBalancingDoubleListDeque<T> copyAndRemove() {
    return copyAndRemoveFirst();
  }

  /**
   * Returns a copy of this deque from which the first element has been removed.
   *
   * @throws NoSuchElementException if deque empty
   * @return copy of this deque from which the first element has been removed
   */
  @Override
  public PersistentBalancingDoubleListDeque<T> copyAndRemoveFirst() {
    if (this.isEmpty()) {
      throw new NoSuchElementException("Deque is empty!");
    }
    // top should only ever be empty if only one element in bottom or deque completely empty
    if (top.isEmpty()) {
      return new PersistentBalancingDoubleListDeque<>(top, bottom.tail()).rebalanceDeque();
    }
    return new PersistentBalancingDoubleListDeque<>(top.tail(), bottom).rebalanceDeque();
  }

  /**
   * Returns a copy of this deque from which the last element has been removed.
   *
   * @throws NoSuchElementException if deque empty
   * @return copy of this deque from which the last element has been removed
   */
  @Override
  public PersistentBalancingDoubleListDeque<T> copyAndRemoveLast() {
    if (this.isEmpty()) {
      throw new NoSuchElementException("Deque is empty!");
    }
    // bottom should only ever be empty if only one element in top or deque completely empty
    if (bottom.isEmpty()) {
      return new PersistentBalancingDoubleListDeque<>(top.tail(), bottom).rebalanceDeque();
    }
    return new PersistentBalancingDoubleListDeque<>(top, bottom.tail()).rebalanceDeque();
  }

  /**
   * Returns a copy of this deque from which the first element has been removed or null if the deque
   * is empty.
   *
   * @return copy of this deque from which the first element has been removed, null if deque empty
   */
  @Override
  @Nullable
  public PersistentBalancingDoubleListDeque<T> copyAndPoll() {
    return copyAndPollFirst();
  }

  /**
   * Returns a copy of this deque from which the first element has been removed or null if the deque
   * is empty.
   *
   * @return copy of this deque from which the first element has been removed, null if deque empty
   */
  @Override
  @Nullable
  public PersistentBalancingDoubleListDeque<T> copyAndPollFirst() {
    if (isEmpty()) {
      return null;
    }
    // top should only ever be empty if only one element in bottom or deque completely empty
    if (top.isEmpty()) {
      return new PersistentBalancingDoubleListDeque<>(top, bottom.tail()).rebalanceDeque();
    }
    return new PersistentBalancingDoubleListDeque<>(top.tail(), bottom).rebalanceDeque();
  }

  /**
   * Returns a copy of this deque from which the last element has been removed or null if the deque
   * is empty.
   *
   * @return copy of this deque from which the last element has been removed, null if deque empty
   */
  @Override
  @Nullable
  public PersistentBalancingDoubleListDeque<T> copyAndPollLast() {
    if (isEmpty()) {
      return null;
    }
    // bottom should only ever be empty if only one element in top or deque completely empty
    if (bottom.isEmpty()) {
      return new PersistentBalancingDoubleListDeque<>(top.tail(), bottom).rebalanceDeque();
    }
    return new PersistentBalancingDoubleListDeque<>(top, bottom.tail()).rebalanceDeque();
  }

  /**
   * Retrieves element at top of deque.
   *
   * @throws NoSuchElementException if deque empty
   * @return element at top of deque
   */
  @Override
  public T getFirst() {
    if (this.isEmpty()) {
      throw new NoSuchElementException("Deque is empty!");
    }

    return peekFirst();
  }

  /**
   * Retrieves element at bottom of deque.
   *
   * @throws NoSuchElementException if deque empty
   * @return element at bottom of deque
   */
  @Override
  public T getLast() {
    if (this.isEmpty()) {
      throw new NoSuchElementException("Deque is empty!");
    }

    return peekLast();
  }

  /**
   * Returns a copy of this deque from which the first occurrence of the provided element has been
   * removed.
   *
   * @return copy of this deque from which the first occurrence of the provided element has been
   *     removed
   */
  @Override
  public PersistentBalancingDoubleListDeque<T> copyAndRemove(T value) {
    return copyAndRemoveFirstOccurrence(value);
  }

  /**
   * Returns a copy of this deque from which the first occurrence of the provided element has been
   * removed.
   *
   * @return copy of this deque from which the first occurrence of the provided element has been
   *     removed
   */
  @Override
  public PersistentBalancingDoubleListDeque<T> copyAndRemoveFirstOccurrence(T value) {
    // afaik without() will remove the first occurrence of the object
    if (top.contains(value)) {
      return new PersistentBalancingDoubleListDeque<>(top.without(value), bottom);
    } else {
      return new PersistentBalancingDoubleListDeque<>(top, bottom.without(value));
    }
  }

  /**
   * Returns a copy of this deque from which the last occurrence of the provided element has been
   * removed.
   *
   * @return copy of this deque from which the last occurrence of the provided element has been
   *     removed
   */
  @Override
  public PersistentBalancingDoubleListDeque<T> copyAndRemoveLastOccurrence(T value) {
    // reverse deque; remove first occurrence; reverse again to have original order without object
    PersistentBalancingDoubleListDeque<T> reversed = this.reversed();
    reversed = reversed.copyAndRemoveFirstOccurrence(value);

    return reversed.reversed();
  }

  private PersistentBalancingDoubleListDeque<T> reversed() {
    return new PersistentBalancingDoubleListDeque<>(bottom, top);
  }

  /**
   * Retrieves element at top of deque.
   *
   * @throws NoSuchElementException if deque empty
   * @return element at top of deque
   */
  public T element() {
    return getFirst();
  }

  /**
   * Returns a copy of this deque to which the provided object has been pushed onto the stack
   * represented by this deque.
   *
   * @throws NullPointerException if provided object is null
   * @return copy of this deque to which the provided element has been added in first place
   */
  @Override
  public PersistentBalancingDoubleListDeque<T> copyAndPush(T value) {
    return copyAndAddFirst(value);
  }

  /**
   * Returns a copy of this deque from which the first element has been removed.
   *
   * @throws NoSuchElementException if deque empty
   * @return copy of this deque from which the first element has been removed
   */
  @Override
  public PersistentBalancingDoubleListDeque<T> copyAndPop() {
    return copyAndRemoveFirst();
  }

  /**
   * Searches the deque for the provided object and returns true if it is found in the deque, false
   * if not.
   *
   * @param o object to be searched for in the deque
   * @return true if o in deque, false if not
   */
  public boolean contains(Object o) {
    return (top.contains(o) | bottom.contains(o));
  }

  /**
   * Calculates the no. of elements in the deque.
   *
   * @return no. of elements in deque
   */
  public int size() {
    return top.size() + bottom.size();
  }

  /**
   * Provides an iterator over all objects in the deque in proper sequence (from first to last).
   *
   * @return iterator over all objects in deque in proper sequence
   */
  public Iterator<T> iterator() {
    // TODO
  }

  /**
   * Provides an iterator over all objects in the deque in reverse sequential order (from last to
   * first).
   *
   * @return iterator over all objects in deque in reverse sequential order
   */
  public Iterator<T> descendingIterator() {
    // TODO
  }

  /**
   * Due to the two-list structure of this deque, adding and removing elements can quickly lead to
   * one of the lists being empty while the other still contains multiple elements. To prevent this,
   * this method is called after each remove or insert operation to even out the two lists if
   * necessary. If one list is empty while the other contains at least two elements, the elements in
   * the latter will be redistributed so both lists are (almost) the same size while still
   * maintaining their original order (this is what split() does).
   *
   * @return deque with the same elements in the same order, but evenly distributed between both the
   *     top and bottom list
   */
  private PersistentBalancingDoubleListDeque<T> rebalanceDeque() {
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

  private PersistentBalancingDoubleListDeque<T> split(PersistentLinkedList<T> list) {
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
    return new PersistentBalancingDoubleListDeque<>(newTop, newBottom);
  }
}
