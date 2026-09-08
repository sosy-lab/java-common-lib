// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://www.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * A simple implementation of {@link UnionFind} using a {@link HashSet} of subsets.
 *
 * <p>The elements in a subset are stored in insertion order. Thus, its first element is its
 * canonical element. When two subsets are merged, the larger subset is retained and consequently
 * its canonical element remains canonical.
 *
 * <p>Let {@code n} be the number of elements, {@code s} the number of subsets, and {@code a} and
 * {@code b} the sizes of two subsets. Assuming expected constant-time hash table operations, {@link
 * #find(Object)} and {@link #contains(Object)} take {@code O(s)} expected time, which is {@code
 * O(n)} in the worst case. {@link #union(Object, Object)} takes {@code O(s + min(a, b))} expected
 * time when it merges two distinct subsets, and {@code O(s)} otherwise. {@link #getAllSubsets()}
 * takes {@code O(n)} expected time to create a snapshot.
 *
 * @param <T> type of elements added to the Union-Find
 */
public class SetOfSetsUnionFind<T> implements UnionFind<T> {

  private final Set<Set<T>> subsets = new HashSet<>();

  /** Creates an empty instance. */
  public SetOfSetsUnionFind() {}

  /**
   * Returns the canonical element of the subset containing the provided element.
   *
   * <p>Expected running time: {@code O(s)}, where {@code s} is the number of subsets.
   *
   * @throws IllegalArgumentException if the element is not contained in any subset
   */
  @Override
  public T find(T pE) {
    return getSubset(pE)
        .orElseThrow(() -> new IllegalArgumentException("Element not contained."))
        .iterator()
        .next();
  }

  /**
   * Merges the subsets containing the provided elements.
   *
   * <p>Expected running time: {@code O(s + min(a, b))} when merging distinct subsets of sizes
   * {@code a} and {@code b}, and {@code O(s)} otherwise, where {@code s} is the number of subsets.
   */
  @Override
  public void union(T pE1, T pE2) {
    Preconditions.checkNotNull(pE1);
    Preconditions.checkNotNull(pE2);

    Optional<Set<T>> subset1 = getSubset(pE1);
    Optional<Set<T>> subset2 = getSubset(pE2);

    if (subset1.isEmpty()) {
      if (subset2.isEmpty()) {
        Set<T> newSubset = new LinkedHashSet<>();
        newSubset.add(pE1);
        newSubset.add(pE2);
        subsets.add(newSubset);
      } else {
        addToSubset(pE1, subset2.orElseThrow());
      }
      return;
    }

    Set<T> existingSubset1 = subset1.orElseThrow();
    if (subset2.isEmpty()) {
      addToSubset(pE2, existingSubset1);
      return;
    }

    Set<T> existingSubset2 = subset2.orElseThrow();
    if (existingSubset1 != existingSubset2) {
      mergeSubsets(existingSubset1, existingSubset2);
    }
  }

  /**
   * Returns copies of all current subsets.
   *
   * <p>Expected running time: {@code O(n)}, where {@code n} is the number of elements.
   */
  @Override
  public Collection<? extends Set<T>> getAllSubsets() {
    Set<Set<T>> result = new HashSet<>(subsets.size());
    result.addAll(subsets);
    return result;
  }

  /**
   * Checks whether an element belongs to any current subset.
   *
   * <p>Expected running time: {@code O(s)}, where {@code s} is the number of subsets.
   */
  @Override
  public boolean contains(T pE) {
    return getSubset(pE).isPresent();
  }

  private void addToSubset(T pE, Set<T> pSubset) {
    // A HashSet member must be removed before changing the member's hash code.
    subsets.remove(pSubset);
    pSubset.add(pE);
    subsets.add(pSubset);
  }

  private void mergeSubsets(Set<T> pSubset1, Set<T> pSubset2) {
    Set<T> largerSubset = pSubset1.size() >= pSubset2.size() ? pSubset1 : pSubset2;
    Set<T> smallerSubset = largerSubset == pSubset1 ? pSubset2 : pSubset1;

    // Remove both before mutation to preserve the outer HashSet's hash table invariants.
    subsets.remove(largerSubset);
    subsets.remove(smallerSubset);
    largerSubset.addAll(smallerSubset);
    subsets.add(largerSubset);
  }

  private Optional<Set<T>> getSubset(T pE) {
    Preconditions.checkNotNull(pE);
    for (Set<T> subset : subsets) {
      if (subset.contains(pE)) {
        return Optional.of(subset);
      }
    }
    return Optional.empty();
  }
}
