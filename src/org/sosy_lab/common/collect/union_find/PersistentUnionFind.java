// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.annotations.Immutable;

/**
 * Interface for a persistent union-find. A persistent data structure is immutable, but provides
 * cheap copy-and-write operations. Thus, all write operations ({@link #union(Object, Object)}) will
 * not modify the current instance, but return a new instance instead.
 *
 * <p>All modifying operations inherited from {@link UnionFind} are not supported and will always
 * throw {@link UnsupportedOperationException}.
 *
 * @param <T> The type of values.
 */
@Immutable(containerOf = "T")
public interface PersistentUnionFind<T> extends UnionFind<T> {

  /**
   * Replacement for {@link #union(Object, Object)} that returns a fresh new instance.
   *
   * @param pE1 first element
   * @param pE2 second element
   * @return new instance that the desired changes have been applied to
   */
  @CheckReturnValue
  PersistentUnionFind<T> unionAndCopy(T pE1, T pE2);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  void union(T pE1, T pE2);
}
