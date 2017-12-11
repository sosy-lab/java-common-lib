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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingNavigableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * An {@link OrderStatisticSet} with naive implementations of its functions.
 *
 * <p>The class wraps a {@link NavigableSet} object and delegates all methods inherited from the
 * <code>NavigableSet</code> interface to that. For the methods particular to the <code>
 * OrderStatisticSet</code> interface, it provides naive implementations that guarantee
 * performance only in O(n).
 *
 * @param <E> type of the elements of this set. See the Javadoc of {@link OrderStatisticSet} for
 *     possible constraints on this type
 * @see OrderStatisticSet
 */
final class NaiveOrderStatisticSet<E> extends ForwardingNavigableSet<E>
    implements OrderStatisticSet<E>, Serializable {

  private static final long serialVersionUID = -1941093176613766876L;

  private final NavigableSet<E> delegate;

  private NaiveOrderStatisticSet(NavigableSet<E> pNavigableSet) {
    delegate = pNavigableSet;
  }

  /** Creates a new empty OrderStatisticSet using natural ordering. */
  static <E> NaiveOrderStatisticSet<E> createSet() {
    return new NaiveOrderStatisticSet<>(new TreeSet<>());
  }

  /** Creates a new empty OrderStatisticSet using the given comparator. */
  static <E> NaiveOrderStatisticSet<E> createSet(Comparator<? super E> pComparator) {
    return new NaiveOrderStatisticSet<>(new TreeSet<>(checkNotNull(pComparator)));
  }

  /**
   * Creates a new OrderStatisticSet containing the same elements as the given collection, using
   * natural ordering.
   */
  static <E> NaiveOrderStatisticSet<E> createSetWithNaturalOrder(Collection<E> pSet) {
    return new NaiveOrderStatisticSet<>(new TreeSet<>(checkNotNull(pSet)));
  }

  /**
   * Creates a new OrderStatisticSet containing the same elements and using the same order as the
   * given {@link NavigableSet}.
   *
   * @param pNavigableSet set to use elements and ordering of
   * @param <E> type of the elements of the given and new set
   * @return a new OrderStatisticSet containing the same elements and using the same order as the
   *     given set
   */
  static <E> NaiveOrderStatisticSet<E> createSetWithSameOrder(NavigableSet<E> pNavigableSet) {
    return new NaiveOrderStatisticSet<>(new TreeSet<>(checkNotNull(pNavigableSet)));
  }

  @Override
  protected NavigableSet<E> delegate() {
    return delegate;
  }

  @Override
  public E getByRank(int pIndex) {
    return Iterables.get(delegate, pIndex);
  }

  @Override
  @CanIgnoreReturnValue
  public E removeByRank(int pIndex) {
    E elem = getByRank(pIndex);
    Preconditions.checkState(delegate.remove(elem), "Element could be retrieved, but not deleted");
    return elem;
  }

  @Override
  public int rankOf(E pObj) {
    checkNotNull(pObj);
    return Iterables.indexOf(delegate, o -> compare(o, pObj) == 0);
  }

  @SuppressWarnings("unchecked")
  private int compare(E pO1, E pO2) {
    Comparator<? super E> comparator = comparator();
    if (comparator != null) {
      return comparator.compare(pO1, pO2);
    } else {
      return ((Comparable<E>) pO1).compareTo(pO2);
    }
  }

  @Override
  public OrderStatisticSet<E> descendingSet() {
    return new NaiveOrderStatisticSet<>(super.descendingSet());
  }

  @Override
  public OrderStatisticSet<E> subSet(
      E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
    return new NaiveOrderStatisticSet<>(
        super.subSet(fromElement, fromInclusive, toElement, toInclusive));
  }

  @Override
  public OrderStatisticSet<E> headSet(E toElement, boolean inclusive) {
    return new NaiveOrderStatisticSet<>(super.headSet(toElement, inclusive));
  }

  @Override
  public OrderStatisticSet<E> tailSet(E fromElement, boolean inclusive) {
    return new NaiveOrderStatisticSet<>(super.tailSet(fromElement, inclusive));
  }

  @Override
  public OrderStatisticSet<E> headSet(E toElement) {
    return headSet(toElement, /* inclusive= */ false);
  }

  @Override
  public OrderStatisticSet<E> subSet(E fromElement, E toElement) {
    return subSet(fromElement, /* fromInclusive= */ true, toElement, /* toInclusive= */ false);
  }

  @Override
  public OrderStatisticSet<E> tailSet(E fromElement) {
    return tailSet(fromElement, /* inclusive= */ true);
  }
}
