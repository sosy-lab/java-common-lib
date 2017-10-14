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
package org.sosy_lab.common.collect;

import java.util.NavigableSet;
import java.util.SortedSet;

/**
 * A {@link SortedSet} that allows two additional operations: receiving (and deleting) an element by
 * its <i>rank</i>, and getting the rank of an element.
 *
 * <p>Implementations should adhere to all contracts of the <code>SortedSet</code> interface.
 *
 * <p>Implementing classes should provide two means for comparing elements:
 *
 * <ol>
 *   <li>Using the natural ordering of the elements. In this case, all elements of the set have to
 *       implement the {@link Comparable} interface.
 *   <li>Using a {@link java.util.Comparator Comparator} to create an order over the elements of the
 *       set.
 * </ol>
 *
 * In both cases, the used compare-method should be consistent with <code>equals</code>, i.e.,
 * <code>compare(a, b) == 0  =&gt;  a.equals(b)</code>, so that the contract provided by {@link
 * java.util.Set Set} is fulfilled. This is not a requirement for elements in this set, though.
 *
 * <p>If the used compare-method is not consistent with <code>equals</code>, unequal (i.e.,
 * different) elements that are equal according to the order can exist in a set. In this case,
 * elements that are added first will have a higher rank than elements of the same order that are
 * added later.
 *
 * @param <T> the type of elements maintained by this set
 */
public interface OrderStatisticSet<T> extends NavigableSet<T> {

  /**
   * Returns the element of this set with the given rank. The lowest element in the set has rank ==
   * 0, the largest element in the set has rank == size - 1.
   *
   * <p>If the used compare-method is not consistent with <code>equals</code>, i.e., <code>
   * compare(a, b)</code> does <b>not</b> imply <code>a.equals(b)</code>, elements that were added
   * first will have a higher rank than elements equal by comparison that were added later.
   *
   * <p>Example:
   *
   * <pre>
   *     * Element type T: {@link java.awt.Point Point(x, y)}
   *     * Comparator: compare(a, b) = a.x - b.x
   *
   *     add(new Point(1, 1))
   *     add(new Point(2, 2))
   *     add(new Point(1, 3))
   *
   *     After these three operations, the set order will be:
   *     Pair.of(1, 3) - Pair.of(1, 1) - Pair.of(2, 2)
   *
   *     Thus:
   *     getByRank(0) = Point(1, 3)
   *     getByRank(1) = Point(1, 1)
   *     getByRank(2) = Point(2, 2)
   *   </pre>
   *
   * @param pIndex the rank of the element to return
   * @return the element of this set with the given rank
   * @throws IndexOutOfBoundsException if the given rank is out of the range of this set (i.e.,
   *     pRank &lt; 0 || pRank &gt;= size)
   */
  T getByRank(int pIndex);

  /**
   * Remove the element of this set with the given rank and return it.
   *
   * <p>The lowest element in the set has rank == 0, the largest element in the set has rank == size
   * - 1.
   *
   * @param pIndex the rank of the element to remove
   * @return the removed element
   * @throws IndexOutOfBoundsException if the given rank is out of the range of this set (i.e.,
   *     pRank &lt; 0 || pRank &gt;= size)
   * @see #getByRank(int)
   */
  T removeByRank(int pIndex);

  /**
   * Return the rank of the given element in this set. Returns -1 if the element does not exist in
   * the set.
   *
   * <p>The lowest element in the set has rank == 0, the largest element in the set has rank == size
   * - 1.
   *
   * @param pObj the element to return the rank for
   * @return the rank of the given element in the set, or -1 if the element is not in the set
   * @throws NullPointerException if the given element is <code>null</code>
   */
  int rankOf(T pObj);
}
