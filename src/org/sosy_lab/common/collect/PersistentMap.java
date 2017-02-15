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

import com.google.errorprone.annotations.CompatibleWith;
import com.google.errorprone.annotations.Immutable;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.annotation.CheckReturnValue;

/**
 * Interface for persistent map. A persistent data structure is immutable, but provides cheap
 * copy-and-write operations. Thus all write operations ({{@link #putAndCopy(Object, Object)},
 * {{@link #removeAndCopy(Object)}}) will not modify the current instance, but return a new instance
 * instead.
 *
 * <p>All modifying operations inherited from {@link Map} are not supported and will always throw
 * {@link UnsupportedOperationException}. All collections returned by methods of this interface are
 * also immutable.
 *
 * <p>Instances of this interface are thread-safe as long as published safely.
 *
 * @param <K> The type of keys.
 * @param <V> The type of values.
 */
@Immutable(containerOf = {"K", "V"})
public interface PersistentMap<K, V> extends Map<K, V> {

  /** Replacement for {{@link #put(Object, Object)} that returns a fresh instance. */
  @CheckReturnValue
  PersistentMap<K, V> putAndCopy(@CompatibleWith("K") K key, @CompatibleWith("V") V value);

  /** Replacement for {{@link #remove(Object)} that returns a fresh instance. */
  @CheckReturnValue
  PersistentMap<K, V> removeAndCopy(@CompatibleWith("K") Object key);

  /** Replacement for {{@link #clear()} that returns an empty instance. */
  @CheckReturnValue
  PersistentMap<K, V> empty();

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  V put(K pKey, V pValue);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  V putIfAbsent(K pKey, V pValue);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  void putAll(Map<? extends K, ? extends V> pM);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  V remove(Object pKey);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  boolean remove(Object pKey, Object pValue);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  void clear();

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  V compute(K pKey, BiFunction<? super K, ? super V, ? extends V> pRemappingFunction);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  V computeIfAbsent(K pKey, Function<? super K, ? extends V> pMappingFunction);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  V computeIfPresent(K pKey, BiFunction<? super K, ? super V, ? extends V> pRemappingFunction);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  V replace(K pKey, V pValue);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  boolean replace(K pKey, V pOldValue, V pNewValue);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  void replaceAll(BiFunction<? super K, ? super V, ? extends V> pFunction);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  V merge(K pKey, V pValue, BiFunction<? super V, ? super V, ? extends V> pRemappingFunction);
}
