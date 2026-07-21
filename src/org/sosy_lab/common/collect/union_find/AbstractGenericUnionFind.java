// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.Var;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * An abstract, generic implementation of {@link UnionFind} using a {@link Map} of {@link Set}s. In
 * order to represent subsets by canonical elements, each one is mapped to its representative
 * canonical element. This is always the first element added to the subset, unless it has changed
 * due to union operations. The union is implemented as union by size.
 *
 * @param <T> type of elements added to the Union-Find.
 */
public abstract class AbstractGenericUnionFind<T, S extends Set<T>, M extends Map<T, S>>
    implements UnionFind<T> {

  protected final M mapOfSets;

  /**
   * Takes an empty map of the desired kind and allocates it to the variable mapOfSets. This enables
   * child classes to simply pass an object of the desired kind without having to modify the
   * constructor and methods.
   */
  @SuppressWarnings("unchecked")
  public AbstractGenericUnionFind() {
    mapOfSets = (M) getEmptyMap();
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

    Preconditions.checkNotNull(e);
    for (S current : mapOfSets.values()) {
      if (current.contains(e)) {
        for (T element : current) {
          if (mapOfSets.containsKey(element)) {
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
  @SuppressWarnings("unchecked")
  @Override
  public void union(T e1, T e2) {

    Preconditions.checkNotNull(e1);
    Preconditions.checkNotNull(e2);

    if (e1.equals(e2)) {
      addElementAsNewSet(e1);
    } else {
      S canonicalElements = (S) mapOfSets.keySet();

      if (canonicalElements.contains(e1)) {
        if (canonicalElements.contains(e2)) {
          mergeExistingSets(e1, e2);
        } else {
          addElementToExistingSet(e2, e1);
        }
      } else if (canonicalElements.contains(e2)) {
        addElementToExistingSet(e1, e2);
      } else {

        if (contains(e1)) {
          if (contains(e2)) {
            mergeExistingSets(find(e1), find(e2));
          } else {
            addElementToExistingSet(e2, find(e1));
          }
        } else {
          addElementAsNewSet(e1);
          addElementToExistingSet(e2, e1);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void addElementAsNewSet(T e) {

    if (!contains(e)) {
      S newSet = (S) getEmptySet();
      newSet.add(e);
      mapOfSets.put(e, newSet);
    }
  }

  private void addElementToExistingSet(T e, T canon) {

    if (!contains(e)) {
      for (S currentSet : mapOfSets.values()) {
        if (currentSet.contains(canon)) {
          assert mapOfSets.remove(canon, currentSet);
          currentSet.add(e);
          mapOfSets.put(canon, currentSet);
          break;
        }
      }
    } else {
      mergeExistingSets(find(e), canon);
    }
  }

  private void mergeExistingSets(T e1, T e2) {

    @Var S set1 = null;
    @Var S set2 = null;

    for (S current : mapOfSets.values()) {
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

    assert mapOfSets.remove(e1, set1);
    assert mapOfSets.remove(e2, set2);

    if (size1 > size2) {
      set1.addAll(set2);
      mapOfSets.put(e1, set1);
    } else {
      set2.addAll(set1);
      mapOfSets.put(e2, set2);
    }
  }

  /**
   * Provides a {@link Collection} containing all current subsets.
   *
   * @return {@link Collection} containing all current subsets
   */
  @Override
  public Collection<S> getAllSubsets() {
    return mapOfSets.values();
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

    Preconditions.checkNotNull(e);

    for (S current : mapOfSets.values()) {
      if (current.contains(e)) {
        return true;
      }
    }
    return false;
  }

  protected abstract Set<T> getEmptySet();

  protected abstract Map<T, Set<T>> getEmptyMap();
}
