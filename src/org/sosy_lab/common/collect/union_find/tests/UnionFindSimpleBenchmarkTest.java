// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find.tests;

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
import org.sosy_lab.common.collect.union_find.SortedTreeSetUnionFind;
import org.sosy_lab.common.collect.union_find.SortedUnionFind;

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

  private static List<Set<Set<Integer>>> generatePartitions(int pHighestNumber) {
    List<Set<Set<Integer>>> allPartitions = new ArrayList<>();
    Set<Set<Integer>> singletonSet = new HashSet<>();

    for (int i = 1; i <= pHighestNumber; i++) {
      Set<Integer> singleNumberSet = new HashSet<>();
      singleNumberSet.add(i);
      singletonSet.add(singleNumberSet);
    }
    allPartitions.add(singletonSet);
    // System.out.println("New loop with upper bound " + pHighestNumber + ":");
    // System.out.println(singletonSet);

    // biggest subset size currently being created
    for (int i = 2; i <= pHighestNumber; i++) {
      // deep copy of allPartitions to iterate over
      List<Set<Set<Integer>>> partitionsSoFar = new ArrayList<>();
      for (Set<Set<Integer>> currentSet : allPartitions) {
        partitionsSoFar.add(createDeepCopy(currentSet));
      }

      // iterate over all partitions that currently exist
      for (Set<Set<Integer>> current : partitionsSoFar) {
        // add no.s from 1 - pHighestNumber to subsets
        for (int j = 1; j <= pHighestNumber; j++) {
          // add j to each subset that doesn't contain it yet by merging with subset containing j
          breaker:
          for (Set<Integer> currentSubset : current) {
            if (!currentSubset.contains(j)) {
              Set<Set<Integer>> copyOfCurrent = createDeepCopy(current);

              @Var Set<Integer> toBeAddedTo = new HashSet<>();
              @Var Set<Integer> toRemove = new HashSet<>();

              for (Set<Integer> subsetOfCopy : copyOfCurrent) {
                if (subsetOfCopy.equals(currentSubset)) {
                  toBeAddedTo = subsetOfCopy;
                } else if (subsetOfCopy.contains(j)) {
                  toRemove = subsetOfCopy;
                }
              }

              copyOfCurrent.remove(toRemove);
              copyOfCurrent.remove(toBeAddedTo);
              toBeAddedTo.addAll(toRemove);
              copyOfCurrent.add(toBeAddedTo);

              for (Set<Set<Integer>> set : allPartitions) {
                if (copyOfCurrent.equals(set)) {
                  continue breaker;
                }
              }
              allPartitions.add(copyOfCurrent);
              // System.out.println(copyOfCurrent);
            }
          }
        }
      }
    }

    return allPartitions;
  }

  private static Set<Set<Integer>> createDeepCopy(Set<Set<Integer>> pSets) {
    Set<Set<Integer>> copy = new HashSet<>();

    for (Set<Integer> subset : pSets) {
      Set<Integer> copyOfSubset = new HashSet<>(subset);
      copy.add(copyOfSubset);
    }

    return copy;
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
