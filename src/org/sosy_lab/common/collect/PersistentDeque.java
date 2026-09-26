// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.annotations.Immutable;
import java.util.Collection;
import java.util.Deque;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;

/**
 * Interface for persistent deques. A persistent data structure is immutable, but provides cheap *
 * copy-and-write operations. Thus, all write operations will not modify the current instance, but
 * return a new instance instead.
 *
 * @param <T> type of elements stored in deque
 */
@Immutable(containerOf = "T")
public interface PersistentDeque<T> extends Deque<T> {

  /**
   * Creates a new deque instance that is empty.
   *
   * @return new, empty deque instance
   */
  PersistentDeque<T> empty();

  /**
   * Returns a copy of the deque to which the provided element has been added in last place.
   * Replacement for {@link #add(Object)}.
   *
   * @return copy of existing deque extended by new element in last place
   */
  @CheckReturnValue
  PersistentDeque<T> copyAndAdd(T t);

  /**
   * Returns a copy of the deque to which the provided element has been added in first place.
   * Replacement for {@link #addFirst(Object)}.
   *
   * @return copy of existing deque extended by new element in first place
   */
  @CheckReturnValue
  PersistentDeque<T> copyAndAddFirst(T t);

  /**
   * Returns a copy of the deque to which the provided element has been added in last place.
   * Replacement for {@link #addLast(Object)}.
   *
   * @return copy of existing deque extended by new element in last place
   */
  @CheckReturnValue
  PersistentDeque<T> copyAndAddLast(T t);

  /**
   * Returns a copy of the deque to which the provided element has been added in last place.
   * Replacement for {@link #offer(Object)}.
   *
   * @return copy of existing deque extended by new element in last place
   */
  @CheckReturnValue
  PersistentDeque<T> copyAndOffer(T t);

  /**
   * Returns a copy of the deque to which the provided element has been added in first place.
   * Replacement for {@link #offerFirst(Object)}.
   *
   * @return copy of existing deque extended by new element in first place
   */
  @CheckReturnValue
  PersistentDeque<T> copyAndOfferFirst(T t);

  /**
   * Returns a copy of the deque to which the provided element has been added in last place.
   * Replacement for {@link #offerLast(Object)}.
   *
   * @return copy of existing deque extended by new element in last place
   */
  @CheckReturnValue
  PersistentDeque<T> copyAndOfferLast(T t);

  /**
   * Returns a copy of this deque from which the first element has been removed or null if the deque
   * is empty. Replacement for {@link #poll()}.
   *
   * @return copy of this deque from which the first element has been removed, null if deque empty
   */
  @CheckReturnValue
  @Nullable
  PersistentDeque<T> copyAndPoll();

  /**
   * Returns a copy of this deque from which the first element has been removed or null if the deque
   * is empty. Replacement for {@link #pollFirst()}.
   *
   * @return copy of this deque from which the first element has been removed, null if deque empty
   */
  @CheckReturnValue
  @Nullable
  PersistentDeque<T> copyAndPollFirst();

  /**
   * Returns a copy of this deque from which the last element has been removed or null if the deque
   * is empty. Replacement for {@link #pollLast()}.
   *
   * @return copy of this deque from which the last element has been removed, null if deque empty
   */
  @CheckReturnValue
  @Nullable
  PersistentDeque<T> copyAndPollLast();

  /**
   * Returns a copy of this deque from which the first element has been removed. Replacement for
   * {@link #pop()}.
   *
   * @throws NoSuchElementException if deque empty
   * @return copy of this deque from which the first element has been removed
   */
  @CheckReturnValue
  PersistentDeque<T> copyAndPop();

  /**
   * Returns a copy of this deque to which the provided object has been pushed onto the stack
   * represented by this deque. Replacement for {@link #push(Object)}.
   *
   * @throws NullPointerException if provided object is null
   * @return copy of this deque to which the provided element has been added in first place
   */
  @CheckReturnValue
  PersistentDeque<T> copyAndPush(T t);

  /**
   * Returns a copy of this deque from which the first element has been removed. Replacement for
   * {@link #remove()}.
   *
   * @throws NoSuchElementException if deque empty
   * @return copy of this deque from which the first element has been removed
   */
  @CheckReturnValue
  PersistentDeque<T> copyAndRemove();

  /**
   * Returns a copy of this deque from which the first occurrence of the provided element has been
   * removed. Replacement for {@link #remove(Object)}.
   *
   * @return copy of this deque from which the first occurrence of the provided element has been
   *     removed
   */
  @CheckReturnValue
  PersistentDeque<T> copyAndRemove(T t);

  /**
   * Returns a copy of this deque from which the first element has been removed. Replacement for
   * {@link #removeFirst()}.
   *
   * @throws NoSuchElementException if deque empty
   * @return copy of this deque from which the first element has been removed
   */
  @CheckReturnValue
  PersistentDeque<T> copyAndRemoveFirst();

  /**
   * Returns a copy of this deque from which the first occurrence of the provided element has been
   * removed. Replacement for {@link #removeFirstOccurrence(Object)}.
   *
   * @return copy of this deque from which the first occurrence of the provided element has been
   *     removed
   */
  @CheckReturnValue
  PersistentDeque<T> copyAndRemoveFirstOccurrence(T t);

  /**
   * Returns a copy of this deque from which the last element has been removed. Replacement for
   * {@link #removeLast()}.
   *
   * @throws NoSuchElementException if deque empty
   * @return copy of this deque from which the last element has been removed
   */
  @CheckReturnValue
  PersistentDeque<T> copyAndRemoveLast();

  /**
   * Returns a copy of this deque from which the last occurrence of the provided element has been
   * removed. Replacement for {@link #removeLastOccurrence(Object)}.
   *
   * @return copy of this deque from which the last occurrence of the provided element has been
   *     removed
   */
  @CheckReturnValue
  PersistentDeque<T> copyAndRemoveLastOccurrence(T t);

  /**
   * Retrieves element at top of deque.
   *
   * @throws NoSuchElementException if deque empty
   * @return element at top of deque
   */
  T element();

  /**
   * Retrieves element at top of deque.
   *
   * @throws NoSuchElementException if deque empty
   * @return element at top of deque
   */
  T getFirst();

  /**
   * Retrieves element at bottom of deque.
   *
   * @throws NoSuchElementException if deque empty
   * @return element at bottom of deque
   */
  T getLast();

  /**
   * Retrieves element at top of deque or returns null in the case of an empty deque.
   *
   * @return element at top of deque; null if deque empty
   */
  T peek();

  /**
   * Retrieves element at top of deque or returns null in the case of an empty deque.
   *
   * @return element at top of deque; null if deque empty
   */
  T peekFirst();

  /**
   * Retrieves element at bottom of deque or returns null in the case of an empty deque.
   *
   * @return element at bottom of deque; null if deque empty
   */
  T peekLast();

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  boolean add(T t);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  void addFirst(T t);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  void addLast(T t);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  boolean offer(T t);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  boolean offerFirst(T t);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  boolean offerLast(T t);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  T poll();

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  T pollFirst();

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  T pollLast();

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  T pop();

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  void push(T t);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  T remove();

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  boolean remove(Object o);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  T removeFirst();

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  boolean removeFirstOccurrence(Object o);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  T removeLast();

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  boolean removeLastOccurrence(Object o);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  boolean addAll(Collection<? extends T> c);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  void clear();

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  boolean containsAll(Collection<?> c);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  boolean removeAll(Collection<?> c);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  boolean retainAll(Collection<?> c);
}
