// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find.tests;

import static com.google.common.truth.Truth.assertThat;

import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableSet;
import org.junit.Test;
import org.sosy_lab.common.collect.union_find.ImmutableSortedParentPointerTreeUnionFind;
import org.sosy_lab.common.collect.union_find.ParentPointerTreeUnionFind.UnionType;

public class ImmutableParentPointerTreeSortednessTest {

  private static ImmutableSortedParentPointerTreeUnionFind<Integer> buildImmutableSorted(
      UnionType pUnionType, int[]... pUnions) {

    ImmutableSortedParentPointerTreeUnionFind.Builder<Integer> builder =
        ImmutableSortedParentPointerTreeUnionFind.Builder.getBuilder(pUnionType);

    for (int[] pair : pUnions) {
      builder.union(pair[0], pair[1]);
    }

    return builder.build();
  }

  private static <T extends Comparable<T>> NavigableSet<T> onlySubsetOf(
      ImmutableSortedParentPointerTreeUnionFind<T> pSortedUnionFind) {

    Collection<? extends NavigableSet<T>> subsets = pSortedUnionFind.getAllSubsets();

    assertThat(subsets).hasSize(1);

    return subsets.iterator().next();
  }

  @Test
  public void testGetAllSubsets_elementsAddedInAscendingOrder_remainSorted() {

    ImmutableSortedParentPointerTreeUnionFind<Integer> sortedUnionFind =
        buildImmutableSorted(
            UnionType.UNION_BY_SIZE, new int[] {0, 1}, new int[] {0, 2}, new int[] {0, 3});

    assertThat(onlySubsetOf(sortedUnionFind)).containsExactly(0, 1, 2, 3).inOrder();
  }

  @Test
  public void testGetAllSubsets_elementsAddedInDescendingOrder_areSortedAscending() {

    ImmutableSortedParentPointerTreeUnionFind<Integer> sortedUnionFind =
        buildImmutableSorted(
            UnionType.UNION_BY_SIZE, new int[] {3, 2}, new int[] {3, 1}, new int[] {3, 0});

    assertThat(onlySubsetOf(sortedUnionFind)).containsExactly(0, 1, 2, 3).inOrder();
  }

  @Test
  public void testGetAllSubsets_nonLinearInsertionOrder_areReturnedSorted() {

    ImmutableSortedParentPointerTreeUnionFind<Integer> sortedUnionFind =
        buildImmutableSorted(
            UnionType.UNION_BY_SIZE,
            new int[] {3, 2},
            new int[] {3, 4},
            new int[] {3, 0},
            new int[] {3, 5},
            new int[] {3, 1});

    assertThat(onlySubsetOf(sortedUnionFind)).containsExactly(0, 1, 2, 3, 4, 5).inOrder();
  }

  @Test
  public void testGetAllSubsets_multipleSubsets_eachSortedIndependently() {

    ImmutableSortedParentPointerTreeUnionFind<Integer> sortedUnionFind =
        buildImmutableSorted(
            UnionType.UNION_BY_SIZE,
            new int[] {0, 1},
            new int[] {0, 2},
            new int[] {10, 11},
            new int[] {10, 12});

    for (NavigableSet<Integer> subset : sortedUnionFind.getAllSubsets()) {

      assertThat(subset).isInOrder();
    }
  }

  @Test
  public void testGetAllSubsets_afterMergingTwoSubsets_resultIsSorted() {

    ImmutableSortedParentPointerTreeUnionFind<Integer> sortedUnionFind =
        buildImmutableSorted(
            UnionType.UNION_BY_SIZE,
            new int[] {0, 1},
            new int[] {0, 2},
            new int[] {3, 4},
            new int[] {3, 5},
            new int[] {0, 3});

    assertThat(onlySubsetOf(sortedUnionFind)).containsExactly(0, 1, 2, 3, 4, 5).inOrder();
  }

  @Test
  public void testGetAllSubsets_stringElements_areSortedAlphabetically() {

    ImmutableSortedParentPointerTreeUnionFind.Builder<String> builder =
        ImmutableSortedParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_SIZE);
    String[] expected = {"-1", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

    for (int i = 0; i <= 2; i++) {
      builder.union("0", Integer.toString(i));
    }
    for (int i = 3; i <= 5; i++) {
      builder.union("3", Integer.toString(i));
    }
    for (int i = 6; i <= 8; i++) {
      builder.union("6", Integer.toString(i));
    }

    builder.union("9", "9");
    builder.union("0", "6");
    builder.union("6", "-1");
    builder.union("1", "4");
    builder.union("0", "9");

    ImmutableSortedParentPointerTreeUnionFind<String> stringSortedUnionFind = builder.build();

    assertThat(onlySubsetOf(stringSortedUnionFind)).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  public void testGetAllSubsets_isSortedRegardlessOfUnionType() {

    int[][] unions = {{3, 2}, {3, 4}, {3, 0}, {3, 5}, {3, 1}};

    for (UnionType unionType : UnionType.values()) {

      ImmutableSortedParentPointerTreeUnionFind<Integer> sortedUnionFind =
          buildImmutableSorted(unionType, unions);

      assertThat(onlySubsetOf(sortedUnionFind)).containsExactly(0, 1, 2, 3, 4, 5).inOrder();
    }
  }

  @Test
  public void testGetAllSubsets_subsetsThemselvesAreOrderedByCanonicalElement() {

    ImmutableSortedParentPointerTreeUnionFind<Integer> sortedUnionFind =
        buildImmutableSorted(
            UnionType.UNION_BY_SIZE,
            new int[] {10, 11},
            new int[] {10, 12},
            new int[] {0, 1},
            new int[] {0, 2});

    Collection<? extends NavigableSet<Integer>> subsets = sortedUnionFind.getAllSubsets();
    assertThat(subsets).hasSize(2);

    Iterator<? extends NavigableSet<Integer>> iterator = subsets.iterator();
    NavigableSet<Integer> firstSubset = iterator.next();
    NavigableSet<Integer> secondSubset = iterator.next();

    assertThat(firstSubset.last()).isLessThan(secondSubset.first());
  }

  @Test
  public void testGetAllSubsets_returnedSetSupportsNavigableSetOperations() {

    ImmutableSortedParentPointerTreeUnionFind<Integer> sortedUnionFind =
        buildImmutableSorted(
            UnionType.UNION_BY_SIZE,
            new int[] {0, 1},
            new int[] {0, 2},
            new int[] {0, 3},
            new int[] {0, 4});

    NavigableSet<Integer> subset = onlySubsetOf(sortedUnionFind);

    assertThat(subset.first()).isEqualTo(0);
    assertThat(subset.last()).isEqualTo(4);
    assertThat(subset.higher(1)).isEqualTo(2);
    assertThat(subset.lower(3)).isEqualTo(2);
  }
}
