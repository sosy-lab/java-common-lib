// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import java.util.Collection;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class SortedParentPointerTreeUnionFind<T extends Comparable<T>>
    extends ParentPointerTreeUnionFind<T> {

  /**
   * Provides a {@link Collection} containing all current subsets. It contains the subsets sorted by
   * their canonical elements in ascending order. The contents of the subsets are also sorted in
   * ascending order.
   *
   * @return sorted {@link Collection} containing all current subsets
   */
  // subsets are in order of their canonical elements; elements in subsets are sorted as well
  @Override
  public Collection<? extends Set<T>> getAllSubsets() {

    NavigableMap<T, Set<T>> allSubsets = new TreeMap<>();

    for (AbstractTreeNode<T> node : allNodes.values()) {

      T canon = find(node.getValue());

      if (allSubsets.containsKey(canon)) {
        allSubsets.get(canon).add(node.getValue());
      } else {
        NavigableSet<T> set = new TreeSet<>();
        set.add(node.getValue());
        allSubsets.put(canon, set);
      }
    }

    return allSubsets.values();
  }
}
