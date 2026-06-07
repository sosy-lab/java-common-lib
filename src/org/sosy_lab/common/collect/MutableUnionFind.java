// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MutableUnionFind implements UnionFind {

  private Set setOfSets;
  private ArrayList canonicalElements;

  private MutableUnionFind() {
    setOfSets = new HashSet<>();
    canonicalElements = new ArrayList<>();
  }

  @Override
  public <T> T find(T e) {
    //TODO
  }

  @Override
  public <T> void union(T e1, T e2) {
    //TODO
  }

  @Override
  public UnionFind getEmptyInstanceOf() {
    return new MutableUnionFind();
  }

  @Override
  public void addSetOfSets(Set set) {
    //TODO
  }

  @Override
  public <T> void addElementToNewSet(T e) {
    //TODO
  }

  @Override
  public Set getAllSubsets() {
    return setOfSets;
  }
}
