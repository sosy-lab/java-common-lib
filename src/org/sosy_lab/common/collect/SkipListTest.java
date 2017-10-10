package org.sosy_lab.common.collect;

import com.google.common.collect.testing.SortedSetTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSortedSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
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
                CollectionFeature.SUPPORTS_ADD,
                CollectionFeature.SUPPORTS_REMOVE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionFeature.NON_STANDARD_TOSTRING,
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
}
