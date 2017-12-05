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

import com.google.errorprone.annotations.Immutable;
import java.util.SortedMap;
import java.util.SortedSet;
import javax.annotation.CheckReturnValue;

/**
 * Sub-interface of {@link PersistentMap} analog to {@link SortedMap}.
 *
 * @param <K> The type of keys.
 * @param <V> The type of values.
 */
@Immutable(containerOf = {"K", "V"})
@SuppressWarnings("JdkObsolete")
public interface PersistentSortedMap<K, V>
    extends PersistentMap<K, V>, SortedMap<K, V>, OurSortedMap<K, V> {

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
  SortedSet<Entry<K, V>> entrySet();

  @Override
  SortedSet<K> keySet();
}
