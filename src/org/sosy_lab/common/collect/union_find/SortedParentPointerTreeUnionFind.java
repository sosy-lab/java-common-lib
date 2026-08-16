// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A sorted implementation of {@link UnionFind} using a {@link Map} of {@link AbstractTreeNode}s. In
 * order to represent subsets by canonical elements, each one is mapped to its representative
 * canonical element. This is always the first element added to the subset, unless it has changed
 * due to union operations. Each subset is stored as a parent pointer tree comprised of {@link
 * NonRootNode}s with exactly one {@link RootNode} as the root. The union can be performed either by
 * size or by rank, determined by a constructor parameter.
 *
 * @param <T> type of elements added to the Union-Find. Must be comparable.
 */
public class SortedParentPointerTreeUnionFind<T extends Comparable<T>>
    extends ParentPointerTreeUnionFind<T> implements SortedUnionFind<T> {

  /**
   * Creates an empty instance.
   *
   * @param unionType type of union to be performed for all unions on this instance
   */
  public SortedParentPointerTreeUnionFind(UnionType unionType) {
    super(unionType);
  }

  /**
   * Provides a {@link Collection} containing all current subsets. It contains the subsets sorted by
   * their canonical elements in ascending order. The contents of the subsets are also sorted in
   * ascending order.
   *
   * @return sorted {@link Collection} containing all current subsets
   */
  // subsets are in order of their canonical elements; elements in subsets are sorted as well
  @Override
  public Collection<? extends NavigableSet<T>> getAllSubsets() {

    NavigableMap<T, NavigableSet<T>> allSubsets = new TreeMap<>();

    for (AbstractTreeNode<T> node : allNodes.values()) {

      T canon = find(node.getValue());

      if (allSubsets.containsKey(canon)) {
        allSubsets.get(canon).add(node.getValue());
      } else {
        NavigableSet<T> set = new TreeSet<>();
        set.add(canon);
        set.add(node.getValue());
        allSubsets.put(canon, set);
      }
    }

    return allSubsets.values();
  }
}
