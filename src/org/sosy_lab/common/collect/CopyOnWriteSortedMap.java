/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.common.collect;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import com.google.common.collect.ForwardingCollection;
import com.google.common.collect.ForwardingSortedMap;
import com.google.common.collect.ForwardingSortedSet;


/**
 * This is a map implementation that uses copy-on-write behavior.
 * This may be a good fit when you want to keep old snapshots of the map
 * while modifying it. Through the use of a {@link PersistentMap} backend,
 * snapshots and modifying operations are both cheap (O(1) for the former,
 * usually O(log n) for the latter).
 *
 * There are two usage patterns for this map.
 * First, you can keep one instance of of the map which you modify,
 * and eventually call {@link #getSnapshot()} to take immutable snapshots.
 * Second, you can use an instance of the map and create copies of it with the
 * {@link #copyOf(CopyOnWriteSortedMap)} method (these copies are O(1)).
 * Then you can modify both the old and the new instance, but modifications
 * to one of it won't be reflected by the other.
 *
 * All collection views returned my methods of this map are live views
 * and change if this map is modified.
 * However, they currently do not support any modifying operations.
 * All iterators produced by the collection views iterate over an immutable
 * snapshot of the map taken at iterator creation time and thus do not reflect
 * intermediate changes to the map. The iterators also don't support the {@link Iterator#remove()} method.
 * Thus it is safe to iterate over the map while changing it.
 *
 * This implementation is thread-safe and lock free,
 * but does not guarantee freedom of starvation.
 * Bulk operations are not atomic.
 *
 * @param <K> The type of keys.
 * @param <V> The type of values.
 */
public class CopyOnWriteSortedMap<K, V> extends ForwardingSortedMap<K, V> {

  private final AtomicReference<PersistentSortedMap<K, V>> map;

  private CopyOnWriteSortedMap(PersistentSortedMap<K, V> pMap) {
    map = new AtomicReference<>(pMap);
  }

  /**
   * Create a new map instance with an initial content of the given map.
   * To create an empty instance, get an empty instance of your favorite
   * {@link PersistentSortedMap} implementation and pass it to this method.
   */
  public static <K extends Comparable<? super K>, V> CopyOnWriteSortedMap<K, V> copyOf(PersistentSortedMap<K, V> pMap)  {
    return new CopyOnWriteSortedMap<>(pMap);
  }

  /**
   * Create a new map instance containing all entries of the given map.
   * The snapshot of the given map is created atomically.
   * Changes to the new map don't reflect in the given map and vice-versa.
   */
  public static <K extends Comparable<? super K>, V> CopyOnWriteSortedMap<K, V> copyOf(CopyOnWriteSortedMap<K, V> pMap) {
    return new CopyOnWriteSortedMap<>(pMap.map.get());
  }

  @Override
  protected SortedMap<K, V> delegate() {
    return map.get();
  }

  /**
   * Return a immutable snapshot of the current state of the map.
   */
  public PersistentSortedMap<K, V> getSnapshot() {
    return map.get();
  }

  /**
   * @see Map#put(Object, Object)
   *
   * This method is not starvation free,
   * and thus not strictly guaranteed to terminate in presence of concurrent
   * modifying operations.
   */
  @Override
  public V put(K pKey, V pValue) {
    PersistentSortedMap<K, V> oldMap;

    oldMap = put0(pKey, pValue);

    return oldMap.get(pKey);
  }

  /**
   * Update and replace the map, returning the previous map.
   */
  private PersistentSortedMap<K, V> put0(K pKey, V pValue) {
    PersistentSortedMap<K, V> oldMap;
    PersistentSortedMap<K, V> newMap;
    do {
      oldMap = map.get();
      newMap = oldMap.putAndCopy(pKey, pValue);
    } while (!map.compareAndSet(oldMap, newMap));

    return oldMap;
  }

  /**
   * @see Map#remove(Object)
   *
   * This method is not starvation free,
   * and thus not strictly guaranteed to terminate in presence of concurrent
   * modifying operations.
   */
  @Override
  public @Nullable V remove(Object pKey) {
    PersistentSortedMap<K, V> oldMap;
    PersistentSortedMap<K, V> newMap;


    do {
      oldMap = map.get();
      if (!oldMap.containsKey(pKey)) {
        return null;
      }

      newMap = oldMap.removeAndCopy(pKey);
    } while (!map.compareAndSet(oldMap, newMap));

    return oldMap.get(pKey);
  }

  /**
   * @see Map#putAll(Map)
   *
   * This method is not atomic!
   * It inserts all keys one after the other,
   * and in between each operation arbitrary operations from other threads
   * might get executed.
   *
   * This method is not starvation free,
   * and thus not strictly guaranteed to terminate in presence of concurrent
   * modifying operations.
   */
  @Override
  public void putAll(Map<? extends K, ? extends V> pMap) {
    for (Map.Entry<? extends K, ? extends V> entry : pMap.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * @see Map#clear()
   */
  @Override
  public void clear() {
    map.set(map.get().empty());
  }

  // Override the collection view methods
  // so that they return also live views and not immutable snapshots.

  @Override
  public SortedSet<Map.Entry<K, V>> entrySet() {
    return new ForwardingSortedSet<Map.Entry<K,V>>() {

        @Override
        protected SortedSet<Map.Entry<K, V>> delegate() {
          return map.get().entrySet();
        }
      };
  }

  @Override
  public SortedSet<K> keySet() {
    return new ForwardingSortedSet<K>() {

      @Override
      protected SortedSet<K> delegate() {
        return map.get().keySet();
      }
    };
  }

  @Override
  public Collection<V> values() {
    return new ForwardingCollection<V>() {

      @Override
      protected Collection<V> delegate() {
        return map.get().values();
      }
    };
  }

  @Override
  public SortedMap<K, V> headMap(final K pToKey) {
    return new ForwardingSortedMap<K,V>() {

      @Override
      protected SortedMap<K, V> delegate() {
        return map.get().headMap(pToKey);
      }
    };
  }

  @Override
  public SortedMap<K, V> tailMap(final K pFromKey) {
    return new ForwardingSortedMap<K,V>() {

      @Override
      protected SortedMap<K, V> delegate() {
        return map.get().tailMap(pFromKey);
      }
    };
  }

  @Override
  public SortedMap<K, V> subMap(final K pFromKey, final K pToKey) {
    return new ForwardingSortedMap<K,V>() {

      @Override
      protected SortedMap<K, V> delegate() {
        return map.get().subMap(pFromKey, pToKey);
      }
    };
  }
}
