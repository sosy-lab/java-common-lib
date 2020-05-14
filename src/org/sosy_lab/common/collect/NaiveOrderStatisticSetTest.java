// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.truth.Truth.assertThat;

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
                CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS)
            .createTestSuite();

    suite.addTest(new JUnit4TestAdapter(NaiveOrderStatisticSetTest.class));

    return suite;
  }

  @Test
  public void testNoReference() {
    NavigableSet<String> testCollection = new TreeSet<>(ImmutableList.of("a", "b", "bc", "d"));
    OrderStatisticSet<String> set = NaiveOrderStatisticSet.createSetWithSameOrder(testCollection);

    testCollection.remove("a");
    assertThat(testCollection).doesNotContain("a");
    assertThat(set).contains("a");
    set.remove("bc");
    assertThat(testCollection).contains("bc");
    assertThat(set).doesNotContain("bc");
  }
}
