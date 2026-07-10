// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.junit.Test;

public class UnionFindSimpleBenchmarkTest {
  final int lowerBound = 2;
  final int factorForComparison = 10;

  @Test
  public void unionBigOQuadraticEvaluationTest() {
    Set<SortedUnionFind<Integer>> allUnionFindsOfFirstLoop = new HashSet<>();

    Duration timeBeforeFirstLoop = Duration.ofNanos(System.nanoTime());
    for (int i = 0; i < lowerBound; i++) {
      Set<Integer> values = new HashSet<>();
      for (int j = 0; j <= i; j++) {
        values.add(j);
      }

      allUnionFindsOfFirstLoop.addAll(generateUnionFinds(getPermutations(values)));
    }
    Duration timeAfterFirstLoop = Duration.ofNanos(System.nanoTime());
    Duration timeOfFirstLoop = timeAfterFirstLoop.minus(timeBeforeFirstLoop);

    final int higherBound = lowerBound * factorForComparison;

    Duration timeBeforeSecondLoop = Duration.ofNanos(System.nanoTime());
    for (int i = 0; i < higherBound; i++) {
      Set<Integer> values = new HashSet<>();
      for (int j = 0; j <= i; j++) {
        values.add(j);
      }

      allUnionFindsOfFirstLoop.addAll(generateUnionFinds(getPermutations(values)));
    }
    Duration timeAfterSecondLoop = Duration.ofNanos(System.nanoTime());
    Duration timeOfSecondLoop = timeAfterSecondLoop.minus(timeBeforeSecondLoop);

    assertThat(timeOfSecondLoop)
        .isLessThan(timeOfFirstLoop.multipliedBy(factorForComparison * factorForComparison));
  }

  private Set<SortedUnionFind<Integer>> generateUnionFinds(Set<Set<Set<Integer>>> pInput) {
    Preconditions.checkNotNull(pInput);

    Set<SortedUnionFind<Integer>> unionFindSet = new HashSet<>();

    for (Set<Set<Integer>> subsetOfSets : pInput) {
      SortedUnionFind<Integer> unionFind = new SortedTreeSetUnionFind<>();

      for (Set<Integer> subSubset : subsetOfSets) {
        if (!subSubset.isEmpty()) {
          Iterator<Integer> iterator = subSubset.iterator();
          Integer value = iterator.next();
          unionFind.union(value, value);
          if (subSubset.size() > 1) {
            while (iterator.hasNext()) {
              unionFind.union(value, iterator.next());
            }
          }
        }
      }

      unionFindSet.add(unionFind);
    }

    return unionFindSet;
  }

  private Set<Set<Set<Integer>>> getPermutations(Set<Integer> pInput) {
    Preconditions.checkNotNull(pInput);

    int size = pInput.size();
    Set<Set<Set<Integer>>> allPermutations = new HashSet<>();

    Set<Set<Integer>> powerSet = Sets.powerSet(pInput);

    for (Set<Integer> currentSet : powerSet) {
      int freeSlots = size - currentSet.size();
      Set<Integer> remainingValues = new HashSet<>(pInput);
      remainingValues.removeAll(currentSet);

      if (freeSlots > 1) {
        for (Set<Set<Integer>> combinationValues : getPermutations(remainingValues)) {
          Set<Set<Integer>> subPermutation = new HashSet<>();
          subPermutation.add(currentSet);
          subPermutation.addAll(combinationValues);
          allPermutations.add(subPermutation);
        }
      } else if (freeSlots == 1) {
        Set<Set<Integer>> subPermutation = new HashSet<>();
        subPermutation.add(currentSet);
        subPermutation.add(remainingValues);
        allPermutations.add(subPermutation);
      } else if (freeSlots == 0) {
        Set<Set<Integer>> subPermutation = new HashSet<>();
        subPermutation.add(currentSet);
        allPermutations.add(subPermutation);
      }
    }

    return removeTooSmallSets(allPermutations, size);
  }

  private Set<Set<Set<Integer>>> removeTooSmallSets(Set<Set<Set<Integer>>> pInput, int pN) {
    Preconditions.checkNotNull(pInput);

    Set<Set<Set<Integer>>> returnSet = new HashSet<>(pInput);

    for (Set<Set<Integer>> currentSubset : pInput) {
      if (!totalNoOfElemsInSubsetsEquals(currentSubset, pN)) returnSet.remove(currentSubset);
    }

    return returnSet;
  }

  private boolean totalNoOfElemsInSubsetsEquals(Set<Set<Integer>> pInput, int pN) {
    Preconditions.checkNotNull(pInput);

    int counter = 0;

    for (Set<Integer> currentSubset : pInput) {
      for (Integer e : currentSubset) {
        counter++;
      }
    }

    return counter == pN;
  }
}
