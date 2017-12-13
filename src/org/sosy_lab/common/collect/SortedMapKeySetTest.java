/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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

import com.google.common.collect.testing.NavigableSetTestSuiteBuilder;
import com.google.common.collect.testing.TestSortedSetGenerator;
import com.google.common.collect.testing.TestStringSortedSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.errorprone.annotations.Var;
import java.util.SortedSet;
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

  public static junit.framework.Test suite() {
    TestSortedSetGenerator<String> testSetGenerator = new OrderStatisticsSetProxyFactory();

    TestSuite suite =
        NavigableSetTestSuiteBuilder.using(testSetGenerator)
            .named("SortedMapKeySet")
            .withFeatures(CollectionFeature.KNOWN_ORDER, CollectionSize.ANY)
            .createTestSuite();

    return suite;
  }
}
