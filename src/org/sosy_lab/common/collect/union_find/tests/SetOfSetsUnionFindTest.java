// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://www.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find.tests;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.collect.union_find.SetOfSetsUnionFind;

public class SetOfSetsUnionFindTest {

  private SetOfSetsUnionFind<Integer> unionFind;

  @Before
  public void setup() {
    unionFind = new SetOfSetsUnionFind<>();
  }

  @Test
  public void testUnion_newElements_usesFirstElementAsCanonical() {
    unionFind.union(3, 5);
    unionFind.union(5, 1);

    assertThat(unionFind.find(3)).isEqualTo(3);
    assertThat(unionFind.find(5)).isEqualTo(3);
    assertThat(unionFind.find(1)).isEqualTo(3);
  }

  @Test
  public void testUnion_mergesSubsets_usingAnExistingCanonicalElement() {
    unionFind.union(0, 1);
    unionFind.union(2, 3);

    int firstCanonical = unionFind.find(0);
    int secondCanonical = unionFind.find(2);
    unionFind.union(1, 3);

    int mergedCanonical = unionFind.find(0);
    assertThat(mergedCanonical).isAnyOf(firstCanonical, secondCanonical);
    for (int i = 0; i <= 3; i++) {
      assertThat(unionFind.find(i)).isEqualTo(mergedCanonical);
    }
  }

  @Test
  public void testGetAllSubsets_returnsAllPartitionMembers() {
    unionFind.union(0, 1);
    unionFind.union(2, 3);
    unionFind.union(4, 4);

    Collection<? extends Set<Integer>> subsets = unionFind.getAllSubsets();

    assertThat(subsets)
        .containsExactly(ImmutableSet.of(0, 1), ImmutableSet.of(2, 3), ImmutableSet.of(4));
    assertThat(unionFind.contains(3)).isTrue();
    assertThat(unionFind.contains(5)).isFalse();
  }

  @Test
  public void testGetAllSubsets_returnsImmutableSubsets() {
    unionFind.union(0, 1);

    Collection<? extends Set<Integer>> subsets = unionFind.getAllSubsets();

    assertThrows(UnsupportedOperationException.class, () -> subsets.iterator().next().add(2));
    assertThat(unionFind.contains(2)).isFalse();
  }

  @Test
  public void testFind_absentElement_throwsException() {
    assertThrows(IllegalArgumentException.class, () -> unionFind.find(0));
  }
}
