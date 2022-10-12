// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.CompatibleWith;
import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.annotations.Immutable;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

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
  @DoNotCall
  V put(K pKey, V pValue);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  V putIfAbsent(K pKey, V pValue);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  void putAll(Map<? extends K, ? extends V> pM);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  V remove(Object pKey);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  boolean remove(Object pKey, Object pValue);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  void clear();

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  V compute(K pKey, BiFunction<? super K, ? super V, ? extends V> pRemappingFunction);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  V computeIfAbsent(K pKey, Function<? super K, ? extends V> pMappingFunction);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  V computeIfPresent(K pKey, BiFunction<? super K, ? super V, ? extends V> pRemappingFunction);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  V replace(K pKey, V pValue);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  boolean replace(K pKey, V pOldValue, V pNewValue);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  void replaceAll(BiFunction<? super K, ? super V, ? extends V> pFunction);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  V merge(K pKey, V pValue, BiFunction<? super V, ? super V, ? extends V> pRemappingFunction);
}
