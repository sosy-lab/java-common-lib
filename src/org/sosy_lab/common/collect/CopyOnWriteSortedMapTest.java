/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import java.util.Map.Entry;
import java.util.SortedMap;

import junit.framework.TestSuite;

import com.google.common.collect.testing.SortedMapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSortedMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.testers.MapClearTester;
import com.google.common.collect.testing.testers.MapEntrySetTester;
import com.google.common.collect.testing.testers.MapPutAllTester;
import com.google.common.collect.testing.testers.MapPutTester;
import com.google.common.collect.testing.testers.MapRemoveTester;
import com.google.common.collect.testing.testers.SortedMapNavigationTester;

public class CopyOnWriteSortedMapTest {

  private static final TestStringSortedMapGenerator mapGenerator = new TestStringSortedMapGenerator() {

    @Override
    protected SortedMap<String, String> create(Entry<String, String>[] pEntries) {
      CopyOnWriteSortedMap<String, String> result = CopyOnWriteSortedMap.copyOf(
          PathCopyingPersistentTreeMap.<String, String>of());
      for (Entry<String, String> entry : pEntries) {
        result.put(entry.getKey(), entry.getValue());
      }
      return result;
    }
  };

  public static junit.framework.Test suite() throws NoSuchMethodException, SecurityException {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(PathCopyingPersistentTreeMapTest.class);

    suite.addTest(SortedMapTestSuiteBuilder.using(mapGenerator)
        .named("CopyOnWriteSortedMap")
        .withFeatures(MapFeature.ALLOWS_NULL_VALUES,
                      // MapFeature.GENERAL_PURPOSE Not possible because collection views are unmodifiable
                      CollectionFeature.KNOWN_ORDER,
                      CollectionSize.ANY)

        // We throw ClassCastException as allowed by the JavaDoc of SortedMap
        .suppressing(MapEntrySetTester.class.getMethod("testContainsEntryWithIncomparableKey"))

        // Map is actually mutable, can't select the appropriate tests (see above)
        .suppressing(MapPutTester.class.getMethod("testPut_unsupportedNotPresent"))
        .suppressing(MapPutTester.class.getMethod("testPut_unsupportedPresentDifferentValue"))
        .suppressing(MapPutAllTester.class.getMethod("testPutAll_unsupportedSomePresent"))
        .suppressing(MapPutAllTester.class.getMethod("testPutAll_unsupportedNonePresent"))
        .suppressing(MapRemoveTester.class.getMethod("testRemove_unsupported"))
        .suppressing(MapClearTester.class.getMethod("testClear_unsupported"))

        // subMap is created lazily
        // TODO change this and enable test
        .suppressing(SortedMapNavigationTester.class.getMethod("testSubMapIllegal"))

        .createTestSuite());

    return suite;
  }
}
