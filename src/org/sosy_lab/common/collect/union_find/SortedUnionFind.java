// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import java.util.Collection;
import java.util.NavigableSet;
import java.util.Set;

/**
 * Interface for a sorted Union-Find or Disjoint-Set data structure. Uses a {@link Collection} of
 * {@link Set}s.
 *
 * @param <T> type of elements added to the Union-Find. Must be {@link Comparable} to ensure correct
 *     ordering.
 */
public interface SortedUnionFind<T extends Comparable<T>> extends UnionFind<T> {

  /**
   * Provides a {@link Collection} containing all current subsets.
   *
   * @return {@link Collection} containing all current subsets
   */
  @Override
  Collection<? extends NavigableSet<T>> getAllSubsets();
}
