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
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.SetFeature;
import java.util.Arrays;
import java.util.NavigableSet;
import java.util.TreeSet;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.junit.Assert;
import org.junit.Test;

public final class NaiveOrderStatisticSetTest extends OrderStatisticSetTestSuite {

  private static class OrderStatisticsSetProxyFactory extends OrderStatisticSetFactory {

    @Override
    protected OrderStatisticSet<String> create(String[] pStrings) {
      NaiveOrderStatisticSet<String> list = createSet();
      boolean changed = list.addAll(Arrays.asList(pStrings));
      assert list.isEmpty() || changed;

      return list;
    }

    private static <T> NaiveOrderStatisticSet<T> createSet() {
      return NaiveOrderStatisticSet.createSet();
    }
  }

  public NaiveOrderStatisticSetTest() {
    super(new OrderStatisticsSetProxyFactory());
  }

  public static junit.framework.Test suite() {
    TestSortedSetGenerator<String> testSetGenerator = new OrderStatisticsSetProxyFactory();

    TestSuite suite =
        NavigableSetTestSuiteBuilder.using(testSetGenerator)
            .named("NaiveOrderStatisticSet")
            .withFeatures(
                CollectionSize.ANY,
                SetFeature.GENERAL_PURPOSE,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS,
                CollectionFeature.SUBSET_VIEW)
            .createTestSuite();

    suite.addTest(new JUnit4TestAdapter(NaiveOrderStatisticSetTest.class));

    return suite;
  }

  @Test
  public void testNoReference() {
    NavigableSet<String> testCollection = new TreeSet<>(ImmutableList.of("a", "b", "bc", "d"));
    OrderStatisticSet<String> set = NaiveOrderStatisticSet.createSetWithSameOrder(testCollection);

    testCollection.remove("a");
    Assert.assertFalse(testCollection.contains("a"));
    Assert.assertTrue(set.contains("a"));
    set.remove("bc");
    Assert.assertTrue(testCollection.contains("bc"));
    Assert.assertFalse(set.contains("bc"));
  }
}
