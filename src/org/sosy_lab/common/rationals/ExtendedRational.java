// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2014-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.rationals;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.errorprone.annotations.Immutable;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class represents "extended rational": rationals which allow for infinities, negative
 * infinities and undefined numbers.
 *
 * <p>Any operation on the numbers is guaranteed to never yield an exception.
 *
 * <p>Represented as wrapper around {@link Rational} class.
 */
@Immutable
public final class ExtendedRational implements Comparable<ExtendedRational> {
  @SuppressWarnings({"hiding", "MemberName"})
  public enum NumberType {
    NEG_INFTY,
    RATIONAL, // Normal rational.
    INFTY,
    NaN, // Infinity + negative infinity etc.
    // Like java's Double, UNDEFINED is bigger than everything (when sorting).
  }

  private final NumberType numberType;
  private final @Nullable Rational rational;

  public static final ExtendedRational INFTY = new ExtendedRational(NumberType.INFTY);
  public static final ExtendedRational NEG_INFTY = new ExtendedRational(NumberType.NEG_INFTY);
  public static final ExtendedRational ZERO = new ExtendedRational(Rational.ZERO);

  @SuppressWarnings({"checkstyle:constantname", "MemberName"})
  public static final ExtendedRational NaN = new ExtendedRational(NumberType.NaN);

  public ExtendedRational(Rational pRational) {
    numberType = NumberType.RATIONAL;
    rational = checkNotNull(pRational);
  }

  public boolean isRational() {
    return rational != null;
  }

  /**
   * If the represented number is rational, return the wrapped object.
   *
   * @throws java.lang.UnsupportedOperationException in case the value is not rational
   */
  public Rational getRational() {
    if (rational != null) {
      return rational;
    }
    throw new UnsupportedOperationException("Represented number is not rational");
  }

  private ExtendedRational(NumberType pType) {
    checkState(pType != NumberType.RATIONAL);
    numberType = pType;
    rational = null;
  }

  /**
   * Returns rational converted to double.
   *
   * <p>The method works, because the Java Double class also supports Infinity/-Infinity/NaN.
   */
  public double toDouble() {
    switch (numberType) {
      case NEG_INFTY:
        return Double.NEGATIVE_INFINITY;
      case RATIONAL:
        assert rational != null;
        return rational.doubleValue();
      case INFTY:
        return Double.POSITIVE_INFINITY;
      case NaN:
        return Double.NaN;
    }
    throw new UnsupportedOperationException("Unexpected number type");
  }

  /**
   * Returns one of TWO things. a) String of the form num/den if the number is rational. b) String
   * representation of infinity/etc, consistent with the {@code Double} class.
   */
  @Override
  public String toString() {
    switch (numberType) {
      case RATIONAL:
        assert rational != null;
        return rational.toString();
      default:
        return Double.toString(toDouble());
    }
  }

  /**
   * Reverses the effect of {@link ExtendedRational#toString}. Supports 4 different formats, to be
   * consistent with the {@link Double} class:
   *
   * <ul>
   *   <li>{@code "Infinity"}
   *   <li>{@code "-Infinity"}
   *   <li>{@code "NaN"}
   *   <li>{@code a/b} for some integers {@code a} and {@code b}
   *   <li>{@code a} for some integer {@code a}
   * </ul>
   *
   * @param s Input string,
   * @throws NumberFormatException {@code s} is not a valid representation of ExtendedRational.
   * @return New {@link ExtendedRational}.
   */
  public static ExtendedRational ofString(String s) {
    switch (s) {
      case "Infinity":
        return ExtendedRational.INFTY;
      case "-Infinity":
        return ExtendedRational.NEG_INFTY;
      case "NaN":
        return ExtendedRational.NaN;
      default:
        return new ExtendedRational(Rational.ofString(s));
    }
  }

  @Override
  public int compareTo(ExtendedRational b) {
    NumberType us = numberType;
    NumberType them = b.numberType;
    if (us == them) {
      if (us == NumberType.RATIONAL) {
        assert rational != null;
        assert b.rational != null;
        return rational.compareTo(b.rational);
      } else {
        return 0;
      }
    } else {

      // Take the ordering provided by the enum.
      return us.compareTo(them);
    }
  }

  @Override
  public boolean equals(@Nullable Object y) {
    if (this == y) {
      return true;
    }
    if (y == null) {
      return false;
    }
    if (y.getClass() != this.getClass()) {
      return false;
    }
    ExtendedRational b = (ExtendedRational) y;
    return compareTo(b) == 0;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @SuppressWarnings("ReferenceEquality") // multiton pattern
  public ExtendedRational times(ExtendedRational b) {
    if (this == NaN || b == NaN) {
      return NaN;
    }
    if (this.equals(ExtendedRational.ZERO) || b.equals(ExtendedRational.ZERO)) {
      return ExtendedRational.ZERO;
    } else if (this == NEG_INFTY && b == NEG_INFTY) {
      return INFTY;
    } else if (this == NEG_INFTY || b == NEG_INFTY) {
      return NEG_INFTY;
    } else if (this == INFTY || b == INFTY) {
      return INFTY;
    } else {
      assert rational != null && b.rational != null;
      return new ExtendedRational(rational.times(b.rational));
    }
  }

  @SuppressWarnings("ReferenceEquality") // multiton pattern
  public ExtendedRational plus(ExtendedRational b) {
    if (this == NaN || b == NaN) {
      return NaN;
    } else if ((this == NEG_INFTY && b == INFTY) || (this == INFTY && b == NEG_INFTY)) {
      return NaN;
    } else if (this == INFTY || b == INFTY) {
      return INFTY;
    } else if (this == NEG_INFTY || b == NEG_INFTY) {
      return NEG_INFTY;
    }
    assert rational != null && b.rational != null;
    return new ExtendedRational(rational.plus(b.rational));
  }

  public ExtendedRational minus(ExtendedRational b) {
    ExtendedRational a = this;
    return a.plus(b.negate());
  }

  public ExtendedRational divides(ExtendedRational b) {
    ExtendedRational a = this;
    return a.times(b.reciprocal());
  }

  @SuppressWarnings("ReferenceEquality") // multiton pattern
  public ExtendedRational reciprocal() {
    if (this == NaN) {
      return NaN;
    } else if (this == INFTY || this == NEG_INFTY) {
      return new ExtendedRational(Rational.ZERO);
    }
    assert rational != null;
    return new ExtendedRational(rational.reciprocal());
  }

  @SuppressWarnings("ReferenceEquality") // multiton pattern
  public ExtendedRational negate() {
    if (this == NaN) {
      return NaN;
    } else if (this == INFTY) {
      return NEG_INFTY;
    } else if (this == NEG_INFTY) {
      return INFTY;
    }
    assert rational != null;
    return new ExtendedRational(rational.negate());
  }
}
