// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.annotations.Immutable;
import java.util.NavigableMap;
import java.util.NavigableSet;

/**
 * Sub-interface of {@link PersistentMap} analog to {@link NavigableMap}.
 *
 * @param <K> The type of keys.
 * @param <V> The type of values.
 */
@Immutable(containerOf = {"K", "V"})
public interface PersistentSortedMap<K, V>
    extends PersistentSortedMapBridge<K, V>, NavigableMap<K, V> {

  @Override
  @CheckReturnValue
  PersistentSortedMap<K, V> putAndCopy(K key, V value);

  @Override
  @CheckReturnValue
  PersistentSortedMap<K, V> removeAndCopy(Object pKey);

  @Override
  @CheckReturnValue
  PersistentSortedMap<K, V> empty();

  @Override
  NavigableSet<Entry<K, V>> entrySet();

  @Override
  NavigableSet<K> keySet();

  @Override
  NavigableMap<K, V> descendingMap();

  @Override
  NavigableMap<K, V> subMap(K pFromKey, K pToKey);

  @Override
  NavigableMap<K, V> subMap(K pFromKey, boolean pFromInclusive, K pToKey, boolean pToInclusive);

  @Override
  NavigableMap<K, V> headMap(K pToKey);

  @Override
  NavigableMap<K, V> headMap(K pToKey, boolean pInclusive);

  @Override
  NavigableMap<K, V> tailMap(K pFromKey);

  @Override
  NavigableMap<K, V> tailMap(K pFromKey, boolean pInclusive);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  Entry<K, V> pollFirstEntry();

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  Entry<K, V> pollLastEntry();
}
