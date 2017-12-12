/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.common.collect;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.testing.TestStringSortedSetGenerator;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import com.google.errorprone.annotations.Var;
import java.util.Collection;
import java.util.Collections;
import java.util.NavigableSet;
import org.junit.Test;

public abstract class OrderStatisticSetTestSuite {

  private static final String[] ELEMS = {"aaa", "hha", "ppa", "zza"};
  private static final String[] ELEMS_ABOVE = {"aab", "hhb", "ppb", "zzb"};
  private static final String[] ELEMS_BELOW = {"aa", "hh", "pp", "zz"};

  public abstract static class OrderStatisticSetFactory extends TestStringSortedSetGenerator {

    @Override
    protected abstract OrderStatisticSet<String> create(String[] pStrings);
  }

  private OrderStatisticSetFactory factory;

  public OrderStatisticSetTestSuite(OrderStatisticSetFactory pFactory) {
    factory = pFactory;
  }

  private OrderStatisticSet<String> createSet() {
    String[] elems = new String[0];
    return factory.create(elems);
  }

  private OrderStatisticSet<String> createSet(String[] pElems) {
    return factory.create(pElems);
  }

  @Test
  public void testEquals() {
    EqualsTester setEqualsTester = new EqualsTester();

    @Var OrderStatisticSet<String> l1 = createSet();
    @Var OrderStatisticSet<String> l2 = createSet();

    setEqualsTester.addEqualityGroup(l1, l2);

    l1 = createSet();
    l2 = createSet();
    Collections.addAll(l1, "a", "b", "c", "d");
    Collections.addAll(l2, "d", "c", "a", "b");

    OrderStatisticSet<String> l3 = createSet();
    Collections.addAll(l3, "d", "c", "a", "b", "a", "b", "a");
    setEqualsTester.addEqualityGroup(l1, l2, l3);
    setEqualsTester.testEquals();
  }

  @Test
  public void testSerialize() {
    OrderStatisticSet<String> l = createSet();
    SerializableTester.reserializeAndAssert(l);

    for (int i = 100000; i >= 0; i--) {
      l.add(String.valueOf(i));
    }
    SerializableTester.reserializeAndAssert(l);
  }

  @Test
  public void testSubsetView_mutation() {
    NavigableSet<String> set = createSet(ELEMS);
    NavigableSet<String> subSet = set.subSet(ELEMS[1], true, ELEMS[2], true);

    String toAdd = ELEMS_BELOW[2];

    subSet.add(toAdd);
    assertThat(subSet).contains(toAdd);
    assertThat(set).contains(toAdd);

    subSet.remove(toAdd);
    assertThat(subSet).doesNotContain(toAdd);
    assertThat(set).doesNotContain(toAdd);

    set.add(toAdd);
    assertThat(subSet).contains(toAdd);

    set.remove(toAdd);
    assertThat(subSet).doesNotContain(toAdd);
  }

  @Test
  public void testSubsetView_outOfBounds_add() {
    NavigableSet<String> set = createSet(ELEMS);
    NavigableSet<String> subSet = set.subSet(ELEMS_ABOVE[0], true, ELEMS_ABOVE[2], true);
    Collection<String> toAdd = ImmutableList.of(ELEMS[1], ELEMS_BELOW[2], ELEMS[2], ELEMS_ABOVE[3]);

    try {
      subSet.add(ELEMS[0]);
      fail();
    } catch (IllegalArgumentException expected) {
      try {
        subSet.add(ELEMS[3]);
        fail();
      } catch (IllegalArgumentException expected2) {
        try {
          // the first 3 elements are in the range of the sublist, but the last isn't
          subSet.addAll(toAdd);
          fail();
        } catch (IllegalArgumentException expected3) {
          // expected outcome
        }
      }
    }
  }

  @Test
  public void testSubsetView_outOfBounds_remove() {
    NavigableSet<String> set = createSet(ELEMS);
    NavigableSet<String> subSet = set.subSet(ELEMS_ABOVE[1], true, ELEMS_ABOVE[2], true);

    subSet.remove(ELEMS[1]);
    subSet.remove(ELEMS[3]);

    assertThat(set).contains(ELEMS[1]);
    assertThat(set).contains(ELEMS[3]);

    Collection<String> toRemove = ImmutableList.of(ELEMS_BELOW[2], ELEMS[2], ELEMS[1]);
    subSet.removeAll(toRemove);

    assertThat(set).contains(ELEMS[1]);
    assertThat(set).doesNotContain(ELEMS_BELOW[2]);
    assertThat(set).doesNotContain(ELEMS[2]);
  }

  @Test
  public void testSubsetView_outOfBounds_contains() {
    NavigableSet<String> set = createSet(ELEMS);
    @Var NavigableSet<String> subSet = set.subSet(ELEMS_ABOVE[1], true, ELEMS_ABOVE[2], true);

    assertThat(subSet).doesNotContain(ELEMS[1]);
    assertThat(subSet).doesNotContain(ELEMS[3]);

    subSet = set.subSet(ELEMS_ABOVE[1], false, ELEMS_ABOVE[2], false);

    assertThat(subSet).doesNotContain(ELEMS_ABOVE[1]);
    assertThat(subSet).doesNotContain(ELEMS_ABOVE[2]);

    subSet = set.subSet(ELEMS_ABOVE[0], true, ELEMS_ABOVE[2], true);

    assertThat(subSet).contains(ELEMS[1]);
    assertThat(subSet).contains(ELEMS[2]);
  }

  @Test
  public void testSubsetView_descending() {
    OrderStatisticSet<String> set = createSet(ELEMS);
    @Var
    OrderStatisticSet<String> subSet =
        set.subSet(
                ELEMS[1], /* fromInclusive= */ true,
                ELEMS[2], /* toInclusive= */ true)
            .descendingSet();

    assertThat(ELEMS[2]).isEqualTo(subSet.first());
    assertThat(ELEMS[1]).isEqualTo(subSet.last());

    subSet = subSet.descendingSet();
    assertThat(ELEMS[1]).isEqualTo(subSet.first());
    assertThat(ELEMS[2]).isEqualTo(subSet.last());
  }

  @Test
  public void testSubsetView_subsetOfSubset() {
    OrderStatisticSet<String> set = createSet(ELEMS);
    NavigableSet<String> subSet =
        set.subSet(ELEMS[1], /* fromInclusive= */ true, ELEMS[3], /* toInclusive=*/ true);
    @Var
    NavigableSet<String> subSubSet =
        subSet.subSet(
            ELEMS[1], /* fromInclusive= */ true,
            ELEMS_BELOW[3], /* toInclusive= */ true);

    assertThat(subSubSet).doesNotContain(ELEMS[0]);
    assertThat(subSubSet).contains(ELEMS[1]);
    assertThat(subSubSet).contains(ELEMS[2]);
    assertThat(subSubSet).doesNotContain(ELEMS[3]);

    // make sure that the inclusive-flags are respected
    subSubSet =
        subSet.subSet(ELEMS[1], /* fromInclusive= */ true, ELEMS[3], /* toInclusive=*/ true);
    assertThat(subSubSet).contains(ELEMS[1]);
    assertThat(subSubSet).contains(ELEMS[2]);
    assertThat(subSubSet).contains(ELEMS[3]);

    subSubSet =
        subSubSet.subSet(ELEMS[1], /* fromInclusive= */ false, ELEMS[3], /* toInclusive= */ false);
    assertThat(subSubSet).doesNotContain(ELEMS[1]);
    assertThat(subSubSet).contains(ELEMS[2]);
    assertThat(subSubSet).doesNotContain(ELEMS[3]);
  }

  @Test
  public void testGetByRank_valid() {
    OrderStatisticSet<String> set = createSet(ELEMS);

    for (int i = 0; i < ELEMS.length; i++) {
      assertThat(ELEMS[i]).isEqualTo(set.getByRank(i));
    }
  }

  @Test
  public void testGetByRank_outOfBounds() {
    OrderStatisticSet<String> set = createSet(ELEMS);

    try {
      set.getByRank(-1);
      fail("Expected " + IndexOutOfBoundsException.class.getSimpleName());
    } catch (IndexOutOfBoundsException expected) {
      try {
        set.getByRank(ELEMS.length);
        fail("Expected " + IndexOutOfBoundsException.class.getSimpleName());
      } catch (IndexOutOfBoundsException expected2) {
        // this is expected
      }
    }
  }

  @Test
  public void testGetByRank_subsetFirst() {
    OrderStatisticSet<String> set = createSet(ELEMS);
    OrderStatisticSet<String> subSet =
        set.subSet(ELEMS[1], /* fromInclusive= */ true, ELEMS[2], /* toInclusive= */ true);

    String firstSubSetElement = subSet.getByRank(0);

    assertThat(firstSubSetElement).isEqualTo(ELEMS[1]);
  }

  @Test
  public void testGetByRank_subsetLast() {
    OrderStatisticSet<String> set = createSet(ELEMS);
    OrderStatisticSet<String> subSet =
        set.subSet(ELEMS[1], /* fromInclusive= */ true, ELEMS[2], /* toInclusive= */ true);

    String lastSubSetElement = subSet.getByRank(subSet.size() - 1);

    assertThat(lastSubSetElement).isEqualTo(ELEMS[2]);
  }

  @Test
  public void testGetByRank_descendingSetFirstElement() {
    OrderStatisticSet<String> set = createSet(ELEMS);
    OrderStatisticSet<String> descendingSet = set.descendingSet();

    String firstElementDescending = descendingSet.getByRank(0);

    assertThat(firstElementDescending).isEqualTo(ELEMS[ELEMS.length - 1]);
  }

  @Test
  public void testGetByRank_descendingSetSecondElement() {
    OrderStatisticSet<String> set = createSet(ELEMS);
    OrderStatisticSet<String> descendingSet = set.descendingSet();

    String firstElementDescending = descendingSet.getByRank(1);

    assertThat(firstElementDescending).isEqualTo(ELEMS[ELEMS.length - 2]);
  }

  @Test
  public void testGetByRank_descendingSetLastElement() {
    OrderStatisticSet<String> set = createSet(ELEMS);
    OrderStatisticSet<String> descendingSet = set.descendingSet();

    String lastElementDescending = descendingSet.getByRank(descendingSet.size() - 1);

    assertThat(lastElementDescending).isEqualTo(ELEMS[0]);
  }

  @Test
  public void testRemoveByRank_valid() {
    OrderStatisticSet<String> set = createSet(ELEMS);

    set.removeByRank(2);

    assertThat(set).doesNotContain(ELEMS[2]);
    assertThat(set).contains(ELEMS[0]);
    assertThat(set).contains(ELEMS[1]);
    assertThat(set).contains(ELEMS[3]);

    set.removeByRank(0);

    assertThat(set).doesNotContain(ELEMS[0]);
    assertThat(set).contains(ELEMS[1]);
    assertThat(set).contains(ELEMS[3]);

    set.removeByRank(set.size() - 1);
    assertThat(set).doesNotContain(ELEMS[3]);
    assertThat(set).contains(ELEMS[1]);

    set.removeByRank(0);
    assertThat(set).isEmpty();
  }

  @Test
  public void testRemoveByRank_invalid() {
    OrderStatisticSet<String> set = createSet(ELEMS);
    OrderStatisticSet<String> emptySet = createSet();

    try {
      set.removeByRank(-1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
      try {
        set.removeByRank(set.size());
        fail();
      } catch (IndexOutOfBoundsException expected2) {
        try {
          emptySet.removeByRank(0);
          fail();
        } catch (IndexOutOfBoundsException expected3) {
          // expected outcome
        }
      }
    }
  }

  @Test
  public void testRemoveByRank_subsetFirst() {
    OrderStatisticSet<String> set = createSet(ELEMS);
    OrderStatisticSet<String> subSet =
        set.subSet(ELEMS[1], /* fromInclusive= */ true, ELEMS[2], /* toInclusive= */ true);

    String firstSubSetElement = subSet.removeByRank(0);

    assertThat(firstSubSetElement).isEqualTo(ELEMS[1]);
    assertThat(subSet).doesNotContain(ELEMS[1]);
    assertThat(set).doesNotContain(ELEMS[1]);
  }

  @Test
  public void testRemoveByRank_subsetLast() {
    OrderStatisticSet<String> set = createSet(ELEMS);
    OrderStatisticSet<String> subSet =
        set.subSet(ELEMS[1], /* fromInclusive= */ true, ELEMS[2], /* toInclusive= */ true);

    String lastSubSetElement = subSet.removeByRank(subSet.size() - 1);

    assertThat(lastSubSetElement).isEqualTo(ELEMS[2]);
    assertThat(subSet).doesNotContain(ELEMS[2]);
    assertThat(set).doesNotContain(ELEMS[2]);
  }

  @Test
  public void testRemoveByRank_descendingSetFirstElement() {
    OrderStatisticSet<String> set = createSet(ELEMS);
    OrderStatisticSet<String> descendingSet = set.descendingSet();
    String expectedRemove = ELEMS[ELEMS.length - 1];

    String firstElementDescending = descendingSet.removeByRank(0);

    assertThat(firstElementDescending).isEqualTo(expectedRemove);
    assertThat(descendingSet).doesNotContain(expectedRemove);
    assertThat(set).doesNotContain(expectedRemove);
  }

  @Test
  public void testRemoveByRank_descendingSetLastElement() {
    OrderStatisticSet<String> set = createSet(ELEMS);
    OrderStatisticSet<String> descendingSet = set.descendingSet();
    String expectedRemove = ELEMS[0];

    String lastElementDescending = descendingSet.removeByRank(descendingSet.size() - 1);

    assertThat(lastElementDescending).isEqualTo(expectedRemove);
    assertThat(descendingSet).doesNotContain(expectedRemove);
    assertThat(set).doesNotContain(expectedRemove);
  }

  private static <E> void assertRankOf(E pElement, int pExpectedRank, OrderStatisticSet<E> pSet) {
    int actualRank = pSet.rankOf(pElement);

    assertThat(actualRank).isEqualTo(pExpectedRank);
  }

  @Test
  public void testRankOf_firstElement() {
    OrderStatisticSet<String> set = createSet(ELEMS);

    assertRankOf(ELEMS[0], 0, set);
  }

  @Test
  public void testRankOf_secondElement() {
    OrderStatisticSet<String> set = createSet(ELEMS);

    assertRankOf(ELEMS[1], 1, set);
  }

  @Test
  public void testRankOf_lastElement() {
    OrderStatisticSet<String> set = createSet(ELEMS);
    String element = ELEMS[ELEMS.length - 1];
    int expectedRank = ELEMS.length - 1;

    assertRankOf(element, expectedRank, set);
  }

  @Test
  public void testRankOf_descendingSetFirstElement() {
    OrderStatisticSet<String> descendingSet = createSet(ELEMS).descendingSet();
    String element = ELEMS[ELEMS.length - 1];
    int expectedRank = 0;

    assertRankOf(element, expectedRank, descendingSet);
  }

  @Test
  public void testRankOf_descendingSetSecondElement() {
    OrderStatisticSet<String> descendingSet = createSet(ELEMS).descendingSet();
    String element = ELEMS[ELEMS.length - 2];
    int expectedRank = 1;

    assertRankOf(element, expectedRank, descendingSet);
  }

  @Test
  public void testRankOf_descendingSetLastElement() {
    OrderStatisticSet<String> descendingSet = createSet(ELEMS).descendingSet();
    String element = ELEMS[0];
    int expectedRank = ELEMS.length - 1;

    assertRankOf(element, expectedRank, descendingSet);
  }

  @Test
  public void testRankOf_subSetFirstElement() {
    String firstSubsetElement = ELEMS[1];
    String lastSubsetElement = ELEMS[3];
    OrderStatisticSet<String> subSet =
        createSet(ELEMS)
            .subSet(
                firstSubsetElement,
                /* fromInclusive= */ true,
                lastSubsetElement,
                /* toInclusive= */ true);
    String element = firstSubsetElement;
    int expectedRank = 0;

    assertRankOf(element, expectedRank, subSet);
  }

  @Test
  public void testRankOf_subSetLastElement() {
    String firstSubsetElement = ELEMS[1];
    String lastSubsetElement = ELEMS[3];
    OrderStatisticSet<String> subSet =
        createSet(ELEMS)
            .subSet(
                firstSubsetElement,
                /* fromInclusive= */ true,
                lastSubsetElement,
                /* toInclusive= */ true);
    String element = lastSubsetElement;
    int expectedRank = subSet.size() - 1;

    assertRankOf(element, expectedRank, subSet);
  }

  @Test
  public void testRankOf_subSetSecondElement() {
    String firstSubsetElement = ELEMS[1];
    String lastSubsetElement = ELEMS[3];
    OrderStatisticSet<String> subSet =
        createSet(ELEMS)
            .subSet(
                firstSubsetElement,
                /* fromInclusive= */ true,
                lastSubsetElement,
                /* toInclusive= */ true);
    String element = ELEMS[2];
    int expectedRank = 1;

    assertRankOf(element, expectedRank, subSet);
  }
}
