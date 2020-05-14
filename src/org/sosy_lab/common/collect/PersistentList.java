// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Immutable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Interface for persistent lists. A persistent data structure is immutable, but provides cheap
 * copy-and-write operations. Thus all write operations ({{@link #with(Object)}, {{@link
 * #without(Object)}}) will not modify the current instance, but return a new instance instead.
 *
 * <p>All modifying operations inherited from {@link List} are not supported and will always throw
 * {@link UnsupportedOperationException}.
 *
 * <p>Instances of this interface are thread-safe as long as published safely.
 *
 * @param <T> The type of values.
 */
@Immutable(containerOf = "T")
public interface PersistentList<T> extends List<T> {

  /**
   * Replacement for {@link #add(Object)} that returns a fresh new instance. The position of
   * insertion is not specified.
   */
  @CheckReturnValue
  PersistentList<T> with(T value);

  /**
   * Replacement for {@link #addAll(Collection)} that returns a fresh new instance. The position of
   * insertion is not specified.
   */
  @CheckReturnValue
  PersistentList<T> withAll(List<T> values);

  /**
   * Replacement for {@link #remove(Object)} that returns a fresh new instance. If the value occurs
   * several times, only the first occurrence is removed.
   */
  @CheckReturnValue
  PersistentList<T> without(T value);

  /** Replacement for {{@link #clear()} that returns an empty instance. */
  @CheckReturnValue
  PersistentList<T> empty();

  /** Returns a new list with the elements in the reverse order. */
  @CheckReturnValue
  PersistentList<T> reversed();

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  boolean add(T pE);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  void add(int pIndex, T pElement);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  boolean addAll(Collection<? extends T> pC);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  boolean addAll(int pIndex, Collection<? extends T> pC);

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
  T remove(int pIndex);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  boolean remove(Object pO);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  boolean removeAll(Collection<?> pC);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  void replaceAll(UnaryOperator<T> pOperator);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  boolean retainAll(Collection<?> pC);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  T set(int pIndex, T pElement);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Override
  @Deprecated
  void sort(Comparator<? super T> pC);
}
