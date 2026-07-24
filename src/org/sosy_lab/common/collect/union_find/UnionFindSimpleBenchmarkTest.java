// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Var;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@Ignore
@RunWith(Parameterized.class)
public class UnionFindSimpleBenchmarkTest {

  private static final int maximumLower = 8;
  private static final int maximumUpper = 10;

  private final int lowerBound;
  private final int upperBound;

  /**
   * Builds parameters for lowerBound and upperBound (as 2-Tuples). lowerBounds are computed from 1
   * to maximumLower. And maximumUpper, for each lowerBound, from current lowerBound to
   * maximumUpper.
   */
  @Parameters(name = "{index}: lowerBound {0}, upperBound {1}")
  public static ImmutableList<Object[]> getBounds() {
    ImmutableList.Builder<Object[]> outer = ImmutableList.builder();
    for (int lower = 1; lower <= maximumLower; lower++) {
      for (int upper = 2; upper <= maximumUpper; upper++) {
        if (upper > lower) {
          Integer[] inner = new Integer[2];
          inner[0] = lower;
          inner[1] = upper;
          outer.add(inner);
        }
      }
    }
    return outer.build();
  }

  public UnionFindSimpleBenchmarkTest(int lowerBound, int upperBound) {
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
  }

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
    // System.out.println("Time for first loop: " + timeOfFirstLoop.getSeconds() + "s" + "\n");

    // BEGINNING of 2nd loop
    Set<SortedUnionFind<Integer>> allUnionFindsOfSecondLoop = new HashSet<>();

    Duration timeBeforeSecondLoop = Duration.ofNanos(System.nanoTime());

    List<Set<Set<Integer>>> partitions2 = generatePartitions(upperBound);
    transformPartitionsToUnionFind(partitions2, allUnionFindsOfSecondLoop);

    Duration timeAfterSecondLoop = Duration.ofNanos(System.nanoTime());
    Duration timeOfSecondLoop = timeAfterSecondLoop.minus(timeBeforeSecondLoop);
    // END of 2nd loop
    // System.out.println("Time for second loop: " + timeOfSecondLoop.getSeconds() + "s" + "\n");

    // will throw for larger n's as they won't fit into long
    long bellOfLowerBound = getBellNoOfN(lowerBound).longValueExact();
    long bellOfUpperBound = getBellNoOfN(upperBound).longValueExact();

    assertThat(timeOfSecondLoop.dividedBy(bellOfUpperBound))
        .isLessThan(
            timeOfFirstLoop.multipliedBy(
                (bellOfUpperBound * bellOfUpperBound) / (bellOfLowerBound * bellOfLowerBound)));
  }

  // TODO: add a method that computes only permutations with n elements.

  // TODO: this computes all permutations from 2 to pHighestNumber + 2 -> make it compute them only
  // from 1 to pHighestNumber
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

      /*
      // This prints all permutations that are added without duplicates
      for (Set<Set<Integer>> newSet : newSets) {
        if (!allPermutations.contains(newSet)) {
          System.out.println(newSet);
        }
      }
      */
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

  // calculates the Bell Number of a given n>=0 using the Bell Triangle
  // uses BigInteger because int/Integer would run out of space at comparatively small n's
  private static BigInteger getBellNoOfN(int n) {
    @Var List<BigInteger> previousRow = new ArrayList<>();

    // initialise for n=0
    previousRow.add(BigInteger.valueOf(1));

    for (int i = 1; i < n; i++) {
      List<BigInteger> currentRow = new ArrayList<>();
      currentRow.add(previousRow.get(previousRow.size() - 1));

      for (int j = 1; j <= i; j++) {
        currentRow.add(previousRow.get(j - 1).add(currentRow.get(j - 1)));
      }

      previousRow = currentRow;
    }

    return previousRow.get(previousRow.size() - 1);
  }
}
