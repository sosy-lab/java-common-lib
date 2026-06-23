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

/**
 * An implementation of {@link SortedUnionFind} using a {@link HashMap} of {@link TreeSet}s. In
 * order to represent subsets by canonical elements, each one is mapped to its representative
 * canonical element. This is always the first element added to the subset, unless it has changed
 * due to union operations. The union is implemented as union by size.
 *
 * @param <T> type of elements added to the Union-Find. Must be {@link Comparable} to ensure correct
 *     ordering.
 */
public class SortedTreeSetUnionFind<T extends Comparable<T>> implements SortedUnionFind<T> {

  private final Map<T, NavigableSet<T>> setOfSets;

  /** Generates an empty {@link SortedTreeSetUnionFind}. */
  public SortedTreeSetUnionFind() {
    setOfSets = new HashMap<>();
  }

  /**
   * Returns the canonical element of the set containing the provided element.
   *
   * @param e element for which set is to be found
   * @return canonical element of the found set
   * @throws IllegalArgumentException if element is not contained in any subset
   */
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

  /**
   * Merges the sets represented by the two input values according to standard Union-Find behaviour.
   *
   * <p>USES: Add new element as new set: pass it as both e1 and e2. Add new element to existing
   * set: one input value is the new element, the other the canonical element of the set to be added
   * to. Merge two existing sets: e1, e2 canonical elements of sets to be merged.
   *
   * @param e1 first element
   * @param e2 second element
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

    if (size1 > size2) {
      set1.addAll(set2);
      setOfSets.remove(e2);
    } else {
      set2.addAll(set1);
      setOfSets.remove(e1);
    }
  }

  /**
   * Provides a {@link Collection} containing all current subsets.
   *
   * @return {@link Collection} containing all current subsets
   */
  @Override
  public Collection<? extends Set<T>> getAllSubsets() {
    return setOfSets.values();
  }

  /**
   * Checks whether the provided element is contained in any current subset and returns true or
   * false accordingly.
   *
   * @param e element to be searched for
   * @return true if contained, false if not
   */
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
