// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;

/**
 * A {@link NavigableMap} that allows two additional operations: receiving (and deleting) an entry
 * by its <i>rank</i>, and getting the rank of an entry.
 *
 * <p>Implementations should adhere to all contracts of the <code>NavigableMap</code> interface.
 *
 * <p>Implementing classes should provide two means for comparing elements:
 *
 * <ol>
 *   <li>Using the natural ordering over the keys. In this case, all keys of the map have to
 *       implement the {@link Comparable} interface.
 *   <li>Using a {@link java.util.Comparator Comparator} to create an order over the keys of the
 *       map.
 * </ol>
 *
 * <p>In both cases, the used compare-method should be consistent with <code>equals</code>, i.e.,
 * <code>compare(k, l) == 0  =&gt;  k.equals(l)</code>, so that the {@link java.util.Map Map}
 * interface is correctly implemented. If the used compare-method is not consistent with <code>
 * equals</code>, the Map contract is not fulfilled (See the {@link java.util.SortedMap SortedMap}
 * interface for a more detailed description).
 *
 * @param <K> the type of the keys of this map
 * @param <V> the type of the values of this map
 * @see java.util.Map
 * @see java.util.SortedMap
 */
public interface OrderStatisticMap<K, V> extends NavigableMap<K, V> {

  /**
   * Returns the key of this map with the given rank. The lowest key in the map has rank == 0, the
   * largest key in the set has rank == size - 1.
   *
   * <p>If this OrderStatisticMap is a view on some backing OrderStatisticMap (as created, e.g., by
   * {@link #descendingMap()} or {@link #headMap(Object)}), the returned rank is in relation to the
   * keys in the view, not in relation to the keys in the backing set. Thus, one can always expect
   * that key of rank 0 is the first key in this map, and key of rank <code>{@link #size()} - 1
   * </code> is the last.
   *
   * @param pIndex the rank of the key to return
   * @return the key of this map with the given rank
   * @throws IndexOutOfBoundsException if the given rank is out of the range of this map (i.e.,
   *     pRank &lt; 0 || pRank &gt;= size)
   */
  default K getKeyByRank(int pIndex) {
    return getEntryByRank(pIndex).getKey();
  }

  /**
   * Returns the entry of this map with the given rank. The lowest entry in the set has rank == 0,
   * the largest entry in the set has rank == size - 1.
   *
   * <p>If this OrderStatisticMap is a view on some backing OrderStatisticMap (as created, e.g., by
   * {@link #descendingMap()} or {@link #headMap(Object)}), the returned rank is in relation to the
   * entries in the view, not in relation to the entries in the backing set. Thus, one can always
   * expect that entry of rank 0 is the first entry in this map, and entry of rank <code>
   * {@link #size()} - 1</code> is the last.
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
   * <p>If this OrderStatisticMap is a view on some backing OrderStatisticMap (as created, e.g., by
   * {@link #descendingMap()} or {@link #headMap(Object)}), the returned rank is in relation to the
   * entries in the view, not in relation to the entries in the backing set. Thus, one can always
   * expect that entry of rank 0 is the first entry in this map, and entry of rank <code>
   * {@link #size()} - 1</code> is the last.
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
   * <p>If this OrderStatisticMap is a view on some backing OrderStatisticMap (as created, e.g., by
   * {@link #descendingMap()} or {@link #headMap(Object)}), the returned rank is in relation to the
   * entries in the view, not in relation to the entries in the backing set. Thus, one can always
   * expect that key of rank 0 is the first key in this map, and key of rank <code>
   * {@link #size()} - 1</code> is the last.
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

  /**
   * Creates a new empty OrderStatisticMap using natural ordering. The returned map guarantees
   * performance only in O(n) for the operations specific to the OrderStatisticMap interface.
   */
  static <K, V> OrderStatisticMap<K, V> create() {
    return NaiveOrderStatisticMap.createMap();
  }

  /**
   * Creates a new empty OrderStatisticMap using the given comparator over its keys. The returned
   * map guarantees performance only in O(n) for the operations specific to the OrderStatisticMap
   * interface.
   */
  static <K, V> OrderStatisticMap<K, V> create(Comparator<? super K> pComparator) {
    return NaiveOrderStatisticMap.createMap(pComparator);
  }

  /**
   * Creates a new OrderStatisticSet containing the same entries as the given map, using natural
   * ordering over its keys. The returned map guarantees performance only in O(n) for the operations
   * specific to the OrderStatisticMap interface.
   */
  static <K, V> OrderStatisticMap<K, V> createWithNaturalOrder(Map<? extends K, ? extends V> pMap) {
    return NaiveOrderStatisticMap.createMapWithNaturalOrder(pMap);
  }

  /**
   * Creates a new OrderStatisticMap containing the same entries and using the same order over keys
   * as the given {@link SortedMap}. The returned map guarantees performance only in O(n) for the
   * operations specific to the OrderStatisticMap interface.
   *
   * @param pSortedMap map to use entries and ordering of
   * @param <K> type of the keys of the given and new map
   * @param <V> type of the values of the given and new map
   * @return a new OrderStatisticMap containing the same entries and using the same order over keys
   *     as the given map
   */
  static <K, V> OrderStatisticMap<K, V> createWithSameOrder(SortedMap<K, ? extends V> pSortedMap) {
    return NaiveOrderStatisticMap.createMapWithSameOrder(pSortedMap);
  }
}
