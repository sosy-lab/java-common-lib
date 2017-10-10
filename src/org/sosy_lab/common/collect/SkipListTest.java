/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.common.collect;

import com.google.common.collect.testing.SortedSetTestSuiteBuilder;
import com.google.common.collect.testing.TestIntegerSortedSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import java.util.Arrays;
import java.util.SortedSet;

public class SkipListTest {

  private SkipListTest() {}

  private static class TestSkipListGenerator extends TestIntegerSortedSetGenerator {

    @Override
    protected SortedSet<Integer> create(Integer[] pIntegers) {
      SkipList<Integer> list = new SkipList<>();
      // noinspection ResultOfMethodCallIgnored
      boolean changed = list.addAll(Arrays.asList(pIntegers));
      assert list.isEmpty() || changed;

      return list;
    }
  }

  public static junit.framework.Test suite() throws NoSuchMethodException {
    return SortedSetTestSuiteBuilder.using(new TestSkipListGenerator())
        .named("SkipList Test Suite")
        .withFeatures(
            CollectionSize.ANY,
            CollectionFeature.SUPPORTS_ADD,
            CollectionFeature.SUPPORTS_REMOVE,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
            CollectionFeature.NON_STANDARD_TOSTRING,
            CollectionFeature.SUBSET_VIEW)
        .createTestSuite();
  }
}
