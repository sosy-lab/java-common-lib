// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import java.util.HashSet;
import java.util.Set;

public class SortedUnionFind<T> implements UnionFind<T> {

  private HashSet setOfSets; // TODO figure out type

  private SortedUnionFind() {
    // TODO
  }

  @Override
  public T find(T e) {
    return null;
  }

  @Override
  public void union(T e1, T e2) {}

  @Override
  public UnionFind<T> getEmptyInstanceOf() {
    return null;
  }

  @Override
  public void addSetOfSets(Set set) {}

  @Override
  public void addElementToNewSet(T e) {}

  @Override
  public Set getAllSubsets() {
    return Set.of();
  }
}
