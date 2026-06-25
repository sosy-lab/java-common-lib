// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.annotations.Immutable;
import java.util.Map;
import java.util.NavigableSet;

/**
 * Interface for a persistent and sorted union-find. A persistent data structure is immutable, but
 * provides cheap copy-and-write operations. Thus, all write operations ({@link #union(Comparable,
 * Comparable)}) will not modify the current instance, but return a new instance instead.
 *
 * <p>All modifying operations inherited from {@link SortedUnionFind} are not supported and will
 * always throw {@link UnsupportedOperationException}.
 *
 * @param <T> The type of values.
 */
@Immutable(containerOf = "T")
public interface PersistentSortedUnionFind<T extends Comparable<T>> extends SortedUnionFind<T> {

  /**
   * Replacement for {@link #union(Comparable, Comparable)} that returns a fresh new instance.
   *
   * @param e1 first element
   * @param e2 second element
   * @return new instance that the desired changes have been applied to
   */
  @CheckReturnValue
  Map<T, NavigableSet<T>> unionAndCopy(T e1, T e2);

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  void union(T e1, T e2);
}
