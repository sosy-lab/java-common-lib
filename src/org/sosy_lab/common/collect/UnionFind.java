// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import java.util.Set;

public interface UnionFind {
  <T> T find(T e);
  <T> void union(T e1, T e2);

  UnionFind getEmptyUnionFind();
  void addSetOfSets(Set set);
  <T> void addElementToNewSet(T e);
  Set getAllSubsets();
}