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
