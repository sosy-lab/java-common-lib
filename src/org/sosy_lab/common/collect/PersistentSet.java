// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.CompatibleWith;
import com.google.errorprone.annotations.DoNotCall;
import java.util.Collection;
import java.util.Set;

/**
 * Defines a persistent {@link Set}. A persistent data structure is immutable, but provides cheap
 * copy-and-write operations. Thus, all write operations ({{@link #addAndCopy(Comparable)}, {{@link
 * #removeAndCopy(Object)}}) will not modify the current instance, but return a new instance
 * instead.
 *
 * <p>All modifying operations inherited from {@link Set} are not supported and will always throw
 * {@link UnsupportedOperationException}. All collections returned by methods of this interface are
 * also immutable.
 *
 * <p>Instances of this interface are thread-safe as long as published safely.
 *
 * @param <E> The type of elements in the set.
 */
public interface PersistentSet<E extends Comparable<? super E>> extends Set<E> {

  /**
   * Replacement for Constructor that returns a fresh instance based on the given {@link
   * PersistentMap}. The characteristics of the returned {@link PersistentSet} are based on the used
   * {@link PersistentMap}. If the given {@link PersistentMap} is serializable, the returned {@link
   * PersistentSet} is also serializable.
   */
  @CheckReturnValue
  default PersistentSet<E> newSetFromMap(PersistentMap<E, Boolean> mapForSet) {
    // All of our maps are sorted. Let's change this once we have unsorted ones.
    checkArgument(mapForSet instanceof PersistentSortedMap);
    return new OurPersistentSetFromPersistentMap<>((PersistentSortedMap<E, Boolean>) mapForSet);
  }

  /**
   * Replacement for {{@link #add(Comparable)} that returns a fresh instance of the current {@link
   * PersistentSet} with a fresh instance of the same background {@link PersistentMap}.
   */
  @CheckReturnValue
  PersistentSet<E> addAndCopy(@CompatibleWith("E") E e);

  /** Replacement for {{@link #remove(Object)} that returns a fresh instance. */
  @CheckReturnValue
  PersistentSet<E> removeAndCopy(@CompatibleWith("E") Object e);

  /** Replacement for {{@link #clear()} that returns an empty instance. */
  @CheckReturnValue
  PersistentSet<E> empty();

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  boolean add(E var1);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  boolean remove(Object var1);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  boolean addAll(Collection<? extends E> var1);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  boolean retainAll(Collection<?> var1);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  boolean removeAll(Collection<?> var1);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  @DoNotCall
  void clear();
}
