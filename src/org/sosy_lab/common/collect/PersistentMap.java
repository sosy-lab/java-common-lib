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

import java.util.Map;

import javax.annotation.CheckReturnValue;

/**
 * Interface for persistent map.
 * A persistent data structure is immutable, but provides cheap copy-and-write
 * operations. Thus all write operations ({{@link #putAndCopy(Object, Object)}, {{@link #removeAndCopy(Object)}})
 * will not modify the current instance, but return a new instance instead.
 *
 * All modifying operations inherited from {@link Map} are not supported and
 * will always throw {@link UnsupportedOperationException}.
 * All collections returned by methods of this interface are also immutable.
 *
 * Instances of this interface are thread-safe as long as published safely.
 *
 * @param <K> The type of keys.
 * @param <V> The type of values.
 */
public interface PersistentMap<K, V> extends Map<K, V> {

  @CheckReturnValue
  PersistentMap<K, V> putAndCopy(K key, V value);

  @CheckReturnValue
  PersistentMap<K, V> removeAndCopy(K key);

  /**
   * @throws UnsupportedOperationException Always.
   */
  @Override
  @Deprecated
  V put(K pKey, V pValue) throws UnsupportedOperationException;

  /**
   * @throws UnsupportedOperationException Always.
   */
  @Override
  @Deprecated
  void putAll(Map<? extends K, ? extends V> pM) throws UnsupportedOperationException;

  /**
   * @throws UnsupportedOperationException Always.
   */
  @Override
  @Deprecated
  V remove(Object pKey) throws UnsupportedOperationException;

  /**
   * @throws UnsupportedOperationException Always.
   */
  @Override
  @Deprecated
  public void clear() throws UnsupportedOperationException;
}