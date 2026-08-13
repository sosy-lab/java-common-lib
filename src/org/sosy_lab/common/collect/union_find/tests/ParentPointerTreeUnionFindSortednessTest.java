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
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.collect.union_find.SortedParentPointerTreeUnionFind;

public class ParentPointerTreeUnionFindSortednessTest {

  private SortedParentPointerTreeUnionFind<Integer> sortedUnionFind;

  @Before
  public void setup() {
    sortedUnionFind = new SortedParentPointerTreeUnionFind<>();
  }

  @Test
  public void testGetAllSubsets_elementsAddedInAscendingOrder_remainSorted() {

    sortedUnionFind.union(0, 1);
    sortedUnionFind.union(0, 2);
    sortedUnionFind.union(0, 3);

    assertThat(onlySubsetOf(sortedUnionFind)).containsExactly(0, 1, 2, 3).inOrder();
  }

  @Test
  public void testGetAllSubsets_elementsAddedInDescendingOrder_areSortedAscending() {

    sortedUnionFind.union(3, 2);
    sortedUnionFind.union(3, 1);
    sortedUnionFind.union(3, 0);

    assertThat(onlySubsetOf(sortedUnionFind)).containsExactly(0, 1, 2, 3).inOrder();
  }

  @Test
  public void testGetAllSubsets_nonlinearInsertionOrder_areReturnedSorted() {

    sortedUnionFind.union(3, 2);
    sortedUnionFind.union(3, 4);
    sortedUnionFind.union(3, 0);
    sortedUnionFind.union(3, 5);
    sortedUnionFind.union(3, 1);

    assertThat(onlySubsetOf(sortedUnionFind)).containsExactly(0, 1, 2, 3, 4, 5).inOrder();
  }

  @Test
  public void testGetAllSubsets_multipleSubsets_eachSortedIndependently() {

    sortedUnionFind.union(0, 1);
    sortedUnionFind.union(0, 2);
    sortedUnionFind.union(10, 11);
    sortedUnionFind.union(10, 12);

    for (Collection<Integer> subset : sortedUnionFind.getAllSubsets()) {
      assertThat(subset).isInOrder();
    }
  }

  @Test
  public void testGetAllSubsets_afterMergingTwoSubsets_resultIsSorted() {

    sortedUnionFind.union(0, 1);
    sortedUnionFind.union(0, 2);
    sortedUnionFind.union(3, 4);
    sortedUnionFind.union(3, 5);

    sortedUnionFind.union(0, 3);

    assertThat(onlySubsetOf(sortedUnionFind)).containsExactly(0, 1, 2, 3, 4, 5).inOrder();
  }

  @Test
  public void testGetAllSubsets_stringElements_areSortedAlphabetically() {

    SortedParentPointerTreeUnionFind<String> stringSortedUnionFind =
        new SortedParentPointerTreeUnionFind<>();
    String[] expected = {"-1", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

    for (int i = 0; i <= 2; i++) {
      stringSortedUnionFind.union("0", Integer.toString(i));
    }
    for (int i = 3; i <= 5; i++) {
      stringSortedUnionFind.union("3", Integer.toString(i));
    }
    for (int i = 6; i <= 8; i++) {
      stringSortedUnionFind.union("6", Integer.toString(i));
    }

    stringSortedUnionFind.union("9", "9");
    stringSortedUnionFind.union("0", "6");
    stringSortedUnionFind.union("6", "-1");
    stringSortedUnionFind.union("1", "4");
    stringSortedUnionFind.union("0", "9");

    assertThat(onlySubsetOf(stringSortedUnionFind)).containsExactlyElementsIn(expected).inOrder();
  }

  private static <T> Set<T> onlySubsetOf(SortedParentPointerTreeUnionFind<T> sortedUnionFind) {

    Collection<? extends Set<T>> subsets = sortedUnionFind.getAllSubsets();

    assertThat(subsets).hasSize(1);
    return subsets.iterator().next();
  }
}
