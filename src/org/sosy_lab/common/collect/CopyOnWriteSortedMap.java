// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ForwardingCollection;
import com.google.common.collect.ForwardingNavigableMap;
import com.google.common.collect.ForwardingNavigableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Var;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.concurrent.atomic.AtomicReference;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This is a map implementation that uses copy-on-write behavior. This may be a good fit when you
 * want to keep old snapshots of the map while modifying it. Through the use of a {@link
 * PersistentMap} backend, snapshots and modifying operations are both cheap (O(1) for the former,
 * usually O(log n) for the latter).
 *
 * <p>There are two usage patterns for this map. First, you can keep one instance of of the map
 * which you modify, and eventually call {@link #getSnapshot()} to take immutable snapshots. Second,
 * you can use an instance of the map and create copies of it with the {@link
 * #copyOf(CopyOnWriteSortedMap)} method (these copies are O(1)). Then you can modify both the old
 * and the new instance, but modifications to one of it won't be reflected by the other.
 *
 * <p>All collection views returned my methods of this map are live views and change if this map is
 * modified. However, they currently do not support any modifying operations. All iterators produced
 * by the collection views iterate over an immutable snapshot of the map taken at iterator creation
 * time and thus do not reflect intermediate changes to the map. The iterators also don't support
 * the {@link Iterator#remove()} method. Thus it is safe to iterate over the map while changing it.
 *
 * <p>This implementation is thread-safe and lock free, but does not guarantee freedom of
 * starvation. Bulk operations are not atomic.
 *
 * @param <K> The type of keys.
 * @param <V> The type of values.
 */
public final class CopyOnWriteSortedMap<K, V> extends ForwardingNavigableMap<K, V> {

  private final AtomicReference<PersistentSortedMap<K, V>> map;

  private CopyOnWriteSortedMap(PersistentSortedMap<K, V> pMap) {
    map = new AtomicReference<>(checkNotNull(pMap));
  }

  /**
   * Create a new map instance with an initial content of the given map. To create an empty
   * instance, get an empty instance of your favorite {@link PersistentSortedMap} implementation and
   * pass it to this method.
   */
  public static <K extends Comparable<? super K>, V> CopyOnWriteSortedMap<K, V> copyOf(
      PersistentSortedMap<K, V> pMap) {
    return new CopyOnWriteSortedMap<>(pMap);
  }

  /**
   * Create a new map instance containing all entries of the given map. The snapshot of the given
   * map is created atomically. Changes to the new map don't reflect in the given map and
   * vice-versa.
   */
  public static <K extends Comparable<? super K>, V> CopyOnWriteSortedMap<K, V> copyOf(
      CopyOnWriteSortedMap<K, V> pMap) {
    return new CopyOnWriteSortedMap<>(pMap.map.get());
  }

  @Override
  protected NavigableMap<K, V> delegate() {
    return map.get();
  }

  /** Return a immutable snapshot of the current state of the map. */
  public PersistentSortedMap<K, V> getSnapshot() {
    return map.get();
  }

  /**
   * This method is not starvation free, and thus not strictly guaranteed to terminate in presence
   * of concurrent modifying operations.
   *
   * @see Map#put(Object, Object)
   */
  @Override
  @CanIgnoreReturnValue
  public @Nullable V put(K pKey, V pValue) {
    PersistentSortedMap<K, V> oldMap = put0(pKey, pValue);

    return oldMap.get(pKey);
  }

  /** Update and replace the map, returning the previous map. */
  private PersistentSortedMap<K, V> put0(K pKey, V pValue) {
    @Var PersistentSortedMap<K, V> oldMap;
    @Var PersistentSortedMap<K, V> newMap;
    do {
      oldMap = map.get();
      newMap = oldMap.putAndCopy(pKey, pValue);
    } while (!map.compareAndSet(oldMap, newMap));

    return oldMap;
  }

  /**
   * This method is not starvation free, and thus not strictly guaranteed to terminate in presence
   * of concurrent modifying operations.
   *
   * @see Map#remove(Object)
   */
  @Override
  @CanIgnoreReturnValue
  public @Nullable V remove(Object pKey) {
    @Var PersistentSortedMap<K, V> oldMap;
    @Var PersistentSortedMap<K, V> newMap;

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
   * This method is not starvation free, and thus not strictly guaranteed to terminate in presence
   * of concurrent modifying operations.
   *
   * @see NavigableMap#pollFirstEntry()
   */
  @Override
  @CanIgnoreReturnValue
  public @Nullable Entry<K, V> pollFirstEntry() {
    @Var PersistentSortedMap<K, V> oldMap;
    @Var PersistentSortedMap<K, V> newMap;
    @Var Entry<K, V> firstEntry;

    do {
      oldMap = map.get();
      firstEntry = oldMap.firstEntry();
      if (firstEntry == null) {
        return null;
      }
      newMap = oldMap.removeAndCopy(firstEntry.getKey());
    } while (!map.compareAndSet(oldMap, newMap));

    return firstEntry;
  }

  /**
   * This method is not starvation free, and thus not strictly guaranteed to terminate in presence
   * of concurrent modifying operations.
   *
   * @see NavigableMap#pollLastEntry()
   */
  @Override
  @CanIgnoreReturnValue
  public @Nullable Entry<K, V> pollLastEntry() {
    @Var PersistentSortedMap<K, V> oldMap;
    @Var PersistentSortedMap<K, V> newMap;
    @Var Entry<K, V> lastEntry;

    do {
      oldMap = map.get();
      lastEntry = oldMap.lastEntry();
      if (lastEntry == null) {
        return null;
      }
      newMap = oldMap.removeAndCopy(lastEntry.getKey());
    } while (!map.compareAndSet(oldMap, newMap));

    return lastEntry;
  }

  /**
   * This method is not atomic! It inserts all keys one after the other, and in between each
   * operation arbitrary operations from other threads might get executed.
   *
   * <p>This method is not starvation free, and thus not strictly guaranteed to terminate in
   * presence of concurrent modifying operations.
   *
   * @see Map#putAll(Map)
   */
  @Override
  public void putAll(Map<? extends K, ? extends V> pMap) {
    pMap.forEach((key, value) -> put(key, value));
  }

  /** See {@link Map#clear()}. */
  @Override
  public void clear() {
    map.set(map.get().empty());
  }

  // Override the collection view methods
  // so that they return also live views and not immutable snapshots.

  @Override
  public NavigableSet<Map.Entry<K, V>> entrySet() {
    return new ForwardingNavigableSet<>() {

      @Override
      protected NavigableSet<Map.Entry<K, V>> delegate() {
        return map.get().entrySet();
      }
    };
  }

  @Override
  public NavigableSet<K> keySet() {
    return new ForwardingNavigableSet<>() {

      @Override
      protected NavigableSet<K> delegate() {
        return map.get().keySet();
      }
    };
  }

  @Override
  public Collection<V> values() {
    return new ForwardingCollection<>() {

      @Override
      protected Collection<V> delegate() {
        return map.get().values();
      }
    };
  }

  @Override
  @SuppressWarnings("CheckReturnValue")
  public NavigableMap<K, V> headMap(K pToKey) {
    map.get().headMap(pToKey); // for bounds check
    return new ForwardingNavigableMap<>() {

      @Override
      protected NavigableMap<K, V> delegate() {
        return map.get().headMap(pToKey);
      }
    };
  }

  @Override
  @SuppressWarnings("CheckReturnValue")
  public NavigableMap<K, V> tailMap(K pFromKey) {
    map.get().tailMap(pFromKey); // for bounds check
    return new ForwardingNavigableMap<>() {

      @Override
      protected NavigableMap<K, V> delegate() {
        return map.get().tailMap(pFromKey);
      }
    };
  }

  @Override
  @SuppressWarnings("CheckReturnValue")
  public NavigableMap<K, V> subMap(K pFromKey, K pToKey) {
    map.get().subMap(pFromKey, pToKey); // for bounds check
    return new ForwardingNavigableMap<>() {

      @Override
      protected NavigableMap<K, V> delegate() {
        return map.get().subMap(pFromKey, pToKey);
      }
    };
  }
}
