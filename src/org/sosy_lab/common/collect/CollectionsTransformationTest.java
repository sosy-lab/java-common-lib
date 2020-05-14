// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import org.junit.Test;
import org.mockito.Mockito;

public class CollectionsTransformationTest {

  /**
   * Tests that ensure that for certain standard techniques for creating immutable transformed
   * copies of a collection the transformation is applied only once (i.e., only one pass is made
   * over the input collection).
   */
  private static void testTransformCalledOnlyOnce(
      Function<List<AtomicInteger>, Collection<AtomicInteger>> inputSupplier,
      BiFunction<Collection<AtomicInteger>, Function<AtomicInteger, String>, Collection<String>>
          transformer) {
    AtomicInteger i1 = new AtomicInteger();
    AtomicInteger i2 = new AtomicInteger(10);
    AtomicInteger i3 = new AtomicInteger(20);
    Collection<AtomicInteger> input = inputSupplier.apply(ImmutableList.of(i1, i2, i3));

    Collection<String> result = transformer.apply(input, e -> "" + e.incrementAndGet());

    // Check twice to ensure transform function is not called again
    assertThat(result).containsAtLeast("1", "11", "21");
    assertThat(result).containsAtLeast("1", "11", "21");

    // Check how often transform function was called
    assertThat(i1.get()).isEqualTo(1);
    assertThat(i2.get()).isEqualTo(11);
    assertThat(i3.get()).isEqualTo(21);
  }

  @Test
  @SuppressWarnings("JdkObsolete")
  public void testTransformedListCopy() {
    testTransformCalledOnlyOnce(ImmutableList::copyOf, Collections3::transformedImmutableListCopy);
    testTransformCalledOnlyOnce(Lists::newArrayList, Collections3::transformedImmutableListCopy);
    testTransformCalledOnlyOnce(Sets::newHashSet, Collections3::transformedImmutableListCopy);

    testTransformCalledOnlyOnce(
        ImmutableList::copyOf,
        (input, transformer) -> new ArrayList<>(Collections2.transform(input, transformer)));
    testTransformCalledOnlyOnce(
        ImmutableList::copyOf,
        (input, transformer) -> new LinkedList<>(Collections2.transform(input, transformer)));
  }

  @Test
  public void testTransformedSetCopy() {
    testTransformCalledOnlyOnce(ImmutableList::copyOf, Collections3::transformedImmutableSetCopy);
    testTransformCalledOnlyOnce(Lists::newArrayList, Collections3::transformedImmutableSetCopy);
    testTransformCalledOnlyOnce(Sets::newHashSet, Collections3::transformedImmutableSetCopy);

    testTransformCalledOnlyOnce(
        ImmutableList::copyOf,
        (input, transformer) -> new HashSet<>(Collections2.transform(input, transformer)));
    testTransformCalledOnlyOnce(
        ImmutableList::copyOf,
        (input, transformer) -> new TreeSet<>(Collections2.transform(input, transformer)));
  }

  private static void testMapTransformCalledOnlyOnce(
      Function<Map<String, AtomicInteger>, Map<String, AtomicInteger>> inputSupplier,
      BiFunction<Map<String, AtomicInteger>, Function<AtomicInteger, String>, Map<String, String>>
          transformer) {
    AtomicInteger i1 = new AtomicInteger();
    AtomicInteger i2 = new AtomicInteger(10);
    AtomicInteger i3 = new AtomicInteger(20);
    Map<String, AtomicInteger> input =
        inputSupplier.apply(ImmutableMap.of("a", i1, "b", i2, "c", i3));

    Map<String, String> result = transformer.apply(input, e -> "" + e.incrementAndGet());

    // Check twice to ensure transform function is not called again
    assertThat(result).containsExactly("a", "1", "b", "11", "c", "21");
    assertThat(result).containsExactly("a", "1", "b", "11", "c", "21");

    // Check how often transform function was called
    assertThat(i1.get()).isEqualTo(1);
    assertThat(i2.get()).isEqualTo(11);
    assertThat(i3.get()).isEqualTo(21);
  }

  @Test
  public void testTransformedMapCopy() {
    testMapTransformCalledOnlyOnce(
        ImmutableMap::copyOf,
        (input, transformer) -> ImmutableMap.copyOf(Maps.transformValues(input, transformer)));
    testMapTransformCalledOnlyOnce(
        Maps::newHashMap,
        (input, transformer) -> ImmutableMap.copyOf(Maps.transformValues(input, transformer)));
    testMapTransformCalledOnlyOnce(
        input -> new TreeMap<>(input),
        (input, transformer) -> ImmutableMap.copyOf(Maps.transformValues(input, transformer)));

    testMapTransformCalledOnlyOnce(
        ImmutableMap::copyOf,
        (input, transformer) -> new HashMap<>(Maps.transformValues(input, transformer)));
    testMapTransformCalledOnlyOnce(
        ImmutableMap::copyOf,
        (input, transformer) -> new TreeMap<>(Maps.transformValues(input, transformer)));
  }

  /**
   * Test our assumption that {@link Collections3#transformedImmutableListCopy(java.util.Collection,
   * com.google.common.base.Function)} actually calls {@link Collection#size()} on the original
   * collection.
   *
   * <p>Strictly speaking, this does not guarantee that the resulting list is appropriately
   * pre-sized, but it is a strong indicator.
   *
   * <p>If this test fails, it is probably an indicator that something changed in Guava or the JDK
   * and we should re-evaluate whether the assumption that this method does an efficient (pre-sized)
   * copy still holds.
   */
  @Test
  public void testTransformedImmutableListCopyUsesSize() {
    List<String> list = Mockito.spy(ImmutableList.of("a", "b"));

    @SuppressWarnings("unused")
    List<String> transformed =
        Collections3.transformedImmutableListCopy(list, Functions.identity());
    Mockito.verify(list).size();
  }

  /**
   * Test our assumption that copying a list via {@code Stream.map().collect()} does not call {@link
   * Collection#size()} on the original collection.
   *
   * <p>This basically means that the copy cannot be made into a pre-sized list but needs an
   * incrementally growing temporary collection and thus repeated copying.
   *
   * <p>If this test fails, it is probably an indicator that something changed in Guava or the JDK
   * and we should re-evaluate whether the assumption that this method does not do an efficient
   * (pre-sized) copy still holds.
   */
  @Test
  public void testStreamMapDoesNotUseSize() {
    List<String> list = Mockito.spy(ImmutableList.of("a", "b"));

    @SuppressWarnings("unused")
    List<String> transformed = list.stream().map(s -> s).collect(ImmutableList.toImmutableList());
    Mockito.verify(list, Mockito.never()).size();
  }

  /**
   * Test our assumption that copying a list via {@code FluentIterable.transform().toList()} does
   * not call {@link Collection#size()} on the original collection.
   *
   * <p>This basically means that the copy cannot be made into a pre-sized list but needs an
   * incrementally growing temporary collection and thus repeated copying.
   *
   * <p>If this test fails, it is probably an indicator that something changed in Guava or the JDK
   * and we should re-evaluate whether the assumption that this method does not do an efficient
   * (pre-sized) copy still holds.
   */
  @Test
  public void testFluentIterableTransformDoesNotUseSize() {
    List<String> list = Mockito.spy(ImmutableList.of("a", "b"));

    @SuppressWarnings("unused")
    List<String> transformed = FluentIterable.from(list).transform(Functions.identity()).toList();
    Mockito.verify(list, Mockito.never()).size();
    Mockito.verify(list).iterator();
  }
}
