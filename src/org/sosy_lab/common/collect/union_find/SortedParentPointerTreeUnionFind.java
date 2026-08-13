// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class SortedParentPointerTreeUnionFind<T> extends ParentPointerTreeUnionFind<T> {

  // subsets are in order of their canonical elements; elements in subsets are sorted as well
  @Override
  public Collection<? extends Set<T>> getAllSubsets() {

    NavigableMap<T, ParentPointerTree<T>> forestSortedByKeys = new TreeMap<>(forest);
    List<Set<T>> allSubsets = new ArrayList<>();

    for (ParentPointerTree<T> tree : forestSortedByKeys.values()) {
      allSubsets.add(new TreeSet<>(tree.getSetOfNodeValues()));
    }

    return allSubsets;
  }
}
