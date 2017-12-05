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
import java.util.Map;
import java.util.NavigableMap;

public interface OrderStatisticMap<K, V> extends NavigableMap<K, V> {

  /**
   * Returns the element of this set with the given rank. The lowest element in the set has rank ==
   * 0, the largest element in the set has rank == size - 1.
   *
   * @param pIndex the rank of the element to return
   * @return the element of this set with the given rank
   * @throws IndexOutOfBoundsException if the given rank is out of the range of this set (i.e.,
   *     pRank &lt; 0 || pRank &gt;= size)
   */
  K getKeyByRank(int pIndex);

  /**
   * Returns the entry of this map with the given rank. The lowest entry in the set has rank == 0,
   * the largest entry in the set has rank == size - 1.
   *
   * @param pIndex the rank of the entry to return
   * @return the entry of this map with the given rank
   * @throws IndexOutOfBoundsException if the given rank is out of the range of this map (i.e.,
   *     pRank &lt; 0 || pRank &gt;= size)
   */
  Map.Entry<K, V> getEntryByRank(int pIndex);

  /**
   * Remove the entry of this map with the given rank and return its key.
   *
   * <p>The lowest entry in the map has rank == 0, the largest entry in the map has rank == size -
   * 1.
   *
   * @param pIndex the rank of the element to remove
   * @return the removed element
   * @throws IndexOutOfBoundsException if the given rank is out of the range of this set (i.e.,
   *     pRank &lt; 0 || pRank &gt;= size)
   * @see #getKeyByRank(int)
   */
  @CanIgnoreReturnValue
  K removeByRank(int pIndex);

  /**
   * Return the rank of the entry with the given key in this map. Returns -1 if the key does not
   * exist in the map.
   *
   * <p>The lowest entry in the set has rank == 0, the largest entry in the set has rank == size -
   * 1.
   *
   * @param pObj the key of the entry to return the rank for
   * @return the rank of the entry with the given key in the map, or -1 if the key is not in the set
   * @throws NullPointerException if the given key is <code>null</code>
   */
  int rankOf(K pObj);

  @Override
  OrderStatisticMap<K, V> descendingMap();

  @Override
  OrderStatisticSet<K> navigableKeySet();

  @Override
  OrderStatisticSet<K> descendingKeySet();

  @Override
  OrderStatisticMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive);

  @Override
  OrderStatisticMap<K, V> headMap(K toKey, boolean inclusive);

  @Override
  OrderStatisticMap<K, V> tailMap(K fromKey, boolean inclusive);

  @Override
  OrderStatisticMap<K, V> subMap(K fromKey, K toKey);

  @Override
  OrderStatisticMap<K, V> headMap(K toKey);

  @Override
  OrderStatisticMap<K, V> tailMap(K fromKey);

  /** Creates a new empty OrderStatisticMap using natural ordering. */
  static <K, V> OrderStatisticMap<K, V> create() {
    return NaiveOrderStatisticMap.createMap();
  }

  /** Creates a new empty OrderStatisticMap using the given comparator over its keys. */
  static <K, V> OrderStatisticMap<K, V> create(Comparator<? super K> pComparator) {
    return NaiveOrderStatisticMap.createMap(pComparator);
  }

  /**
   * Creates a new OrderStatisticSet containing the same entries as the given map, using natural
   * ordering over its keys.
   */
  static <K, V> OrderStatisticMap<K, V> createWithNaturalOrder(Map<? extends K, ? extends V> pMap) {
    return NaiveOrderStatisticMap.createMapWithNaturalOrder(pMap);
  }

  /**
   * Creates a new OrderStatisticMap containing the same entries and using the same order over keys
   * as the given {@link NavigableMap}.
   *
   * @param pNavigableMap map to use entries and ordering of
   * @param <K> type of the keys of the given and new map
   * @param <V> type of the values of the given and new map
   * @return a new OrderStatisticMap containing the same entries and using the same order over keys
   *     as the given map
   */
  static <K, V> OrderStatisticMap<K, V> createWithSameOrder(
      NavigableMap<K, ? extends V> pNavigableMap) {
    return NaiveOrderStatisticMap.createMapWithSameOrder(pNavigableMap);
  }
}
