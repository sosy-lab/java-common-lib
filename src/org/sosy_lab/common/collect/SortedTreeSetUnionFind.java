// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.errorprone.annotations.Var;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

public class SortedTreeSetUnionFind<T> implements SortedUnionFind<T> {

  private final Map<T, NavigableSet<T>> setOfSets;

  public SortedTreeSetUnionFind() {
    setOfSets = new HashMap<>();
  }

  @Override
  public T find(T e) {
    for (NavigableSet<T> current : setOfSets.values()) {
      if (current.contains(e)) {
        for (T element : current) {
          if (setOfSets.containsKey(element)) {
            return element;
          }
        }
      }
    }

    throw new IllegalArgumentException("Element not contained");
  }

  /*
  USE
  - add new element to own new set: e1 and e2 both element to be added
  - add new element to existing set: one e new element, other e canon. elem. of set to add to
  - merge two existing sets: e1 and e2 canon. elem.s of sets to be merged
   */
  @Override
  public void union(T e1, T e2) {
    if (e1.equals(e2)) {
      addElementAsNewSet(e1);
    } else {
      Set<T> canonicalElements = setOfSets.keySet();

      if (canonicalElements.contains(e1)) {
        if (canonicalElements.contains(e2)) {
          mergeExistingSets(e1, e2);
        } else {
          addElementToExistingSet(e2, e1);
        }
      } else if (canonicalElements.contains(e2)) {
        addElementToExistingSet(e1, e2);
      } else {
        addElementAsNewSet(e1);
        addElementToExistingSet(e2, e1);
      }
    }
  }

  private void addElementAsNewSet(T e) {
    if (!contains(e)) {
      NavigableSet<T> newSet = new TreeSet<>();
      newSet.add(e);
      setOfSets.put(e, newSet);
    } else {
      throw new IllegalArgumentException("Element already contained");
    }
  }

  private void addElementToExistingSet(T e, T canon) {
    if (!contains(e)) {
      for (NavigableSet<T> currentSet : setOfSets.values()) {
        if (currentSet.contains(canon)) {
          currentSet.add(e);
          setOfSets.replace(canon, currentSet);
          break;
        }
      }
    } else {
      throw new IllegalArgumentException("Element already contained");
    }
  }

  private void mergeExistingSets(T e1, T e2) {
    @Var NavigableSet<T> set1 = null;
    @Var NavigableSet<T> set2 = null;

    for (NavigableSet<T> current : setOfSets.values()) {
      if (current.contains(e1)) {
        set1 = current;
      } else if (current.contains(e2)) {
        set2 = current;
      }
    }

    assert set1 != null;
    assert set2 != null;

    int size1 = set1.size();
    int size2 = set2.size();

    // TODO potential problem: this could cause canon elem to not be the same as before (even though
    // it needs to be)
    if (size1 > size2) {
      set1.addAll(set2);
      setOfSets.remove(e2);
    } else {
      set2.addAll(set1);
      setOfSets.remove(e1); // TODO it seems removal doesn't actually take place though it should
    }
  }

  @Override
  public Collection<? extends Set<T>> getAllSubsets() {
    return setOfSets.values();
  }

  @Override
  public boolean contains(T e) {
    for (NavigableSet<T> current : setOfSets.values()) {
      if (current.contains(e)) {
        return true;
      }
    }
    return false;
  }
}
