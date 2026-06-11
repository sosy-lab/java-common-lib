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
import java.util.TreeSet;

public class SortedTreeSetUnionFind<T> implements UnionFind<T> {

  private HashSet<TreeSet<T>> setOfSets;

  private SortedTreeSetUnionFind() {
    // TODO

    setOfSets = new HashSet<>();
  }

  @Override
  public T find(T e) {
    // TODO
    return null;
  }

  /*
  USE
  - add new element to own new set: e1 and e2 both element to be added
  - add new element to existing set: one e new element, other e canon. elem. of set to add to
  - merge two existing sets: e1 and e2 canon. elem.s of sets to be merged
   */
  @Override
  public void union(T e1, T e2) throws IllegalArgumentException {
    if (e1.equals(e2)) {
      addElementAsNewSet(e1);
    } else {
      ArrayList<T> canonicalElements = getListOfCanonicalElements();

      if (canonicalElements.contains(e1)) {
        if (canonicalElements.contains(e2)) {
          mergeExistingSets(e1, e2);
        } else {
          addElementToExistingSet(e2, e1);
        }
      } else if (canonicalElements.contains(e2)) {
        addElementToExistingSet(e1, e2);
      }
    }
  }

  private void addElementAsNewSet(T e) throws IllegalArgumentException {
    if (!contains(e)) {
      TreeSet<T> newSet = new TreeSet<>();
      newSet.add(e);
      setOfSets.add(newSet);
    } else {
      throw new IllegalArgumentException("Element already contained");
    }
  }

  private void addElementToExistingSet(T e, T canon) throws IllegalArgumentException {
    if (!contains(e)) {
      for (TreeSet<T> treeSet : setOfSets) {
        if (treeSet.first().equals(canon)) {
          treeSet.add(e);
          break;
        }
      }
    } else {
      throw new IllegalArgumentException("Element already contained");
    }
  }

  private void mergeExistingSets(T e1, T e2) {
    TreeSet<T> set1;
    TreeSet<T> set2;
    int size1;
    int size2;

    for (TreeSet<T> current : setOfSets) {
      if (current.first().equals(e1)) {
        set1 = current;
        size1 = set1.size();
      } else if (current.first().equals(e2)) {
        set2 = current;
        size2 = set2.size();
      }
    }

    // TODO potential problem: this could cause canon elem to not be the same as before (even though it needs to be)
    if (size1 > size2) {
      for (T current : set2) {
        set1.add(current);
      }
      setOfSets.remove(set2);
    } else {
      for (T current : set1) {
        set2.add(current);
      }
      setOfSets.remove(set1);
    }
  }

  private ArrayList<T> getListOfCanonicalElements() {
    ArrayList<T> list = new ArrayList<>();

    for (TreeSet<T> treeSet : setOfSets) {
      list.add(treeSet.first());
    }

    return list;
  }

  @Override
  public UnionFind<T> getEmptyInstanceOf() {
    return new SortedTreeSetUnionFind<>();
  }

  @Override
  public Set<TreeSet<T>> getAllSubsets() {
    return setOfSets;
  }

  // consider making public and adding to interface
  private boolean contains(T e) {
    for (TreeSet<T> current : setOfSets) {
      if (current.contains(e)) {
        return true;
      }
    }
    return false;
  }
}
