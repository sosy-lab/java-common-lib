// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.TestStringListGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.testers.ListLastIndexOfTester;
import com.google.common.collect.testing.testers.ListListIteratorTester;
import com.google.common.collect.testing.testers.ListSubListTester;
import java.util.Arrays;
import java.util.List;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.junit.Test;

public class PersistentLinkedListTest {

  private static final TestStringListGenerator listGenerator =
      new TestStringListGenerator() {

        @Override
        protected List<String> create(String[] pElements) {
          return PersistentLinkedList.copyOf(pElements);
        }
      };

  public static junit.framework.Test suite() throws NoSuchMethodException {
    TestSuite suite = new TestSuite();
    suite.addTest(new JUnit4TestAdapter(PersistentLinkedListTest.class));

    suite.addTest(
        ListTestSuiteBuilder.using(listGenerator)
            .named("PersistentLinkedList")
            .withFeatures(CollectionFeature.KNOWN_ORDER, CollectionSize.ANY)
            .suppressing(
                // These tests all rely on a fully implemented ListIterator.
                ListLastIndexOfTester.class.getMethod("testFind_wrongType"),
                ListLastIndexOfTester.class.getMethod("testFind_no"),
                ListLastIndexOfTester.class.getMethod("testFind_yes"),
                ListLastIndexOfTester.class.getMethod("testFind_nullNotContainedAndUnsupported"),
                ListLastIndexOfTester.class.getMethod("testLastIndexOf_duplicate"),
                ListListIteratorTester.class.getMethod("testListIterator_tooLow"),
                ListListIteratorTester.class.getMethod("testListIterator_unmodifiable"),
                ListSubListTester.class.getMethod("testSubList_lastIndexOf"))
            .createTestSuite());

    return suite;
  }

  @Test
  public void testOf1() {
    assertThat(PersistentLinkedList.of("a")).containsExactly("a").inOrder();
  }

  @Test
  public void testOf2() {
    assertThat(PersistentLinkedList.of("a", "b")).containsExactly("a", "b").inOrder();
  }

  @Test
  public void testOf3() {
    assertThat(PersistentLinkedList.of("a", "b", "c")).containsExactly("a", "b", "c").inOrder();
  }

  @Test
  public void testOfVarArgs() {
    assertThat(PersistentLinkedList.of("a", "b", "c", "d"))
        .containsExactly("a", "b", "c", "d")
        .inOrder();
  }

  @Test
  public void testWithAll() {
    assertThat(PersistentLinkedList.of("d").withAll(Arrays.asList("a", "b", "c")))
        .containsExactly("a", "b", "c", "d")
        .inOrder();
  }

  @Test
  public void testReversed() {
    assertThat(PersistentLinkedList.of("a", "b", "c", "d").reversed())
        .containsExactly("d", "c", "b", "a")
        .inOrder();
  }
}
