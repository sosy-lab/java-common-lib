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
import org.sosy_lab.common.collect.union_find.ParentPointerTreeUnionFind;

public class ParentPointerTreeUnionFindTest {
  private ParentPointerTreeUnionFind<Integer> integerUnionFind;

  @Before
  public void setup() {
    integerUnionFind = new ParentPointerTreeUnionFind<>();
  }

  @Test
  public void testFind_afterSelfUnion_returnsItself() {

    integerUnionFind.union(0, 0);

    assertThat(integerUnionFind.find(0)).isEqualTo(0);
  }

  @Test
  public void testUnion_twoNewElements_producesSingleSubsetOfSizeTwo() {

    integerUnionFind.union(0, 1);

    assertThat(integerUnionFind.find(0)).isEqualTo(integerUnionFind.find(1));
    assertThat(integerUnionFind.getAllSubsets()).hasSize(1);
  }

  @Test
  public void testUnion_disjointPairs_produceDistinctSubsets() {

    integerUnionFind.union(0, 1);
    integerUnionFind.union(2, 3);

    assertThat(integerUnionFind.find(0)).isNotEqualTo(integerUnionFind.find(2));
    assertThat(integerUnionFind.getAllSubsets()).hasSize(2);
  }

  @Test
  public void testUnion_severalElementsToSameSubset() {

    integerUnionFind.union(0, 1);
    integerUnionFind.union(1, 2);
    integerUnionFind.union(2, 3);

    Integer canon = integerUnionFind.find(0);
    assertThat(integerUnionFind.find(0)).isEqualTo(canon);
    assertThat(integerUnionFind.find(1)).isEqualTo(canon);
    assertThat(integerUnionFind.find(2)).isEqualTo(canon);
    assertThat(integerUnionFind.find(3)).isEqualTo(canon);
    assertThat(integerUnionFind.getAllSubsets()).hasSize(1);
  }

  @Test
  public void testUnion_duplicateUnionCall_doesNotLeadToDuplicates() {

    integerUnionFind.union(0, 1);
    integerUnionFind.union(0, 1);
    integerUnionFind.union(1, 0);

    assertThat(integerUnionFind.find(0)).isEqualTo(integerUnionFind.find(1));
    assertThat(integerUnionFind.getAllSubsets()).hasSize(1);
  }

  @Test
  public void testUnion_mergesTwoExistingMultiElementSubsets() {

    integerUnionFind.union(0, 1);
    integerUnionFind.union(0, 2);
    integerUnionFind.union(3, 4);
    integerUnionFind.union(3, 5);

    assertThat(integerUnionFind.getAllSubsets()).hasSize(2);

    integerUnionFind.union(0, 3);

    Integer canon = integerUnionFind.find(0);
    for (int i = 0; i <= 5; i++) {
      assertThat(integerUnionFind.find(i)).isEqualTo(canon);
    }
  }

  @Test
  public void testUnion_constantCanonicalElementDuringNonLinearInsertion() {

    integerUnionFind.union(3, 3);
    integerUnionFind.union(3, 2);
    integerUnionFind.union(3, 5);
    integerUnionFind.union(3, 1);
    integerUnionFind.union(3, 8);
    integerUnionFind.union(3, 6);
    integerUnionFind.union(3, 9);
    integerUnionFind.union(3, 7);
    integerUnionFind.union(3, 4);
    integerUnionFind.union(3, 0);

    Integer canon = integerUnionFind.find(3);
    for (int i = 0; i <= 9; i++) {
      assertThat(integerUnionFind.find(i)).isEqualTo(canon);
    }
  }

  @Test
  public void testGetAllSubsets_reflectsCorrectMembershipAfterMultipleUnions() {

    for (int i = 0; i <= 4; i++) {
      integerUnionFind.union(0, i);
    }
    for (int i = 5; i <= 9; i++) {
      integerUnionFind.union(5, i);
    }

    Collection<? extends Set<Integer>> subsets = integerUnionFind.getAllSubsets();

    assertThat(subsets).hasSize(2);
    for (Set<Integer> subset : subsets) {
      assertThat(subset).hasSize(5);
    }
  }

  @Test
  public void testUnion_StringElements() {

    ParentPointerTreeUnionFind<String> stringUnionFind = new ParentPointerTreeUnionFind<>();

    stringUnionFind.union(Integer.toString(0), Integer.toString(1));
    stringUnionFind.union(Integer.toString(0), Integer.toString(2));
    stringUnionFind.union(Integer.toString(3), Integer.toString(4));

    assertThat(stringUnionFind.find(Integer.toString(0)))
        .isEqualTo(stringUnionFind.find(Integer.toString(2)));
    assertThat(stringUnionFind.find(Integer.toString(0)))
        .isNotEqualTo(stringUnionFind.find(Integer.toString(3)));
    assertThat(stringUnionFind.getAllSubsets()).hasSize(2);
  }
}
