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
import com.google.errorprone.annotations.Var;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class UnionFindSimpleBenchmarkTest {
  final int lowerBound = 2;
  final int factorForComparison = 5;

  @Test
  public void unionBigOQuadraticEvaluationTest() {
    // BEGINNING of 1st loop
    Set<SortedUnionFind<Integer>> allUnionFindsOfFirstLoop = new HashSet<>();

    Duration timeBeforeFirstLoop = Duration.ofNanos(System.nanoTime());

    List<Set<Set<Integer>>> partitions1 = generatePartitions(lowerBound);
    transformPartitionsToUnionFind(partitions1, allUnionFindsOfFirstLoop);

    Duration timeAfterFirstLoop = Duration.ofNanos(System.nanoTime());
    Duration timeOfFirstLoop = timeAfterFirstLoop.minus(timeBeforeFirstLoop);
    // END of 1st loop

    // BEGINNING of 2nd loop
    int upperBound = lowerBound * factorForComparison;
    Set<SortedUnionFind<Integer>> allUnionFindsOfSecondLoop = new HashSet<>();

    Duration timeBeforeSecondLoop = Duration.ofNanos(System.nanoTime());

    List<Set<Set<Integer>>> partitions2 = generatePartitions(upperBound);
    transformPartitionsToUnionFind(partitions2, allUnionFindsOfSecondLoop);

    Duration timeAfterSecondLoop = Duration.ofNanos(System.nanoTime());
    Duration timeOfSecondLoop = timeAfterSecondLoop.minus(timeBeforeSecondLoop);
    // END of 2nd loop

    assertThat(timeOfSecondLoop)
        .isLessThan(timeOfFirstLoop.multipliedBy(factorForComparison * factorForComparison));
  }

  private static List<Set<Set<Integer>>> generatePartitions(int pHighestNumber) {
    @Var List<Set<Set<Integer>>> allPermutations = new ArrayList<>();

    // initialise allPermutations
    Set<Set<Integer>> init = new HashSet<>();
    Set<Integer> initSubset = new HashSet<>();
    initSubset.add(0);
    init.add(initSubset);
    allPermutations.add(init);

    for (int i = 1; i <= pHighestNumber; i++) {

      List<Set<Set<Integer>>> newSets = new ArrayList<>();

      for (Set<Set<Integer>> existingSet : allPermutations) {
        for (Set<Integer> existingSubset : existingSet) {
          Set<Set<Integer>> setWithNumber = new HashSet<>(existingSet);
          setWithNumber.remove(existingSubset);
          Set<Integer> subsetWithNumber = new HashSet<>(existingSubset);
          subsetWithNumber.add(i);
          setWithNumber.add(subsetWithNumber);
          newSets.add(setWithNumber);
        }

        Set<Set<Integer>> currentExistingSet = new HashSet<>(existingSet);
        Set<Integer> subsetWithCurrentI = new HashSet<>();
        subsetWithCurrentI.add(i);
        currentExistingSet.add(subsetWithCurrentI);
        newSets.add(currentExistingSet);
      }

      allPermutations = newSets;
    }

    return allPermutations;
  }

  private static void transformPartitionsToUnionFind(
      List<Set<Set<Integer>>> pPartitions, Set<SortedUnionFind<Integer>> pSetOfUnionFinds) {
    Preconditions.checkNotNull(pPartitions);
    Preconditions.checkNotNull(pSetOfUnionFinds);

    for (Set<Set<Integer>> subsetOfSets : pPartitions) {
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

      pSetOfUnionFinds.add(unionFind);
    }
  }
}
