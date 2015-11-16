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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Implementation of {@link SortedSet} to be used as the key set of a {@link SortedMap}.
 *
 * This implementation forwards all methods to the underlying map.
 *
 * @param <K> The type of keys.
 */
class SortedMapKeySet<K extends Comparable<? super K>> extends AbstractSet<K>
    implements SortedSet<K> {

  private final SortedMap<K, ?> map;

  SortedMapKeySet(SortedMap<K, ?> pMap) {
    map = checkNotNull(pMap);
  }

  @Override
  public Iterator<K> iterator() {
    return Iterators.transform(
        map.entrySet().iterator(),
        new Function<Map.Entry<K, ?>, K>() {
          @Override
          public K apply(Map.Entry<K, ?> input) {
            return input.getKey();
          }
        });
  }

  @Override
  public boolean contains(Object pO) {
    return map.containsKey(pO);
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public Comparator<? super K> comparator() {
    return map.comparator();
  }

  @Override
  public K first() {
    return map.firstKey();
  }

  @Override
  public K last() {
    return map.lastKey();
  }

  @Override
  public SortedSet<K> subSet(K pFromElement, K pToElement) {
    return new SortedMapKeySet<>(map.subMap(pFromElement, pToElement));
  }

  @Override
  public SortedSet<K> headSet(K pToElement) {
    return new SortedMapKeySet<>(map.headMap(pToElement));
  }

  @Override
  public SortedSet<K> tailSet(K pFromElement) {
    return new SortedMapKeySet<>(map.tailMap(pFromElement));
  }
}
