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
import java.util.List;

import javax.annotation.CheckReturnValue;

/**
 * Interface for persistent lists.
 * A persistent data structure is immutable, but provides cheap copy-and-write
 * operations. Thus all write operations ({{@link #with(Object)}, {{@link #withOut(Object)}})
 * will not modify the current instance, but return a new instance instead.
 *
 * All modifying operations inherited from {@link List} are not supported and
 * will always throw {@link UnsupportedOperationException}.
 *
 * Instances of this interface are thread-safe as long as published safely.
 *
 * @param <T> The type of values.
 */
public interface PersistentList<T> extends List<T> {

  /**
   * Replacement for {@link #add(Object)} that returns a fresh new instance.
   */
  @CheckReturnValue
  PersistentList<T> with(T value);

  /**
   * Replacement for {@link #remove(Object)} that returns a fresh new instance.
   */
  @CheckReturnValue
  PersistentList<T> without(T value);

  /**
   * Replacement for {{@link #clear()} that returns an empty instance.
   */
  @CheckReturnValue
  PersistentList<T> empty();

  /**
   * Returns a new list with the elements in the reverse order.
   */
  @CheckReturnValue
  PersistentList<T> reversed();

  /**
   * @throws UnsupportedOperationException Always.
   */
  @Override
  @Deprecated
  public boolean add(T pE) throws UnsupportedOperationException;

  /**
   * @throws UnsupportedOperationException Always.
   */
  @Override
  @Deprecated
  void add(int pIndex, T pElement) throws UnsupportedOperationException;

  /**
   * @throws UnsupportedOperationException Always.
   */
  @Override
  @Deprecated
  public boolean addAll(Collection<? extends T> pC) throws UnsupportedOperationException;

  /**
   * @throws UnsupportedOperationException Always.
   */
  @Override
  @Deprecated
  public boolean addAll(int pIndex, Collection<? extends T> pC) throws UnsupportedOperationException;

  /**
   * @throws UnsupportedOperationException Always.
   */
  @Override
  @Deprecated
  public void clear() throws UnsupportedOperationException;

  /**
   * @throws UnsupportedOperationException Always.
   */
  @Override
  @Deprecated
  public T remove(int pIndex) throws UnsupportedOperationException;

  /**
   * @throws UnsupportedOperationException Always.
   */
  @Override
  @Deprecated
  public boolean remove(Object pO) throws UnsupportedOperationException;

  /**
   * @throws UnsupportedOperationException Always.
   */
  @Override
  @Deprecated
  public boolean removeAll(Collection<?> pC) throws UnsupportedOperationException;

  /**
   * @throws UnsupportedOperationException Always.
   */
  @Override
  @Deprecated
  public boolean retainAll(Collection<?> pC) throws UnsupportedOperationException;

  /**
   * @throws UnsupportedOperationException Always.
   */
  @Override
  @Deprecated
  public T set(int pIndex, T pElement) throws UnsupportedOperationException;
}