/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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

import com.google.common.collect.Collections2;
import com.google.common.collect.ForwardingNavigableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import javax.annotation.Nullable;

@SuppressFBWarnings(
  value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE",
  justification = "nullability depends on underlying map"
)
final class DescendingSortedMap<K, V> extends ForwardingNavigableMap<K, V>
    implements OurSortedMap<K, V> {

  private final OurSortedMap<K, V> map;

  DescendingSortedMap(OurSortedMap<K, V> pMap) {
    map = checkNotNull(pMap);
  }

  @Override
  protected NavigableMap<K, V> delegate() {
    return map;
  }

  @Override
  public Iterator<Entry<K, V>> entryIterator() {
    return map.descendingEntryIterator();
  }

  @Override
  public Iterator<Entry<K, V>> descendingEntryIterator() {
    return map.entryIterator();
  }

  @Override
  public @Nullable Entry<K, V> getEntry(@Nullable Object pKey) {
    return map.getEntry(pKey);
  }

  @Override
  public Comparator<? super K> comparator() {
    return Collections.reverseOrder(map.comparator());
  }

  @Override
  public K firstKey() {
    return map.lastKey();
  }

  @Override
  public K lastKey() {
    return map.firstKey();
  }

  @Override
  public NavigableSet<Entry<K, V>> entrySet() {
    return new SortedMapEntrySet<>(this);
  }

  @Override
  public NavigableSet<K> keySet() {
    return navigableKeySet();
  }

  @Override
  public NavigableSet<K> navigableKeySet() {
    return new SortedMapKeySet<>(this);
  }

  @Override
  public NavigableSet<K> descendingKeySet() {
    return map.navigableKeySet();
  }

  @Override
  public OurSortedMap<K, V> descendingMap() {
    return map;
  }

  @Override
  public Collection<V> values() {
    return Collections2.transform(entrySet(), Entry::getValue);
  }

  @Override
  public Entry<K, V> lowerEntry(@Nullable K pKey) {
    return map.higherEntry(pKey);
  }

  @Override
  public K lowerKey(@Nullable K pKey) {
    return map.higherKey(pKey);
  }

  @Override
  public Entry<K, V> floorEntry(@Nullable K pKey) {
    return map.ceilingEntry(pKey);
  }

  @Override
  public K floorKey(@Nullable K pKey) {
    return map.ceilingKey(pKey);
  }

  @Override
  public Entry<K, V> ceilingEntry(@Nullable K pKey) {
    return map.floorEntry(pKey);
  }

  @Override
  public K ceilingKey(@Nullable K pKey) {
    return map.floorKey(pKey);
  }

  @Override
  public Entry<K, V> higherEntry(@Nullable K pKey) {
    return map.lowerEntry(pKey);
  }

  @Override
  public K higherKey(@Nullable K pKey) {
    return map.lowerKey(pKey);
  }

  @Override
  public Entry<K, V> firstEntry() {
    return map.lastEntry();
  }

  @Override
  public Entry<K, V> lastEntry() {
    return map.firstEntry();
  }

  @Override
  public Entry<K, V> pollFirstEntry() {
    return map.pollLastEntry();
  }

  @Override
  public Entry<K, V> pollLastEntry() {
    return map.pollFirstEntry();
  }

  @Override
  public OurSortedMap<K, V> subMap(
      @Nullable K pFromKey, boolean pFromInclusive, @Nullable K pToKey, boolean pToInclusive) {
    return map.subMap(
            pToKey, /*pFromInclusive=*/ pToInclusive, pFromKey, /*pToInclusive=*/ pFromInclusive)
        .descendingMap();
  }

  @Override
  public OurSortedMap<K, V> headMap(@Nullable K pToKey, boolean pInclusive) {
    return map.tailMap(pToKey, /*pInclusive=*/ pInclusive).descendingMap();
  }

  @Override
  public OurSortedMap<K, V> tailMap(@Nullable K pFromKey, boolean pInclusive) {
    return map.headMap(pFromKey, /*pInclusive=*/ pInclusive).descendingMap();
  }

  @Override
  public OurSortedMap<K, V> headMap(@Nullable K pToKey) {
    return headMap(pToKey, /*pInclusive=*/ false);
  }

  @Override
  public OurSortedMap<K, V> tailMap(@Nullable K pFromKey) {
    return tailMap(pFromKey, /*pInclusive=*/ true);
  }

  @Override
  public OurSortedMap<K, V> subMap(@Nullable K pFromKey, @Nullable K pToKey) {
    return subMap(pFromKey, /*pFromInclusive=*/ true, pToKey, /*pToInclusive=*/ false);
  }

  @Override
  public String toString() {
    return standardToString();
  }
}
