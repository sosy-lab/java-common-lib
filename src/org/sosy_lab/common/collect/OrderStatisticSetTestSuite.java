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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.testing.TestStringSortedSetGenerator;
import com.google.common.testing.SerializableTester;
import com.google.errorprone.annotations.Var;
import java.util.Collection;
import java.util.Collections;
import java.util.NavigableSet;
import org.junit.Assert;
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
    OrderStatisticSet<String> l1 = createSet();
    @Var OrderStatisticSet<String> l2 = createSet();

    Assert.assertEquals(l1, l2);

    Collections.addAll(l1, "a", "b", "c", "d");
    Assert.assertNotEquals(l1, l2);

    Collections.addAll(l2, "d", "c", "a", "b");
    Assert.assertEquals(l1, l2);

    l2 = createSet();
    Collections.addAll(l2, "d", "c", "a", "b", "a", "b", "a");
    Assert.assertEquals(l1, l2);
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
    Assert.assertTrue(subSet.contains(toAdd));
    Assert.assertTrue(set.contains(toAdd));

    subSet.remove(toAdd);
    Assert.assertFalse(set.contains(toAdd));
    Assert.assertFalse(subSet.contains(toAdd));

    set.add(toAdd);
    Assert.assertTrue(subSet.contains(toAdd));

    set.remove(toAdd);
    Assert.assertFalse(subSet.contains(toAdd));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSubsetView_outOfBounds_add() {
    NavigableSet<String> set = createSet(ELEMS);
    NavigableSet<String> subSet = set.subSet(ELEMS_ABOVE[0], true, ELEMS_ABOVE[2], true);

    try {
      subSet.add(ELEMS[0]);
    } catch (IllegalArgumentException e1) {
      try {
        subSet.add(ELEMS[3]);
      } catch (IllegalArgumentException e2) {
        // the first 3 elements are in the range of the sublist, but the last isn't
        Collection<String> toAdd =
            ImmutableList.of(ELEMS[1], ELEMS_BELOW[2], ELEMS[2], ELEMS_ABOVE[3]);
        subSet.addAll(toAdd);
      }
    }
  }

  @Test
  public void testSubsetView_outOfBounds_remove() {
    NavigableSet<String> set = createSet(ELEMS);
    NavigableSet<String> subSet = set.subSet(ELEMS_ABOVE[1], true, ELEMS_ABOVE[2], true);

    subSet.remove(ELEMS[1]);
    subSet.remove(ELEMS[3]);

    Assert.assertTrue(set.contains(ELEMS[1]));
    Assert.assertTrue(set.contains(ELEMS[3]));

    Collection<String> toRemove = ImmutableList.of(ELEMS_BELOW[2], ELEMS[2], ELEMS[1]);
    subSet.removeAll(toRemove);

    Assert.assertTrue(set.contains(ELEMS[1]));
    Assert.assertFalse(set.contains(ELEMS_BELOW[2]));
    Assert.assertFalse(set.contains(ELEMS[2]));
  }

  @Test
  public void testSubsetView_outOfBounds_contains() {
    NavigableSet<String> set = createSet(ELEMS);
    @Var NavigableSet<String> subSet = set.subSet(ELEMS_ABOVE[1], true, ELEMS_ABOVE[2], true);

    Assert.assertFalse(subSet.contains(ELEMS[1]));
    Assert.assertFalse(subSet.contains(ELEMS[3]));

    subSet = set.subSet(ELEMS_ABOVE[1], false, ELEMS_ABOVE[2], false);

    Assert.assertFalse(subSet.contains(ELEMS_ABOVE[1]));
    Assert.assertFalse(subSet.contains(ELEMS_ABOVE[2]));

    subSet = set.subSet(ELEMS_ABOVE[0], true, ELEMS_ABOVE[2], true);

    Assert.assertTrue(subSet.contains(ELEMS[1]));
    Assert.assertTrue(subSet.contains(ELEMS[2]));
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

    Assert.assertEquals(ELEMS[2], subSet.first());
    Assert.assertEquals(ELEMS[1], subSet.last());

    subSet = subSet.descendingSet();
    Assert.assertEquals(ELEMS[1], subSet.first());
    Assert.assertEquals(ELEMS[2], subSet.last());
  }

  @Test
  public void testSubsetView_subsetOfSubset() {
    OrderStatisticSet<String> set = createSet(ELEMS);
    NavigableSet<String> subSet =
        set.subSet(ELEMS[1], /* fromInclusive= */ true, ELEMS[3], /*
    toInclusive=
    */ true);
    @Var
    NavigableSet<String> subSubSet =
        subSet.subSet(
            ELEMS[1], /* fromInclusive= */ true,
            ELEMS_BELOW[3], /* toInclusive= */ true);

    Assert.assertFalse(subSubSet.contains(ELEMS[0]));
    Assert.assertTrue(subSubSet.contains(ELEMS[1]));
    Assert.assertTrue(subSubSet.contains(ELEMS[2]));
    Assert.assertFalse(subSubSet.contains(ELEMS[3]));

    // make sure that the inclusive-flags are respected
    subSubSet =
        subSet.subSet(ELEMS[1], /* fromInclusive= */ true, ELEMS[3], /* toInclusive=*/ true);
    Assert.assertTrue(subSubSet.contains(ELEMS[1]));
    Assert.assertTrue(subSubSet.contains(ELEMS[2]));
    Assert.assertTrue(subSubSet.contains(ELEMS[3]));

    subSubSet =
        subSubSet.subSet(ELEMS[1], /* fromInclusive= */ false, ELEMS[3], /* toInclusive= */ false);
    Assert.assertFalse(subSubSet.contains(ELEMS[1]));
    Assert.assertTrue(subSubSet.contains(ELEMS[2]));
    Assert.assertFalse(subSubSet.contains(ELEMS[3]));
  }

  @Test
  public void testGetByRank_valid() {
    OrderStatisticSet<String> set = createSet(ELEMS);

    for (int i = 0; i < ELEMS.length; i++) {
      Assert.assertEquals(ELEMS[i], set.getByRank(i));
    }
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testGetByRank_outOfBounds() {
    OrderStatisticSet<String> set = createSet(ELEMS);

    try {
      String x = set.getByRank(-1);
      Assert.assertFalse(x != null);
      Assert.fail("Expected " + IndexOutOfBoundsException.class.getSimpleName());
    } catch (IndexOutOfBoundsException e) {
      String x = set.getByRank(ELEMS.length);
      Assert.assertFalse(x != null);
      Assert.fail("Expected " + IndexOutOfBoundsException.class.getSimpleName());
    }
  }

  @Test
  public void testRemoveByRank_valid() {
    OrderStatisticSet<String> set = createSet(ELEMS);

    set.removeByRank(2);

    Assert.assertFalse(set.contains(ELEMS[2]));
    Assert.assertTrue(set.contains(ELEMS[0]));
    Assert.assertTrue(set.contains(ELEMS[1]));
    Assert.assertTrue(set.contains(ELEMS[3]));

    set.removeByRank(0);

    Assert.assertFalse(set.contains(ELEMS[0]));
    Assert.assertTrue(set.contains(ELEMS[1]));
    Assert.assertTrue(set.contains(ELEMS[3]));

    set.removeByRank(set.size() - 1);
    Assert.assertFalse(set.contains(ELEMS[3]));
    Assert.assertTrue(set.contains(ELEMS[1]));

    set.removeByRank(0);
    Assert.assertTrue(set.isEmpty());
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testRemoveByRank_invalid() {
    OrderStatisticSet<String> set = createSet(ELEMS);

    try {
      set.removeByRank(-1);
    } catch (IndexOutOfBoundsException e1) {
      try {
        set.removeByRank(set.size());

      } catch (IndexOutOfBoundsException e2) {
        OrderStatisticSet<String> emptySet = createSet();
        emptySet.removeByRank(0);
      }
    }
  }
}
