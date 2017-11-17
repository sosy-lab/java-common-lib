/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
import com.google.common.collect.testing.NavigableSetTestSuiteBuilder;
import com.google.common.collect.testing.TestSortedSetGenerator;
import com.google.common.collect.testing.TestStringSortedSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.SetFeature;
import com.google.common.testing.SerializableTester;
import com.google.errorprone.annotations.Var;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.NavigableSet;
import java.util.SortedSet;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.junit.Assert;
import org.junit.Test;

public class SkipListTest {

  private static class TestSkipListGenerator extends TestStringSortedSetGenerator {

    @Override
    protected SortedSet<String> create(String[] pStrings) {
      SkipList<String> list = SkipList.create();
      // noinspection ResultOfMethodCallIgnored
      boolean changed = list.addAll(Arrays.asList(pStrings));
      assert list.isEmpty() || changed;

      return list;
    }
  }

  public static junit.framework.Test suite() {
    TestSortedSetGenerator<String> testSetGenerator = new TestSkipListGenerator();

    TestSuite suite =
        NavigableSetTestSuiteBuilder.using(testSetGenerator)
            .named("SkipList Test Suite")
            .withFeatures(
                CollectionSize.ANY,
                SetFeature.GENERAL_PURPOSE,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS,
                CollectionFeature.SUBSET_VIEW)
            .createTestSuite();

    suite.addTest(new JUnit4TestAdapter(SkipListTest.class));

    return suite;
  }

  @Test
  public void testEquals() {
    SkipList<Integer> l1 = SkipList.create();
    @Var SkipList<Integer> l2 = SkipList.create();

    Assert.assertEquals(l1, l2);

    Collections.addAll(l1, 0, 5, 4, 3);
    Assert.assertNotEquals(l1, l2);

    Collections.addAll(l2, 3, 4, 0, 5);
    Assert.assertEquals(l1, l2);

    l2 = SkipList.create();
    Collections.addAll(l2, 3, 4, 0, 5, 0, 5, 0);
    Assert.assertEquals(l1, l2);
  }

  @Test
  public void testSerialize() {
    SkipList<Integer> l = SkipList.create();
    SerializableTester.reserializeAndAssert(l);

    for (int i = 100000; i >= 0; i--) {
      l.add(i);
    }
    SerializableTester.reserializeAndAssert(l);
  }

  @Test
  public void testSubsetView_mutation() {
    Collection<Integer> testCollection = ImmutableList.of(1, 9, 99, 999);
    NavigableSet<Integer> set = SkipList.create(testCollection);
    NavigableSet<Integer> subSet = set.subSet(10, true, 100, true);

    Integer toAdd = 50;

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
    Collection<Integer> testCollection = ImmutableList.of(1, 9, 99, 999);
    NavigableSet<Integer> set = SkipList.create(testCollection);
    NavigableSet<Integer> subSet = set.subSet(10, true, 100, true);

    try {
      subSet.add(9);
    } catch (IllegalArgumentException e1) {
      try {
        subSet.add(101);
      } catch (IllegalArgumentException e2) {
        Collection<Integer> toAdd = ImmutableList.of(20, 30, 101, 50);
        subSet.addAll(toAdd);
      }
    }
  }

  @Test
  public void testSubsetView_outOfBounds_remove() {
    Integer toRemove1 = 20;
    Integer toRemove2 = 30;
    Collection<Integer> testCollection = ImmutableList.of(1, 9, toRemove1, toRemove2, 99, 999);
    NavigableSet<Integer> set = SkipList.create(testCollection);
    NavigableSet<Integer> subSet = set.subSet(10, true, 100, true);

    subSet.remove(9);
    subSet.remove(999);

    Assert.assertTrue(set.contains(9));
    Assert.assertTrue(set.contains(999));

    Collection<Integer> toRemove = ImmutableList.of(toRemove1, toRemove2, 9);
    subSet.removeAll(toRemove);

    Assert.assertTrue(set.contains(9));
    Assert.assertFalse(set.contains(toRemove1));
    Assert.assertFalse(set.contains(toRemove2));
  }

  @Test
  public void testSubsetView_outOfBounds_contains() {
    Collection<Integer> testCollection = ImmutableList.of(1, 9, 99, 999);
    NavigableSet<Integer> set = SkipList.create(testCollection);
    @Var NavigableSet<Integer> subSet = set.subSet(10, true, 100, true);

    Assert.assertFalse(subSet.contains(9));
    Assert.assertFalse(subSet.contains(999));

    subSet = set.subSet(9, false, 99, false);

    Assert.assertFalse(subSet.contains(9));
    Assert.assertFalse(subSet.contains(99));

    subSet = set.subSet(9, true, 99, true);

    Assert.assertTrue(subSet.contains(9));
    Assert.assertTrue(subSet.contains(99));
  }

  @Test
  public void testSubsetView_descending() {
    Collection<Integer> testCollection = ImmutableList.of(1, 9, 99, 999);
    NavigableSet<Integer> set = SkipList.create(testCollection);
    @Var NavigableSet<Integer> subSet = set.subSet(9, true, 99, true).descendingSet();

    Assert.assertEquals(subSet.pollFirst(), Integer.valueOf(99));
    Assert.assertEquals(subSet.pollLast(), Integer.valueOf(9));

    subSet = subSet.descendingSet();
    Assert.assertEquals(subSet.pollFirst(), Integer.valueOf(9));
    Assert.assertEquals(subSet.pollLast(), Integer.valueOf(99));
  }

  @Test
  public void testSubsetView_subsetOfSubset() {
    Collection<Integer> testCollection = ImmutableList.of(1, 9, 99, 999);
    NavigableSet<Integer> set = SkipList.create(testCollection);
    NavigableSet<Integer> subSet = set.subSet(1, true, 99, true);
    @Var NavigableSet<Integer> subSubSet = subSet.subSet(9, true, 1000, true);

    Assert.assertFalse(subSubSet.contains(1));
    Assert.assertTrue(subSubSet.contains(9));
    Assert.assertTrue(subSubSet.contains(99));
    Assert.assertFalse(subSubSet.contains(999));

    subSubSet = subSet.subSet(1, false, 99, false);
    Assert.assertFalse(subSubSet.contains(1));
    Assert.assertTrue(subSubSet.contains(9));
    Assert.assertFalse(subSubSet.contains(99));

    // make sure that the inclusive-flags are respected
    subSubSet = subSubSet.subSet(1, true, 99, true);
    Assert.assertFalse(subSubSet.contains(1));
    Assert.assertTrue(subSubSet.contains(9));
    Assert.assertFalse(subSubSet.contains(99));
  }
}
