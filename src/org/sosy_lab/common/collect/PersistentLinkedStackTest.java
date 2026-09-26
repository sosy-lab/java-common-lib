// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.sosy_lab.common.collect.Collections3.elementAndList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.testing.CollectionTestSuiteBuilder;
import com.google.common.collect.testing.IteratorFeature;
import com.google.common.collect.testing.IteratorTester;
import com.google.common.collect.testing.TestStringCollectionGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.testing.CollectorTester;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.Var;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.stream.Collector;
import java.util.stream.Stream;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Test;

public class PersistentLinkedStackTest {

  private static final ImmutableList<ImmutableList<String>> INPUTS =
      ImmutableList.of(
          ImmutableList.of(),
          ImmutableList.of("a"),
          ImmutableList.of("a", "b", "c"),
          ImmutableList.of("", "", "b"));

  public static junit.framework.Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(new JUnit4TestAdapter(PersistentLinkedStackTest.class));
    suite.addTest(
        CollectionTestSuiteBuilder.using(
                new TestStringCollectionGenerator() {
                  @Override
                  protected Collection<String> create(String[] pElements) {
                    @Var PersistentLinkedStack<String> stack = PersistentLinkedStack.of();
                    for (@Var int index = pElements.length - 1; index >= 0; index--) {
                      stack = stack.pushAndCopy(pElements[index]);
                    }
                    return stack.asTopDownIterable();
                  }
                })
            .named("PersistentLinkedStack.asTopDownIterable")
            .withFeatures(CollectionFeature.KNOWN_ORDER, CollectionSize.ANY)
            .createTestSuite());
    return suite;
  }

  @Test
  public void testIterator() {
    for (ImmutableList<String> input : INPUTS) {
      PersistentStack<String> stack = pushAll(input);
      Iterable<String> view = stack.asTopDownIterable();
      IteratorTester<String> tester =
          new IteratorTester<>(
              5,
              IteratorFeature.UNMODIFIABLE,
              input.reverse(),
              IteratorTester.KnownOrder.KNOWN_ORDER) {
            @Override
            protected Iterator<String> newTargetIterator() {
              return view.iterator();
            }
          };
      tester.test();
      tester.testForEachRemaining();
    }
  }

  @Test
  public void testForEachRemainingBoundaryCases() {
    List<String> remaining = new ArrayList<>();
    Iterator<String> iterator = PersistentLinkedStack.of("a").asTopDownIterable().iterator();
    iterator.forEachRemaining(remaining::add);
    assertThat(remaining).containsExactly("a");

    remaining.clear();
    iterator.forEachRemaining(remaining::add);
    PersistentLinkedStack.<String>of()
        .asTopDownIterable()
        .iterator()
        .forEachRemaining(remaining::add);
    assertThat(remaining).isEmpty();
  }

  @Test
  public void testIteratorsAreIndependent() {
    Iterable<String> view = pushAll(ImmutableList.of("a", "b")).asTopDownIterable();
    Iterator<String> first = view.iterator();
    Iterator<String> second = view.iterator();

    assertThat(first.next()).isEqualTo("b");
    assertThat(second.next()).isEqualTo("b");
    assertThat(first.next()).isEqualTo("a");
    assertThat(second.next()).isEqualTo("a");
  }

  @Test
  public void testViewsRemainOnOriginalVersion() {
    PersistentStack<String> stack = pushAll(ImmutableList.of("a", "b"));
    Collection<String> view = stack.asTopDownIterable();
    List<String> copy = stack.copyToList();
    PersistentStack<String> extended = stack.pushAndCopy("c");

    assertThat(extended.peek()).isEqualTo("c");
    assertThat(view).containsExactly("b", "a").inOrder();
    assertThat(view.spliterator().hasCharacteristics(Spliterator.ORDERED)).isTrue();
    assertThat(view.parallelStream().toList()).containsExactly("b", "a").inOrder();
    assertThat(copy).containsExactly("a", "b").inOrder();
  }

  @Test
  public void testPersistentVersions() {
    for (ImmutableList<String> input : INPUTS) {
      List<PersistentStack<String>> versions = new ArrayList<>();
      versions.add(PersistentLinkedStack.of());
      for (String value : input) {
        versions.add(versions.get(versions.size() - 1).pushAndCopy(value));
      }

      for (int i = 0; i < versions.size(); i++) {
        PersistentStack<String> version = versions.get(i);
        assertThat(version.size()).isEqualTo(i);
        assertThat(version.isEmpty()).isEqualTo(i == 0);
        assertThat(version.asTopDownIterable())
            .containsExactlyElementsIn(input.subList(0, i).reverse())
            .inOrder();
        if (i > 0) {
          assertThat(version.peek()).isEqualTo(input.get(i - 1));
          assertThat(version.popAndCopy()).isSameInstanceAs(versions.get(i - 1));
        }
        // Identity checks enforce the linked implementation's structural-sharing guarantee.
        for (@Var int count = 0; count <= version.size(); count++) {
          assertThat(version.takeBottom(count)).isSameInstanceAs(versions.get(count));
        }
        assertThrows(IndexOutOfBoundsException.class, () -> version.takeBottom(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> version.takeBottom(version.size() + 1));
      }
    }
  }

  @Test
  public void testBranchesSharePredecessor() {
    PersistentStack<String> predecessor = pushAll(ImmutableList.of("bottom", "middle"));
    PersistentStack<String> left = predecessor.pushAndCopy("left");
    PersistentStack<String> right = predecessor.pushAndCopy("right");

    assertThat(left.peek()).isEqualTo("left");
    assertThat(right.peek()).isEqualTo("right");
    assertThat(left.popAndCopy()).isSameInstanceAs(predecessor);
    assertThat(right.popAndCopy()).isSameInstanceAs(predecessor);
    assertThat(predecessor.asTopDownIterable()).containsExactly("middle", "bottom").inOrder();
  }

  @Test
  public void testCanonicalEmpty() {
    PersistentStack<String> empty = PersistentLinkedStack.of();
    PersistentStack<String> singleton = PersistentLinkedStack.of("a");

    assertThat(empty.isEmpty()).isTrue();
    assertThat(empty.size()).isEqualTo(0);
    assertThat(PersistentLinkedStack.<String>of()).isSameInstanceAs(empty);
    assertThat(empty.empty()).isSameInstanceAs(empty);
    assertThat(singleton.empty()).isSameInstanceAs(empty);
    assertThat(singleton.popAndCopy()).isSameInstanceAs(empty);
    assertThat(PersistentLinkedStack.copyOf(ImmutableList.<String>of())).isSameInstanceAs(empty);
    assertThat(Stream.<String>empty().collect(PersistentLinkedStack.toPersistentLinkedStack()))
        .isSameInstanceAs(empty);
  }

  @Test
  public void testEmptyOperationsThrow() {
    PersistentStack<String> empty = PersistentLinkedStack.of();

    assertThrows(NoSuchElementException.class, empty::peek);
    assertThrows(NoSuchElementException.class, empty::popAndCopy);
  }

  @Test
  public void testOfFactories() {
    assertThat(PersistentLinkedStack.of("a").asTopDownIterable()).containsExactly("a");
    assertThat(PersistentLinkedStack.of("a", "b").asTopDownIterable())
        .containsExactly("b", "a")
        .inOrder();
    assertThat(PersistentLinkedStack.of("a", "b", "c", "d").asTopDownIterable())
        .containsExactly("d", "c", "b", "a")
        .inOrder();
  }

  @Test
  public void testArrayIsOneElement() {
    String[] value = {"a", "b"};
    PersistentStack<String[]> stack = PersistentLinkedStack.of(value);

    assertThat(stack.size()).isEqualTo(1);
    assertThat(stack.peek()).isSameInstanceAs(value);
    assertThat(stack.asTopDownIterable().iterator().next()).isSameInstanceAs(value);
  }

  @Test
  public void testCopyOf() {
    for (ImmutableList<String> input : INPUTS) {
      PersistentStack<String> stack = PersistentLinkedStack.copyOf(input);

      assertThat(stack.size()).isEqualTo(input.size());
      assertThat(stack.asTopDownIterable()).containsExactlyElementsIn(input.reverse()).inOrder();
    }
  }

  @Test
  public void testCopyOfIterable() {
    /*
     * testCopyOf and testCopyOfDoesNotRetainInput use collections; this checks an Iterable input
     * that is not a Collection.
     */
    Iterable<String> input = () -> ImmutableList.of("a", "b", "c").iterator();
    PersistentLinkedStack<String> stack = PersistentLinkedStack.copyOf(input);

    assertThat(stack.size()).isEqualTo(3);
    assertThat(stack.asTopDownIterable()).containsExactly("c", "b", "a").inOrder();
  }

  @Test
  public void testCopyOfDoesNotRetainInput() {
    List<String> input = new ArrayList<>(ImmutableList.of("a", "b"));
    PersistentStack<String> stack = PersistentLinkedStack.copyOf(input);
    input.clear();

    assertThat(stack.asTopDownIterable()).containsExactly("b", "a").inOrder();
    assertThat(stack.size()).isEqualTo(2);
  }

  @Test
  public void testCopyToList() {
    /*
     * The generated suite tests asTopDownIterable, not copyToList. Check bottom-to-top order and
     * the unmodifiable result here.
     */
    for (ImmutableList<String> input : INPUTS) {
      PersistentStack<String> stack = pushAll(input);
      List<String> copy = stack.copyToList();

      assertThat(copy).containsExactlyElementsIn(input).inOrder();
      assertThrows(UnsupportedOperationException.class, () -> copy.add("other"));
    }
  }

  @Test
  public void testCollector() {
    CollectorTester<String, ?, PersistentLinkedStack<String>> tester =
        CollectorTester.of(PersistentLinkedStack.<String>toPersistentLinkedStack());
    for (ImmutableList<String> input : INPUTS) {
      tester.expectCollects(pushAll(input), input.toArray(new String[0]));
    }
  }

  @Test
  public void testCollectorCombinesMultiElementPartitions() {
    /*
     * testCollector never merges two multi-element accumulators; testCollectorWithParallelStream
     * does not fix partition sizes. This explicitly tests a two-by-two merge.
     */
    assertCombinesMultiElementPartitions(PersistentLinkedStack.<String>toPersistentLinkedStack());
  }

  @Test
  public void testCollectorWithParallelStream() {
    /*
     * testCollector and testCollectorCombinesMultiElementPartitions bypass Stream.collect();
     * testViewsRemainOnOriginalVersion tests only the view's stream. Check parallel collection
     * here.
     */
    PersistentLinkedStack<String> stack =
        Stream.of("a", "b", "c", "d")
            .parallel()
            .collect(PersistentLinkedStack.toPersistentLinkedStack());

    assertThat(stack.size()).isEqualTo(4);
    assertThat(stack.asTopDownIterable()).containsExactly("d", "c", "b", "a").inOrder();
  }

  private static <A> void assertCombinesMultiElementPartitions(
      Collector<String, A, PersistentLinkedStack<String>> collector) {
    A left = collector.supplier().get();
    collector.accumulator().accept(left, "a");
    collector.accumulator().accept(left, "b");
    A right = collector.supplier().get();
    collector.accumulator().accept(right, "c");
    collector.accumulator().accept(right, "d");

    PersistentLinkedStack<String> stack =
        collector.finisher().apply(collector.combiner().apply(left, right));

    assertThat(stack.size()).isEqualTo(4);
    assertThat(stack.asTopDownIterable()).containsExactly("d", "c", "b", "a").inOrder();
  }

  @Test
  public void testNulls() {
    assertThrows(NullPointerException.class, () -> PersistentLinkedStack.of((String) null));
    assertThrows(NullPointerException.class, () -> PersistentLinkedStack.of(null, "a"));
    assertThrows(NullPointerException.class, () -> PersistentLinkedStack.of("a", null));
    assertThrows(
        NullPointerException.class, () -> PersistentLinkedStack.of("a", "b", (String[]) null));
    assertThrows(NullPointerException.class, () -> PersistentLinkedStack.copyOf(null));
    assertThrows(NullPointerException.class, () -> PersistentLinkedStack.of().pushAndCopy(null));
    assertThrows(NullPointerException.class, () -> PersistentLinkedStack.of("a").pushAndCopy(null));
  }

  @Test
  public void testNullElements() {
    assertThrows(
        NullPointerException.class, () -> PersistentLinkedStack.of("a", "b", (String) null));
    assertThrows(
        NullPointerException.class,
        () -> PersistentLinkedStack.copyOf(Arrays.asList("a", null, "b")));
    assertThrows(
        NullPointerException.class,
        () -> Stream.of("a", null, "b").collect(PersistentLinkedStack.toPersistentLinkedStack()));
  }

  @Test
  public void testEquals() {
    PersistentStack<String> tail = PersistentLinkedStack.of("bottom");
    BigInteger value = new BigInteger("123456789012345678901234567890");
    BigInteger equalValue = new BigInteger("123456789012345678901234567890");

    new EqualsTester()
        .addEqualityGroup(PersistentLinkedStack.of(), new ListStack<>(ImmutableList.of()))
        .addEqualityGroup(PersistentLinkedStack.of("a"), new ListStack<>(ImmutableList.of("a")))
        .addEqualityGroup(
            tail.pushAndCopy("middle").pushAndCopy("top"),
            tail.pushAndCopy("middle").pushAndCopy("top"),
            pushAll(ImmutableList.of("bottom", "middle", "top")),
            new ListStack<>(ImmutableList.of("top", "middle", "bottom")))
        .addEqualityGroup(tail.pushAndCopy("middle").pushAndCopy("other"))
        .addEqualityGroup(tail.pushAndCopy("other").pushAndCopy("top"))
        .addEqualityGroup(pushAll(ImmutableList.of("other", "middle", "top")))
        .addEqualityGroup(pushAll(ImmutableList.of("top", "middle", "bottom")))
        .addEqualityGroup(tail.pushAndCopy("top"))
        .addEqualityGroup(
            pushAll(ImmutableList.of("a", "a")), new ListStack<>(ImmutableList.of("a", "a")))
        .addEqualityGroup(
            PersistentLinkedStack.of(value),
            PersistentLinkedStack.of(equalValue),
            new ListStack<>(ImmutableList.of(equalValue)))
        .addEqualityGroup(ImmutableList.of("top", "middle", "bottom"))
        .testEquals();
  }

  @Test
  public void testToString() {
    assertThat(PersistentLinkedStack.of().toString()).isEqualTo("[]");
    assertThat(PersistentLinkedStack.of("a").toString()).isEqualTo("[a]");
    assertThat(pushAll(ImmutableList.of("bottom", "middle", "top")).toString())
        .isEqualTo("[top, middle, bottom]");
  }

  @Test
  public void testSerializable() {
    for (ImmutableList<String> input : INPUTS) {
      PersistentStack<String> stack = pushAll(input);
      @Var PersistentStack<String> copy = SerializableTester.reserializeAndAssert(stack);

      assertThat(copy.size()).isEqualTo(input.size());
      for (String value : input.reverse()) {
        assertThat(copy.peek()).isEqualTo(value);
        copy = copy.popAndCopy();
      }
      assertThat(copy).isSameInstanceAs(PersistentLinkedStack.<String>of());
    }
  }

  @Test
  public void testLongStackSerialization() {
    int length = 10_000;
    @Var PersistentStack<Integer> stack = PersistentLinkedStack.of();
    for (int i = 0; i < length; i++) {
      stack = stack.pushAndCopy(i);
    }
    @Var PersistentStack<Integer> copy = SerializableTester.reserializeAndAssert(stack);

    for (int i = length - 1; i >= 0; i--) {
      assertThat(copy.size()).isEqualTo(i + 1);
      assertThat(copy.peek()).isEqualTo(i);
      copy = copy.popAndCopy();
    }
    assertThat(copy).isSameInstanceAs(PersistentLinkedStack.<Integer>of());
  }

  @Test
  public void testSerializationRejectsNonSerializableElement() throws IOException {
    try (ObjectOutputStream output = new ObjectOutputStream(new ByteArrayOutputStream())) {
      assertThrows(
          NotSerializableException.class,
          () -> output.writeObject(PersistentLinkedStack.of(new Object())));
    }
  }

  @Test
  public void testSerializationWithValidProxyValues() throws IOException, ClassNotFoundException {
    /*
     * testSerializable does not replace the proxy's value array; testSerializationRejectsNullArray
     * and testSerializationRejectsNullElement only check rejection. Verify replacement here.
     */
    byte[] serialized = serializeWithProxyValues(new Object[] {"top", "bottom"});
    PersistentLinkedStack<?> stack = deserializeStack(serialized);

    assertThat(stack.size()).isEqualTo(2);
    assertThat(stack.asTopDownIterable()).containsExactly("top", "bottom").inOrder();
  }

  @Test
  public void testSerializationRejectsNullArray() throws IOException {
    // Serialization must succeed; only deserialization should reject this data.
    byte[] serialized = serializeWithProxyValues(null);

    assertThrows(InvalidObjectException.class, () -> deserializeStack(serialized));
  }

  @Test
  public void testSerializationRejectsNullElement() throws IOException {
    // Serialization must succeed; only deserialization should reject this data.
    byte[] serialized = serializeWithProxyValues(new Object[] {"top", null, "bottom"});

    assertThrows(NullPointerException.class, () -> deserializeStack(serialized));
  }

  private static <T> PersistentLinkedStack<T> pushAll(Iterable<? extends T> input) {
    @Var PersistentLinkedStack<T> stack = PersistentLinkedStack.of();
    for (T value : input) {
      stack = stack.pushAndCopy(value);
    }
    return stack;
  }

  // The string-only fixture has exactly one object array: the proxy's element array.
  private static byte[] serializeWithProxyValues(@Nullable Object @Nullable [] values)
      throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream output =
        new ObjectOutputStream(bytes) {
          {
            enableReplaceObject(true);
          }

          @Override
          protected @Nullable Object replaceObject(Object object) {
            return object instanceof Object[] ? values : object;
          }
        }) {
      output.writeObject(PersistentLinkedStack.of("value"));
    }
    return bytes.toByteArray();
  }

  @SuppressWarnings("BanSerializableRead") // Reads only locally generated test data.
  private static PersistentLinkedStack<?> deserializeStack(byte[] bytes)
      throws IOException, ClassNotFoundException {
    try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      return (PersistentLinkedStack<?>) input.readObject();
    }
  }

  /** Independent value-based implementation used only for equality and hash-code tests. */
  @Immutable(containerOf = "T")
  private static final class ListStack<T> implements PersistentStack<T> {

    @Serial private static final long serialVersionUID = 1L;

    private final ImmutableList<T> values;

    private ListStack(ImmutableList<T> pValues) {
      values = pValues;
    }

    @Override
    public PersistentStack<T> pushAndCopy(T value) {
      return new ListStack<>(elementAndList(value, values));
    }

    @Override
    public PersistentStack<T> popAndCopy() {
      if (isEmpty()) {
        throw new NoSuchElementException();
      }
      return new ListStack<>(values.subList(1, values.size()));
    }

    @Override
    public T peek() {
      if (isEmpty()) {
        throw new NoSuchElementException();
      }
      return values.get(0);
    }

    @Override
    public PersistentStack<T> empty() {
      return new ListStack<>(ImmutableList.of());
    }

    @Override
    public boolean isEmpty() {
      return values.isEmpty();
    }

    @Override
    public int size() {
      return values.size();
    }

    @Override
    public ImmutableList<T> asTopDownIterable() {
      return values;
    }

    @Override
    public ImmutableList<T> copyToList() {
      return values.reverse();
    }

    @Override
    public PersistentStack<T> takeBottom(int count) {
      // Currently not needed in tests
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      return obj instanceof PersistentStack<?> other
          && values.equals(ImmutableList.copyOf(other.asTopDownIterable()));
    }

    @Override
    public int hashCode() {
      return values.hashCode();
    }
  }
}
