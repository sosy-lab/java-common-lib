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

import com.google.common.collect.ForwardingNavigableMap;
import com.google.common.collect.Maps;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * An {@link OrderStatisticMap} with naive implementations of its functions.
 *
 * <p>The class wraps a {@link NavigableMap} object and delegates all methods inherited from the
 * <code>NavigableMap</code> interface to that. For the methods particular to the <code>
 * OrderStatisticMap</code> interface, it provides naive implementations that do guarantee
 * performance only in O(n).
 *
 * @param <K> type of the keys of this map. See the Javadoc of {@link OrderStatisticMap} for
 *     possible constraints on this type
 * @param <V> type of the values of this map
 * @see OrderStatisticMap
 */
final class NaiveOrderStatisticMap<K, V> extends ForwardingNavigableMap<K, V>
    implements OrderStatisticMap<K, V>, Serializable {

  private static final long serialVersionUID = -3542217590830996599L;

  private final NavigableMap<K, V> delegate;

  private NaiveOrderStatisticMap(NavigableMap<K, ? extends V> pNavigableMap) {
    delegate = new TreeMap<>(pNavigableMap);
  }

  /** Creates a new empty OrderStatisticMap using natural ordering. */
  static <K, V> NaiveOrderStatisticMap<K, V> createMap() {
    return new NaiveOrderStatisticMap<>(new TreeMap<>());
  }

  /** Creates a new empty OrderStatisticMap using the given comparator over its keys. */
  static <K, V> NaiveOrderStatisticMap<K, V> createMap(Comparator<? super K> pComparator) {
    return new NaiveOrderStatisticMap<>(new TreeMap<>(checkNotNull(pComparator)));
  }

  /**
   * Creates a new OrderStatisticSet containing the same entries as the given map, using natural
   * ordering over its keys.
   */
  static <K, V> NaiveOrderStatisticMap<K, V> createMapWithNaturalOrder(
      Map<? extends K, ? extends V> pMap) {
    return new NaiveOrderStatisticMap<>(new TreeMap<>(checkNotNull(pMap)));
  }

  /**
   * Creates a new OrderStatisticMap containing the sames entries as the given map and using the
   * same ordering over its keys as the given map.
   */
  static <K, V> NaiveOrderStatisticMap<K, V> createMapWithSameOrder(
      NavigableMap<K, ? extends V> pNavigableMap) {
    return new NaiveOrderStatisticMap<>(checkNotNull(pNavigableMap));
  }

  @Override
  protected NavigableMap<K, V> delegate() {
    return delegate;
  }

  @Override
  public K getKeyByRank(int pIndex) {
    return NaiveOrderStatisticSet.createSetWithSameOrder(delegate.navigableKeySet())
        .getByRank(pIndex);
  }

  @Override
  public Entry<K, V> getEntryByRank(int pIndex) {
    K key = getKeyByRank(pIndex);
    return Maps.immutableEntry(key, get(key));
  }

  @Override
  @CanIgnoreReturnValue
  public K removeByRank(int pIndex) {
    K key = getKeyByRank(pIndex);
    V val = remove(key);
    assert val != null : "Key could be retrieved by rank, but no (or null) value associated";
    return key;
  }

  @Override
  public int rankOf(K pObj) {
    checkNotNull(pObj);
    return NaiveOrderStatisticSet.createSetWithSameOrder(delegate.navigableKeySet()).rankOf(pObj);
  }

  @Override
  public OrderStatisticSet<K> navigableKeySet() {
    return NaiveOrderStatisticSet.createSetWithSameOrder(super.navigableKeySet());
  }

  @Override
  public OrderStatisticSet<K> descendingKeySet() {
    return NaiveOrderStatisticSet.createSetWithSameOrder(super.descendingKeySet());
  }

  @Override
  public OrderStatisticMap<K, V> descendingMap() {
    return new NaiveOrderStatisticMap<>(super.descendingMap());
  }

  @Override
  public OrderStatisticMap<K, V> subMap(
      K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
    return new NaiveOrderStatisticMap<>(super.subMap(fromKey, fromInclusive, toKey, toInclusive));
  }

  @Override
  public OrderStatisticMap<K, V> headMap(K toKey, boolean inclusive) {
    return new NaiveOrderStatisticMap<>(super.headMap(toKey, inclusive));
  }

  @Override
  public OrderStatisticMap<K, V> tailMap(K fromKey, boolean inclusive) {
    return new NaiveOrderStatisticMap<>(super.tailMap(fromKey, inclusive));
  }

  @Override
  public OrderStatisticMap<K, V> headMap(K toKey) {
    return headMap(toKey, /* inclusive= */ false);
  }

  @Override
  public OrderStatisticMap<K, V> subMap(K fromKey, K toKey) {
    return subMap(fromKey, /* fromInclusive= */ true, toKey, /* toInclusive= */ false);
  }

  @Override
  public OrderStatisticMap<K, V> tailMap(K fromKey) {
    return tailMap(fromKey, /* inclusive= */ true);
  }
}
