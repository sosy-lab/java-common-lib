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

import com.google.common.collect.testing.SortedSetTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSortedSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.SetFeature;
import com.google.common.testing.SerializableTester;
import com.google.errorprone.annotations.Var;
import java.util.Arrays;
import java.util.Collections;
import java.util.SortedSet;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.junit.Assert;
import org.junit.Test;

public class SkipListTest {

  private static class TestSkipListGenerator extends TestStringSortedSetGenerator {

    @Override
    protected SortedSet<String> create(String[] pIntegers) {
      SkipList<String> list = new SkipList<>();
      // noinspection ResultOfMethodCallIgnored
      boolean changed = list.addAll(Arrays.asList(pIntegers));
      assert list.isEmpty() || changed;

      return list;
    }
  }

  public static junit.framework.Test suite() {
    TestSuite suite =
        SortedSetTestSuiteBuilder.using(new TestSkipListGenerator())
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
    SkipList<Integer> l1 = new SkipList<>();
    @Var SkipList<Integer> l2 = new SkipList<>();

    Assert.assertEquals(l1, l2);

    Collections.addAll(l1, 0, 5, 4, 3);
    Assert.assertNotEquals(l1, l2);

    Collections.addAll(l2, 3, 4, 0, 5);
    Assert.assertEquals(l1, l2);

    l2 = new SkipList<>();
    Collections.addAll(l2, 3, 4, 0, 5, 0, 5, 0);
    Assert.assertEquals(l1, l2);
  }

  @Test
  public void testSerialize() {
    SkipList<Integer> l = new SkipList<>();
    SerializableTester.reserializeAndAssert(l);

    for (int i = 100000; i >= 0; i--) {
      boolean changed = l.add(i);
      assert changed;
    }
    SerializableTester.reserializeAndAssert(l);
  }
}
