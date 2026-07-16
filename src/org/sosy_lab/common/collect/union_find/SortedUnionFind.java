// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import java.util.Collection;
import java.util.Set;

/**
 * Interface for a sorted Union-Find or Disjoint-Set data structure. Uses a {@link Collection} of
 * {@link Set}s.
 *
 * @param <T> type of elements added to the Union-Find. Must be {@link Comparable} to ensure correct
 *     ordering.
 */
public interface SortedUnionFind<T extends Comparable<T>> {
  /**
   * Returns the canonical element of the set containing the provided element.
   *
   * @param e element for which set is to be found
   * @return canonical element of the found set
   */
  T find(T e);

  /**
   * Merges the sets represented by the two input values according to standard Union-Find behaviour.
   *
   * @param e1 first element
   * @param e2 second element
   */
  void union(T e1, T e2);

  /**
   * Provides a {@link Collection} containing all current subsets.
   *
   * @return {@link Collection} containing all current subsets
   */
  Collection<? extends Set<T>> getAllSubsets();

  /**
   * Checks whether the provided element is contained in any current subset and returns true or
   * false accordingly.
   *
   * @param e element to be searched for
   * @return true if contained, false if not
   */
  boolean contains(T e);
}
