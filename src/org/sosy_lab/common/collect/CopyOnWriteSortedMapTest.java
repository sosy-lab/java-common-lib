// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.testing.AbstractTester;
import com.google.common.collect.testing.FeatureSpecificTestSuiteBuilder;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.NavigableMapTestSuiteBuilder;
import com.google.common.collect.testing.OneSizeTestContainerGenerator;
import com.google.common.collect.testing.TestMapGenerator;
import com.google.common.collect.testing.TestStringSortedMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.testers.MapEntrySetTester;
import com.google.common.collect.testing.testers.MapReplaceAllTester;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Stream;
import junit.framework.TestSuite;

public class CopyOnWriteSortedMapTest {

  private CopyOnWriteSortedMapTest() {}

  /** A delegating {@link FeatureSpecificTestSuiteBuilder} that overrides the set of features. */
  private static class FeatureOverrideTestSuiteBuilder<K, V>
      extends FeatureSpecificTestSuiteBuilder<
          FeatureOverrideTestSuiteBuilder<K, V>,
          OneSizeTestContainerGenerator<Map<K, V>, Map.Entry<K, V>>> {

    private final FeatureSpecificTestSuiteBuilder<
            ?, ? extends OneSizeTestContainerGenerator<Map<K, V>, Map.Entry<K, V>>>
        delegate;
    private final Set<Feature<?>> features;

    FeatureOverrideTestSuiteBuilder(
        FeatureSpecificTestSuiteBuilder<
                ?, ? extends OneSizeTestContainerGenerator<Map<K, V>, Map.Entry<K, V>>>
            pDelegate,
        Set<Feature<?>> pFeatures) {
      delegate = pDelegate;
      features = ImmutableSet.copyOf(pFeatures);
    }

    @Override
    public Set<Feature<?>> getFeatures() {
      return features;
    }

    @Override
    public String getName() {
      return delegate.getName();
    }

    @Override
    public Set<Method> getSuppressedTests() {
      return delegate.getSuppressedTests();
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected List<Class<? extends AbstractTester>> getTesters() {
      throw new UnsupportedOperationException();
    }

    @Override
    public OneSizeTestContainerGenerator<Map<K, V>, Map.Entry<K, V>> getSubjectGenerator() {
      return delegate.getSubjectGenerator();
    }
  }

  /**
   * A {@link NavigableMapTestSuiteBuilder} that ensures that submaps, entry set etc. are not tested
   * for mutability.
   */
  private static class UnmodifiableViewSortedMapTestSuiteBuilder<K, V>
      extends NavigableMapTestSuiteBuilder<K, V> {

    @Override
    protected MapTestSuiteBuilder<K, V> usingGenerator(TestMapGenerator<K, V> pSubjectGenerator) {
      return super.usingGenerator(pSubjectGenerator);
    }

    @Override
    protected List<TestSuite> createDerivedSuites(
        FeatureSpecificTestSuiteBuilder<
                ?, ? extends OneSizeTestContainerGenerator<Map<K, V>, Map.Entry<K, V>>>
            pParentBuilder) {

      Set<Feature<?>> features = new HashSet<>(pParentBuilder.getFeatures());
      features.remove(MapFeature.GENERAL_PURPOSE);
      features.remove(MapFeature.SUPPORTS_PUT);
      features.remove(MapFeature.SUPPORTS_REMOVE);
      features.remove(CollectionFeature.REMOVE_OPERATIONS);
      features.remove(CollectionFeature.SUPPORTS_ADD);
      features.remove(CollectionFeature.SUPPORTS_REMOVE);
      features.remove(CollectionFeature.SUPPORTS_ITERATOR_REMOVE);
      features.remove(CollectionFeature.GENERAL_PURPOSE);

      return super.createDerivedSuites(
          new FeatureOverrideTestSuiteBuilder<>(pParentBuilder, features));
    }
  }

  private static final TestStringSortedMapGenerator mapGenerator =
      new TestStringSortedMapGenerator() {

        @Override
        protected SortedMap<String, String> create(Map.Entry<String, String>[] pEntries) {
          CopyOnWriteSortedMap<String, String> result =
              CopyOnWriteSortedMap.copyOf(PathCopyingPersistentTreeMap.<String, String>of());
          Stream.of(pEntries).forEach((entry) -> result.put(entry.getKey(), entry.getValue()));
          return result;
        }
      };

  public static junit.framework.Test suite() throws NoSuchMethodException {
    // Our collection views are unmodifiable, so we need special TestSuiteBuilder
    return new UnmodifiableViewSortedMapTestSuiteBuilder<String, String>()
        .usingGenerator(mapGenerator)
        .named("CopyOnWriteSortedMap")
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            MapFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY)

        // We do not support Map.Entry.setValue()
        .suppressing(MapEntrySetTester.class.getMethod("testSetValue"))
        .suppressing(MapEntrySetTester.class.getMethod("testSetValueWithNullValuesPresent"))
        .suppressing(MapReplaceAllTester.class.getMethod("testReplaceAllPreservesOrder"))
        .suppressing(MapReplaceAllTester.class.getMethod("testReplaceAllRotate"))
        .createTestSuite();
  }
}
