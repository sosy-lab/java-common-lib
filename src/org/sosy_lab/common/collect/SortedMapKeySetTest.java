// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.common.collect.testing.NavigableSetTestSuiteBuilder;
import com.google.common.collect.testing.TestSortedSetGenerator;
import com.google.common.collect.testing.TestStringSortedSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.errorprone.annotations.Var;
import java.util.SortedSet;
import junit.framework.Test;
import junit.framework.TestSuite;

public class SortedMapKeySetTest {

  private SortedMapKeySetTest() {}

  private static class OrderStatisticsSetProxyFactory extends TestStringSortedSetGenerator {

    @SuppressWarnings("unchecked")
    @Override
    protected SortedSet<String> create(String[] pStrings) {
      @Var PersistentSortedMap<String, Boolean> map = PathCopyingPersistentTreeMap.of();
      for (String s : pStrings) {
        map = map.putAndCopy(s, Boolean.TRUE);
      }
      return new SortedMapKeySet<>((OurSortedMap<String, ?>) map);
    }
  }

  public static Test suite() {
    TestSortedSetGenerator<String> testSetGenerator = new OrderStatisticsSetProxyFactory();

    TestSuite suite =
        NavigableSetTestSuiteBuilder.using(testSetGenerator)
            .named("SortedMapKeySet")
            .withFeatures(
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS,
                CollectionSize.ANY)
            .createTestSuite();

    return suite;
  }
}
