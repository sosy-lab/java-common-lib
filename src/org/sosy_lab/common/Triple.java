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
package org.sosy_lab.common;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Ordering.from;

import java.io.Serializable;
import java.util.Comparator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;


/**
 * A generic Triple class based on Pair.java
 *
 * @param <A>
 * @param <B>
 * @param <C>
 */
public class Triple<A, B, C> implements Serializable {

  private static final long serialVersionUID = 1272029955865151903L;

  @Nullable private final A first;
  @Nullable private final B second;
  @Nullable private final C third;

  private Triple(@Nullable A first, @Nullable B second, @Nullable C third) {
    this.first = first;
    this.second = second;
    this.third = third;
  }

  public static <A, B, C> Triple<A, B, C> of(@Nullable A first, @Nullable B second, @Nullable C third) {
    return new Triple<>(first, second, third);
  }

  @Nullable public final A getFirst() { return first; }
  @Nullable public final B getSecond() { return second; }
  @Nullable public final C getThird() { return third; }

  @Override
  public String toString() {
    return "(" + first + ", " + second + ", " + third + ")";
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (other instanceof Triple<?,?,?>)
      && equal(first,  ((Triple<?,?,?>)other).first)
      && equal(second, ((Triple<?,?,?>)other).second)
      && equal(third,  ((Triple<?,?,?>)other).third);
  }

  @Override
  public int hashCode() {
    if (first == null && second == null) {
      return (third == null) ? 0 : third.hashCode() + 1;
    } else if (first == null && third == null) {
      return second.hashCode() + 2;
    } else if (first == null) {
      return second.hashCode() * 7 + third.hashCode();
    } else if (second == null && third == null) {
      return first.hashCode() + 3;
    } else if (second == null) {
      return first.hashCode() * 11 + third.hashCode();
    } else if (third == null) {
      return first.hashCode() * 13 + second.hashCode();
    } else {
      return first.hashCode() * 17 + second.hashCode() * 5 + third.hashCode();
    }
  }


  public static <T> Function<Triple<? extends T, ?, ?>, T> getProjectionToFirst() {
    return Holder.<T>getInstance().PROJECTION_TO_FIRST;
  }

  public static <T> Function<Triple<?, ? extends T, ?>, T> getProjectionToSecond() {
    return Holder.<T>getInstance().PROJECTION_TO_SECOND;
  }

  public static <T> Function<Triple<?, ?, ? extends T>, T> getProjectionToThird() {
    return Holder.<T>getInstance().PROJECTION_TO_THIRD;
  }

  /*
   * Static holder class for several function objects because if these fields
   * were static fields of the Triple class, they couldn't be generic.
   */
  private static final class Holder<T> {

    private static final Holder<?> INSTANCE = new Holder<Void>();

    // Cast is safe because class has no mutable state
    @SuppressWarnings("unchecked")
    public static <T> Holder<T> getInstance() {
      return (Holder<T>) INSTANCE;
    }

    private final Function<Triple<? extends T, ?, ?>, T> PROJECTION_TO_FIRST = new Function<Triple<? extends T, ?, ?>, T>() {
      @Override
      public T apply(@Nonnull Triple<? extends T, ?, ?> pArg0) {
        return pArg0.getFirst();
      }
    };

    private final Function<Triple<?, ? extends T, ?>, T> PROJECTION_TO_SECOND = new Function<Triple<?, ? extends T, ?>, T>() {
      @Override
      public T apply(@Nonnull Triple<?, ? extends T, ?> pArg0) {
        return pArg0.getSecond();
      }
    };

    private final Function<Triple<?, ?, ? extends T>, T> PROJECTION_TO_THIRD = new Function<Triple<?, ?, ? extends T>, T>() {
      @Override
      public T apply(@Nonnull Triple<?, ?, ? extends T> pArg0) {
        return pArg0.getThird();
      }
    };
  }

  /**
   * Create a function which applies three other functions on the first, the
   * second, and the third element of a triple, respectively,
   * and returns a triple of the results.
   * @param f1 The function applied to the first element of the triple.
   * @param f2 The function applied to the second element of the triple.
   * @param f3 The function applied to the third element of the triple.
   * @return A component-wise composition of f1, f2, and f3.
   */
  public static <A1, B1, A2, B2, A3, B3> Function<Triple<A1, A2, A3>, Triple<B1, B2, B3>>
                componentWise(final Function<? super A1, ? extends B1> f1,
                              final Function<? super A2, ? extends B2> f2,
                              final Function<? super A3, ? extends B3> f3) {
    checkNotNull(f1);
    checkNotNull(f2);
    checkNotNull(f3);

    return new Function<Triple<A1, A2, A3>, Triple<B1, B2, B3>>() {
      @Override
      public Triple<B1, B2, B3> apply(@Nonnull Triple<A1, A2, A3> pInput) {
        return Triple.<B1, B2, B3>of(f1.apply(pInput.getFirst()),
                                      f2.apply(pInput.getSecond()),
                                      f3.apply(pInput.getThird()));
      }
    };
  }

  /**
   * Return a comparator for comparing triples lexicographically,
   * if their component types define a natural ordering.
   */
  public static <A extends Comparable<? super A>,
                  B extends Comparable<? super B>,
                  C extends Comparable<? super C>>
      Ordering<Triple<A, B, C>> lexicographicalNaturalComparator() {

    return lexicographicalComparator(Ordering.<A>natural(),
                                      Ordering.<B>natural(),
                                      Ordering.<C>natural());
  }

  /**
   * Return a comparator for comparing triples lexicographically,
   * delegating the comparison of the components to three comparators.
   */
  public static <A, B, C> Ordering<Triple<A, B, C>> lexicographicalComparator(
      Comparator<A> firstOrdering, Comparator<B> secondOrdering,
      Comparator<C> thirdOrdering) {

    return from(firstOrdering).onResultOf(Triple.<A>getProjectionToFirst())
        .<Triple<? extends A, ? extends B, ?>>compound(from(secondOrdering).onResultOf(Triple.<B>getProjectionToSecond()))
        .compound(from(thirdOrdering).onResultOf(Triple.<C>getProjectionToThird()));
  }
}
