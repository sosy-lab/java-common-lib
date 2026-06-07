// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class UnsortedUnionFind implements UnionFind {

  /*
   * CURRENT PROBLEMS:
   * - trying to keep set type as flexible as possible but finding certain things can't be implemented without deciding on type
   * - currently using HashSet
   * - considering using set of maps instead --> map each element of a disjoint set to canonical element of set (but then need to ensure duplicate elements do not occur)
   * - not sure ArrayList is ideal for canonical elements
   * - addSetOfSets might be dangerous as it relies on user providing set with the correct type of values
   * - was trying to make one class work for both sorted and unsorted (defined by type of set user provides) but it's looking like they're going to be separate and this will be unsorted
   */
  private HashSet setOfMaps;
  private ArrayList canonicalElements;

  private UnsortedUnionFind() {
    setOfMaps = new HashSet<>();
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
    return new UnsortedUnionFind();
  }

  @Override
  public void addSetOfSets(Set set) {
    //TODO
    //problem: extracting canonical elements to add to canonicalElements with current HashSet situation
  }

  @Override
  public <T> void addElementToNewSet(T e) {
    //TODO check whether new element already in structure and handle case where it is
    HashMap<T,T> newMap = new HashMap<>();
    newMap.put(e, e);

    setOfMaps.add(newMap);
    canonicalElements.add(e);
  }

  @Override
  public Set getAllSubsets() {
    return setOfMaps;
  }
}
