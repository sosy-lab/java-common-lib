// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
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
   * @param pE element for which set is to be found
   * @return canonical element of the found set
   * @throws IllegalArgumentException if element is not contained in any subset
   */
  @Override
  public T find(T pE) {

    Preconditions.checkNotNull(pE);

    for (Entry<T, S> mapping : mapOfSets.entrySet()) {
      if (mapping.getValue().contains(pE)) {
        return mapping.getKey();
      }
    }

    throw new IllegalArgumentException("Element not contained");
  }

  /**
   * Merges the sets represented by the two input values according to standard Union-Find behaviour.
   *
   * <p>USES: Add new element as new set: pass it as both pE1 and pE2. Add new element to existing
   * set: one input value is the new element, the other the canonical element of the set to be added
   * to. Merge two existing sets: pE1, pE2 canonical elements of sets to be merged.
   *
   * @param pE1 first element
   * @param pE2 second element
   */
  @Override
  public void union(T pE1, T pE2) {

    Preconditions.checkNotNull(pE1);
    Preconditions.checkNotNull(pE2);

    if (pE1.equals(pE2)) {
      addElementAsNewSet(pE1);
    } else {
      Set<T> canonicalElements = mapOfSets.keySet();

      if (canonicalElements.contains(pE1)) {
        if (canonicalElements.contains(pE2)) {
          mergeExistingSets(pE1, pE2);
        } else {
          addElementToExistingSet(pE2, pE1);
        }
      } else if (canonicalElements.contains(pE2)) {
        addElementToExistingSet(pE1, pE2);
      } else {

        if (contains(pE1)) {
          if (contains(pE2)) {
            mergeExistingSets(find(pE1), find(pE2));
          } else {
            addElementToExistingSet(pE2, find(pE1));
          }
        } else {
          addElementAsNewSet(pE1);
          addElementToExistingSet(pE2, pE1);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void addElementAsNewSet(T pE) {

    if (!contains(pE)) {
      S newSet = (S) getEmptySet();
      newSet.add(pE);
      mapOfSets.put(pE, newSet);
    }
  }

  private void addElementToExistingSet(T pE, T pCanon) {

    if (!contains(pE)) {
      mapOfSets.get(pCanon).add(pE);
    } else {
      mergeExistingSets(find(pE), pCanon);
    }
  }

  // pE1 will be new canonical element only if its set is actually bigger, otherwise pE2 new canon
  private void mergeExistingSets(T pE1, T pE2) {

    S set1 = mapOfSets.get(pE1);
    S set2 = mapOfSets.get(pE2);

    assert set1 != null;
    assert set2 != null;

    int size1 = set1.size();
    int size2 = set2.size();

    if (size1 > size2) {
      set1.addAll(set2);
      assert mapOfSets.remove(pE2, set2);
    } else {
      set2.addAll(set1);
      assert mapOfSets.remove(pE1, set1);
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
   * @param pE element to be searched for
   * @return true if contained, false if not
   */
  @Override
  public boolean contains(T pE) {

    Preconditions.checkNotNull(pE);

    for (S current : mapOfSets.values()) {
      if (current.contains(pE)) {
        return true;
      }
    }
    return false;
  }

  protected abstract Set<T> getEmptySet();

  protected abstract Map<T, Set<T>> getEmptyMap();
}
