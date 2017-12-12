/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.SortedSet;

/**
 * A {@link NavigableSet} that allows two additional operations: receiving (and deleting) an element
 * by its <i>rank</i>, and getting the rank of an element.
 *
 * <p>Implementations should adhere to all contracts of the <code>NavigableSet</code> interface.
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
 * <p>In both cases, the used compare-method should be consistent with <code>equals</code>, i.e.,
 * <code>compare(a, b) == 0  =&gt;  a.equals(b)</code>, so that the contract provided by {@link
 * java.util.Set Set} is fulfilled. If the used compare-method is not consistent with <code>equals
 * </code>, the Set contract is not fulfilled.
 *
 * @param <E> the type of elements maintained by this set
 */
public interface OrderStatisticSet<E> extends NavigableSet<E> {

  /**
   * Returns the element of this set with the given rank. The lowest element in the set has rank ==
   * 0, the largest element in the set has rank == size - 1.
   *
   * <p>If this OrderStatisticSet is a view on some backing OrderStatisticSet (as created, e.g., by
   * {@link #descendingSet()} or {@link #headSet(Object)}), the returned rank is in relation to the
   * elements in the view, not in relation to the elements in the backing set. Thus, one can always
   * expect that element of rank 0 is the first element in this set, and element of rank <code>
   * {@link #size()} - 1</code> is the last.
   *
   * @param pIndex the rank of the element to return
   * @return the element of this set with the given rank
   * @throws IndexOutOfBoundsException if the given rank is out of the range of this set (i.e.,
   *     pRank &lt; 0 || pRank &gt;= size)
   */
  E getByRank(int pIndex);

  /**
   * Remove the element of this set with the given rank and return it.
   *
   * <p>The lowest element in the set has rank == 0, the largest element in the set has rank == size
   * - 1.
   *
   * <p>If this OrderStatisticSet is a view on some backing OrderStatisticSet (as created, e.g., by
   * {@link #descendingSet()} or {@link #headSet(Object)}), the returned rank is in relation to the
   * elements in the view, not in relation to the elements in the backing set. Thus, one can always
   * expect that element of rank 0 is the first element in this set, and element of rank <code>
   * {@link #size()} - 1</code> is the last.
   *
   * @param pIndex the rank of the element to remove
   * @return the removed element
   * @throws IndexOutOfBoundsException if the given rank is out of the range of this set (i.e.,
   *     pRank &lt; 0 || pRank &gt;= size)
   * @see #getByRank(int)
   */
  @CanIgnoreReturnValue
  E removeByRank(int pIndex);

  /**
   * Return the rank of the given element in this set. Returns -1 if the element does not exist in
   * the set.
   *
   * <p>The lowest element in the set has rank == 0, the largest element in the set has rank == size
   * - 1.
   *
   * <p>If this OrderStatisticSet is a view on some backing OrderStatisticSet (as created, e.g., by
   * {@link #descendingSet()} or {@link #headSet(Object)}), the returned rank is in relation to the
   * elements in the view, not in relation to the elements in the backing set. Thus, one can always
   * expect that element of rank 0 is the first element in this set, and element of rank <code>
   * {@link #size()} - 1</code> is the last.
   *
   * @param pObj the element to return the rank for
   * @return the rank of the given element in the set, or -1 if the element is not in the set
   * @throws NullPointerException if the given element is <code>null</code>
   */
  int rankOf(E pObj);

  @Override
  OrderStatisticSet<E> descendingSet();

  @Override
  OrderStatisticSet<E> subSet(
      E pFromElement, boolean fromInclusive, E pToElement, boolean toInclusive);

  @Override
  OrderStatisticSet<E> headSet(E pToElement, boolean inclusive);

  @Override
  OrderStatisticSet<E> tailSet(E pFromElement, boolean inclusive);

  @Override
  OrderStatisticSet<E> subSet(E pFromElement, E pToElement);

  @Override
  OrderStatisticSet<E> headSet(E pToElement);

  @Override
  OrderStatisticSet<E> tailSet(E pFromElement);

  /**
   * Creates a new empty OrderStatisticSet using natural ordering. The returned map guarantees
   * performance only in O(n) for the operations specific to the OrderStatisticSet interface.
   */
  static <E> OrderStatisticSet<E> create() {
    return NaiveOrderStatisticSet.createSet();
  }

  /**
   * Creates a new empty OrderStatisticSet using the given comparator. The returned map guarantees
   * performance only in O(n) for the operations specific to the OrderStatisticSet interface.
   */
  static <E> OrderStatisticSet<E> create(Comparator<? super E> pComparator) {
    return NaiveOrderStatisticSet.createSet(pComparator);
  }

  /**
   * Creates a new OrderStatisticSet containing the same elements as the given Iterable, using
   * natural ordering. The returned map guarantees performance only in O(n) for the operations
   * specific to the OrderStatisticSet interface.
   */
  static <E> OrderStatisticSet<E> createWithNaturalOrder(Iterable<E> pCollection) {
    return NaiveOrderStatisticSet.createSetWithNaturalOrder(pCollection);
  }

  /**
   * Creates a new OrderStatisticSet containing the same elements and using the same order as the
   * given {@link SortedSet}. The returned map guarantees performance only in O(n) for the
   * operations specific to the OrderStatisticSet interface.
   *
   * @param pSortedSet set to use elements and ordering of
   * @param <E> type of the elements of the given and new set
   * @return a new OrderStatisticSet containing the same elements and using the same order as the
   *     given set
   */
  static <E> OrderStatisticSet<E> createWithSameOrder(SortedSet<E> pSortedSet) {
    return NaiveOrderStatisticSet.createSetWithSameOrder(pSortedSet);
  }
}
