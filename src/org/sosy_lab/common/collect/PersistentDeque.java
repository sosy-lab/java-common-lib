// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.annotations.Immutable;
import java.util.Deque;
import java.util.Iterator;

/**
 * Interface for persistent deques. A persistent data structure is immutable, but provides cheap *
 * copy-and-write operations. Thus, all write operations ({@link #insertTop(Object)}, {@link
 * #insertBottom(Object)}, {@link #deleteTop()}, {@link #deleteBottom()}) will not modify the
 * current instance, but return a new instance instead.
 *
 * @param <T> type of elements stored in deque
 */
@Immutable(containerOf = "T")
public interface PersistentDeque<T> extends Deque<T> {

  /**
   * Provides an iterator over all objects in the deque in reverse sequential order (from last to
   * first).
   *
   * @return iterator over all objects in deque in reverse sequential order
   */
  Iterator<T> descendingIterator();

  /**
   * Provides an iterator over all objects in the deque in proper sequence (from first to last).
   *
   * @return iterator over all objects in deque in proper sequence
   */
  Iterator<T> iterator();

  /**
   * Calculates the no. of elements in the deque.
   *
   * @return no. of elements in deque
   */
  int size();

  /**
   * Retrieves element at top of deque.
   *
   * @return element at top of deque
   */
  T element();

  /**
   * Searches the deque for the provided object and returns true if it is found in the deque, false
   * if not.
   *
   * @param o object to be searched for in the deque
   * @return true if o in deque, false if not
   */
  boolean contains(Object o);

  /**
   * Returns true if deque is empty, false if not.
   *
   * @return true if deque empty, else false
   */
  boolean isEmpty();

  /**
   * Retrieves element at top of deque.
   *
   * @return element at top of deque
   */
  T getFirst();

  /**
   * Retrieves element at bottom of deque.
   *
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
   * Inserts element at top of deque.
   *
   * @param value element to be inserted
   * @return deque instance with new element at top of deque
   */
  PersistentBalancingDoubleListDeque<T> insertTop(T value);

  /**
   * Inserts element at bottom of deque.
   *
   * @param value element to be inserted
   * @return deque instance with new element at bottom of deque
   */
  PersistentBalancingDoubleListDeque<T> insertBottom(T value);

  /**
   * Deletes element currently at top of deque.
   *
   * @return deque instance after top element has been removed
   */
  PersistentBalancingDoubleListDeque<T> deleteTop();

  /**
   * Deletes element currently at bottom of deque.
   *
   * @return deque instance after bottom element has been removed
   */
  PersistentBalancingDoubleListDeque<T> deleteBottom();

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
}
