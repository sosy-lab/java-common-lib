/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.common;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Streams;
import java.util.Comparator;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/** Utilities for {@link Optional}. */
public final class Optionals {

  private Optionals() {}

  /**
   * Convert an {@link Optional} to a Guava {@link com.google.common.base.Optional}.
   *
   * @deprecated use {@link com.google.common.base.Optional#fromJavaUtil(Optional)}
   */
  @Deprecated
  public static <T> com.google.common.base.Optional<T> toGuavaOptional(Optional<T> optional) {
    return com.google.common.base.Optional.fromJavaUtil(checkNotNull(optional));
  }

  /**
   * Convert a Guava {@link com.google.common.base.Optional} to an {@link Optional}.
   *
   * @deprecated use {@link com.google.common.base.Optional#toJavaUtil()}
   */
  @Deprecated
  public static <T> Optional<T> fromGuavaOptional(com.google.common.base.Optional<T> optional) {
    return optional.toJavaUtil();
  }

  /**
   * Return a set that is either empty or contains the present instance of the {@link Optional}.
   *
   * <p>Can be used with {@link FluentIterable#transformAndConcat(com.google.common.base.Function)}
   * to project an {@link Iterable} to the present instances. However, using {@link
   * #presentInstances(Iterable)} would be more efficient.
   *
   * @param optional An Optional.
   * @return A set with size at most one.
   */
  public static <T> ImmutableSet<T> asSet(Optional<T> optional) {
    return optional.isPresent() ? ImmutableSet.of(optional.get()) : ImmutableSet.of();
  }

  /**
   * Return a stream that is either empty or contains the present instance of the optional.
   *
   * <p>Can be used with {@link Stream#flatMap(java.util.function.Function)} to project a stream to
   * the present instances. However, using {@link #presentInstances(Stream)} would be more
   * efficient.
   *
   * @param optional An Optional.
   * @return A stream with size at most one.
   * @deprecated use {@link Streams#stream(Optional)}
   */
  @Deprecated
  public static <T> Stream<T> asStream(Optional<T> optional) {
    return Streams.stream(optional);
  }

  /** Get an {@link Iterable} of the present instances of an iterable of {@link Optional}s. */
  public static <T> FluentIterable<T> presentInstances(Iterable<Optional<T>> iterable) {
    return FluentIterable.from(iterable).filter(Optional::isPresent).transform(Optional::get);
  }

  /** Get a {@link Stream} of the present instances of a stream of {@link Optional}s. */
  public static <T> Stream<T> presentInstances(Stream<Optional<T>> stream) {
    return stream.filter(Optional::isPresent).map(Optional::get);
  }

  /** Get a {@link IntStream} of the present integers of a stream of {@link OptionalInt}s. */
  public static IntStream presentInts(Stream<OptionalInt> stream) {
    return stream.filter(OptionalInt::isPresent).mapToInt(OptionalInt::getAsInt);
  }

  /** Get a {@link LongStream} of the present longs of a stream of {@link OptionalLong}s. */
  public static LongStream presentLongs(Stream<OptionalLong> stream) {
    return stream.filter(OptionalLong::isPresent).mapToLong(OptionalLong::getAsLong);
  }

  /** Get a {@link DoubleStream} of the present doubles of a stream of {@link OptionalDouble}s. */
  public static DoubleStream presentDoubles(Stream<OptionalDouble> stream) {
    return stream.filter(OptionalDouble::isPresent).mapToDouble(OptionalDouble::getAsDouble);
  }

  /**
   * Return a {@link Ordering} for {@link Optional} that compares empty optionals as smaller than
   * all non-empty instances, and compares present values using their natural order.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Comparable<T>> Comparator<Optional<T>> comparingEmptyFirst() {
    return (Comparator<Optional<T>>) OptionalComparators.NATURAL_EMTPY_FIRST;
  }

  /**
   * Return a {@link Ordering} for {@link Optional} that compares empty optionals as smaller than
   * all non-empty instances, and compares present values using the given comparator.
   */
  public static <T> Comparator<Optional<T>> comparingEmptyFirst(Comparator<? super T> comparator) {
    return new OptionalComparators.OptionalComparator<>(true, comparator);
  }

  /**
   * Return a {@link Ordering} for {@link Optional} that compares empty optionals as larger than all
   * non-empty instances, and compares present values using their natural order.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Comparable<T>> Comparator<Optional<T>> comparingEmptyLast() {
    return (Comparator<Optional<T>>) OptionalComparators.NATURAL_EMTPY_LAST;
  }

  /**
   * Return a {@link Ordering} for {@link Optional} that compares empty optionals as larger than all
   * non-empty instances, and compares present values using the given comparator.
   */
  public static <T> Comparator<Optional<T>> comparingEmptyLast(Comparator<? super T> comparator) {
    return new OptionalComparators.OptionalComparator<>(false, comparator);
  }

  /**
   * Return a {@link Ordering} for {@link OptionalInt} that compares empty optionals as smaller than
   * all non-empty instances, and compares present integers using their natural order.
   */
  public static Ordering<OptionalInt> comparingIntEmptyFirst() {
    return OptionalComparators.INT_EMTPY_FIRST;
  }

  /**
   * Return a {@link Ordering} for {@link OptionalInt} that compares empty optionals as larger than
   * all non-empty instances, and compares present integers using their natural order.
   */
  public static Ordering<OptionalInt> comparingIntEmptyLast() {
    return OptionalComparators.INT_EMTPY_LAST;
  }

  /**
   * Return a {@link Ordering} for {@link OptionalLong} that compares empty optionals as smaller
   * than all non-empty instances, and compares present longs using their natural order.
   */
  public static Ordering<OptionalLong> comparingLongEmptyFirst() {
    return OptionalComparators.LONG_EMTPY_FIRST;
  }

  /**
   * Return a {@link Ordering} for {@link OptionalLong} that compares empty optionals as larger than
   * all non-empty instances, and compares present longs using their natural order.
   */
  public static Ordering<OptionalLong> comparingLongEmptyLast() {
    return OptionalComparators.LONG_EMTPY_LAST;
  }

  /**
   * Return a {@link Ordering} for {@link OptionalDouble} that compares empty optionals as smaller
   * than all non-empty instances, and compares present doubles using their natural order.
   */
  public static Ordering<OptionalDouble> comparingDoubleEmptyFirst() {
    return OptionalComparators.DOUBLE_EMTPY_FIRST;
  }

  /**
   * Return a {@link Ordering} for {@link OptionalDouble} that compares empty optionals as larger
   * than all non-empty instances, and compares present doubles using their natural order.
   */
  public static Ordering<OptionalDouble> comparingDoubleEmptyLast() {
    return OptionalComparators.DOUBLE_EMTPY_LAST;
  }
}
