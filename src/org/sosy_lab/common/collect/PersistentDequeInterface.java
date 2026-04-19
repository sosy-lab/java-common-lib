// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.errorprone.annotations.Immutable;

/**
 * Interface for persistent deques. A persistent data structure is immutable, but provides cheap *
 * copy-and-write operations. Thus, all write operations ({@link #insertTop(Object)}, {@link
 * #insertBottom(Object)}, {@link #deleteTop()}, {@link #deleteBottom()}) will not modify the
 * current instance, but return a new instance instead.
 *
 * @param <T> type of elements stored in deque
 */
@Immutable(containerOf = "T")
public interface PersistentDequeInterface<T> {

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
  T getTop();

  /**
   * Retrieves element at bottom of deque.
   *
   * @return element at bottom of deque
   */
  T getBottom();

  /**
   * Inserts element at top of deque.
   *
   * @param value element to be inserted
   * @return deque instance with new element at top of deque
   */
  PersistentDeque<T> insertTop(T value);

  /**
   * Inserts element at bottom of deque.
   *
   * @param value element to be inserted
   * @return deque instance with new element at bottom of deque
   */
  PersistentDeque<T> insertBottom(T value);

  /**
   * Deletes element currently at top of deque.
   *
   * @return deque instance after top element has been removed
   */
  PersistentDeque<T> deleteTop();

  /**
   * Deletes element currently at bottom of deque.
   *
   * @return deque instance after bottom element has been removed
   */
  PersistentDeque<T> deleteBottom();
}
