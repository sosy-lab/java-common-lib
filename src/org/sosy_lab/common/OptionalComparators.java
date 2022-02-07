// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common;

import com.google.common.collect.Ordering;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * {@link Comparator} implementations for {@link Optional}, {@link OptionalInt}, {@link
 * OptionalLong}, and {@link OptionalDouble}.
 */
@SuppressFBWarnings("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
final class OptionalComparators {

  private OptionalComparators() {}

  static final Ordering<OptionalInt> INT_EMPTY_FIRST =
      new OptionalIntComparator(/*pEmptyFirst=*/ true);
  static final Ordering<OptionalInt> INT_EMPTY_LAST =
      new OptionalIntComparator(/*pEmptyFirst=*/ false);

  private static final class OptionalIntComparator extends Ordering<OptionalInt>
      implements Serializable {

    private static final long serialVersionUID = 4448617946052179218L;
    private final boolean emptyFirst;

    private OptionalIntComparator(boolean pEmptyFirst) {
      emptyFirst = pEmptyFirst;
    }

    @Override
    public int compare(@NonNull OptionalInt left, @NonNull OptionalInt right) {
      if (!left.isPresent()) {
        if (right.isPresent()) {
          // left no, right yes
          return emptyFirst ? -1 : 1;
        } else {
          return 0;
        }
      }
      if (!right.isPresent()) {
        // left yes, right no
        return emptyFirst ? 1 : -1;
      }
      return Integer.compare(left.orElseThrow(), right.orElseThrow());
    }

    @Override
    public String toString() {
      return "Optionals.comparingIntEmpty" + (emptyFirst ? "First" : "Last") + "()";
    }

    private Object readResolve() {
      // Multiton avoids need for equals()/hashCode()
      return emptyFirst ? INT_EMPTY_FIRST : INT_EMPTY_LAST;
    }
  }

  static final Ordering<OptionalLong> LONG_EMPTY_FIRST =
      new OptionalLongComparator(/*pEmptyFirst=*/ true);
  static final Ordering<OptionalLong> LONG_EMPTY_LAST =
      new OptionalLongComparator(/*pEmptyFirst=*/ false);

  private static final class OptionalLongComparator extends Ordering<OptionalLong>
      implements Serializable {

    private static final long serialVersionUID = -8237349997441501776L;
    private final boolean emptyFirst;

    private OptionalLongComparator(boolean pEmptyFirst) {
      emptyFirst = pEmptyFirst;
    }

    @Override
    public int compare(@NonNull OptionalLong left, @NonNull OptionalLong right) {
      if (!left.isPresent()) {
        if (right.isPresent()) {
          // left no, right yes
          return emptyFirst ? -1 : 1;
        } else {
          return 0;
        }
      }
      if (!right.isPresent()) {
        // left yes, right no
        return emptyFirst ? 1 : -1;
      }
      return Long.compare(left.orElseThrow(), right.orElseThrow());
    }

    @Override
    public String toString() {
      return "Optionals.comparingLongEmpty" + (emptyFirst ? "First" : "Last") + "()";
    }

    private Object readResolve() {
      // Multiton avoids need for equals()/hashCode()
      return emptyFirst ? LONG_EMPTY_FIRST : LONG_EMPTY_LAST;
    }
  }

  static final Ordering<OptionalDouble> DOUBLE_EMPTY_FIRST =
      new OptionalDoubleComparator(/*pEmptyFirst=*/ true);
  static final Ordering<OptionalDouble> DOUBLE_EMPTY_LAST =
      new OptionalDoubleComparator(/*pEmptyFirst=*/ false);

  private static final class OptionalDoubleComparator extends Ordering<OptionalDouble>
      implements Serializable {

    private static final long serialVersionUID = -3210510142079410508L;
    private final boolean emptyFirst;

    private OptionalDoubleComparator(boolean pEmptyFirst) {
      emptyFirst = pEmptyFirst;
    }

    @Override
    public int compare(@NonNull OptionalDouble left, @NonNull OptionalDouble right) {
      if (!left.isPresent()) {
        if (right.isPresent()) {
          // left no, right yes
          return emptyFirst ? -1 : 1;
        } else {
          return 0;
        }
      }
      if (!right.isPresent()) {
        // left yes, right no
        return emptyFirst ? 1 : -1;
      }
      return Double.compare(left.orElseThrow(), right.orElseThrow());
    }

    @Override
    public String toString() {
      return "Optionals.comparingDoubleEmpty" + (emptyFirst ? "First" : "Last") + "()";
    }

    private Object readResolve() {
      // Multiton avoids need for equals()/hashCode()
      return emptyFirst ? DOUBLE_EMPTY_FIRST : DOUBLE_EMPTY_LAST;
    }
  }
}
