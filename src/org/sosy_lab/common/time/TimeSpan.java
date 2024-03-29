// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.time;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ascii;
import com.google.common.collect.EnumHashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.Var;
import java.io.Serializable;
import java.time.Duration;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This is an immutable representation of some time span, using a {@link TimeUnit} and a value.
 *
 * <p>The value may be positive or negative. All operations check for overflows and underflows, the
 * behavior on overflow and underflow differs and is documented for each method.
 *
 * <p>Two instances are considered equal if they represent the exact same time span regardless of
 * their unit, for example, 60s and 1min are considered equal.
 */
@Immutable
public final class TimeSpan implements Comparable<TimeSpan>, Serializable, TemporalAmount {

  private static final long serialVersionUID = -4013592312989551009L;

  private static final ImmutableSortedSet<TimeUnit> ALL_UNITS =
      ImmutableSortedSet.copyOf(EnumSet.allOf(TimeUnit.class));

  static {
    assert ALL_UNITS.higher(SECONDS).equals(MINUTES); // assert expected order of set
  }

  private static final EnumHashBiMap<TimeUnit, String> TIME_UNITS =
      EnumHashBiMap.create(TimeUnit.class);

  static {
    TIME_UNITS.put(NANOSECONDS, "ns");
    TIME_UNITS.put(MICROSECONDS, "µs");
    TIME_UNITS.put(MILLISECONDS, "ms");
    TIME_UNITS.put(SECONDS, "s");
    TIME_UNITS.put(MINUTES, "min");
    TIME_UNITS.put(HOURS, "h");
    TIME_UNITS.put(DAYS, "d");
  }

  private static final Pattern ONLY_DIGITS = Pattern.compile(" *([0-9]+) *");

  private enum CharType {
    BEGIN,
    END,
    LETTER,
    DIGIT,
    WHITESPACE
  }

  private final long span;
  private final TimeUnit unit;

  private TimeSpan(long pSpan, TimeUnit pUnit) {
    span = pSpan;
    unit = checkNotNull(pUnit);
  }

  public static TimeSpan of(long pSpan, TimeUnit pUnit) {
    return new TimeSpan(pSpan, pUnit);
  }

  public static TimeSpan ofSeconds(long pSeconds) {
    return new TimeSpan(pSeconds, SECONDS);
  }

  public static TimeSpan ofMillis(long pMillis) {
    return new TimeSpan(pMillis, MILLISECONDS);
  }

  public static TimeSpan ofNanos(long pNanos) {
    return new TimeSpan(pNanos, NANOSECONDS);
  }

  public static TimeSpan empty() {
    return new TimeSpan(0, DAYS);
  }

  /**
   * Converts the given {@link String} into a {@link TimeSpan} object. Supported units are day,
   * hour, minute and second.
   *
   * @param input the {@link String} to convert
   * @return a {@link TimeSpan} represented by the given {@link String}
   * @throws IllegalArgumentException if the input is not a valid string representation of a {@link
   *     TimeSpan}.
   */
  public static TimeSpan valueOf(String input) {

    // only seconds: use simple regex
    Matcher secondMatcher = ONLY_DIGITS.matcher(input);
    if (secondMatcher.matches()) {
      return ofSeconds(Long.parseLong(secondMatcher.group(1)));
    }

    // values with units: more elaborate parsing necessary
    List<String> tokens = splitIntoTokens(input);

    @Var long days = 0;
    @Var long hours = 0;
    @Var long minutes = 0;
    @Var long seconds = 0;

    Iterator<String> it = tokens.iterator();

    while (it.hasNext()) {
      // first: value
      String nextString = it.next();
      long value = Long.parseLong(nextString);

      // second: unit
      if (!it.hasNext()) {
        throw new IllegalArgumentException("Value " + nextString + " has no unit.");
      }

      String unit = it.next();
      switch (unit) {
        case "day":
        case "days":
        case "d":
          if (days != 0) {
            throw new IllegalArgumentException("Days set twice: " + unit);
          }
          days = value;
          break;

        case "h":
        case "hour":
        case "hours":
          if (hours != 0) {
            throw new IllegalArgumentException("Hours set twice: " + unit);
          }
          hours = value;
          break;

        case "min":
        case "m":
          if (minutes != 0) {
            throw new IllegalArgumentException("Minutes set twice: " + unit);
          }
          minutes = value;
          break;

        case "s":
          if (seconds != 0) {
            throw new IllegalArgumentException("Seconds set twice: " + unit);
          }
          seconds = value;
          break;

        default:
          throw new IllegalArgumentException("Unknown unit: " + unit);
      }
    }

    return sum(of(seconds, SECONDS), of(minutes, MINUTES), of(hours, HOURS), of(days, DAYS));
  }

  private static List<String> splitIntoTokens(String input) {
    List<String> tokens = new ArrayList<>();
    @Var CharType previous = CharType.BEGIN;
    @Var int pos = 0;

    for (int i = 0; i <= input.length(); ++i) {

      CharType current;
      if (i == input.length()) {
        current = CharType.END;
      } else {
        char currentChar = input.charAt(i);
        if (Character.isLetter(currentChar)) {
          current = CharType.LETTER;
        } else if (Character.isDigit(currentChar)) {
          current = CharType.DIGIT;
        } else if (Character.isWhitespace(currentChar)) {
          current = CharType.WHITESPACE;
        } else {
          throw new IllegalArgumentException(
              "Unreconized character '" + currentChar + "' when parsing " + input);
        }
      }

      if (current != previous) {
        // we want to use the previous token
        if (previous == CharType.LETTER || previous == CharType.DIGIT) {
          tokens.add(input.substring(pos, i));
        }

        if (current == CharType.LETTER || current == CharType.DIGIT) {
          pos = i;
        }

        previous = current;
      }
    }

    return tokens;
  }

  /**
   * Get the value of this TimeSpan represented in the given unit. If the given unit is larger than
   * the current unit, precision may be lost.
   *
   * @throws ArithmeticException If the value cannot be represented in the given unit due to
   *     overflow.
   */
  public long getChecked(TimeUnit dest) {
    if (dest.compareTo(unit) < 0) {
      // Example case: we have seconds, but we want milliseconds (can overflow)
      long factor = dest.convert(1, unit);
      assert factor > 1;
      return Math.multiplyExact(span, factor);
    }

    // Example case: we have nanoseconds, but we want seconds (cannot overflow)
    return dest.convert(span, unit);
  }

  /**
   * Get the value of this TimeSpan represented in the given unit. If the given unit is larger than
   * the current unit, precision may be lost. If the value cannot be represented in the given unit
   * due to overflow, Long.MAX_VALUE/Long.MIN_VALUE is returned.
   */
  public long getSaturated(TimeUnit dest) {
    return dest.convert(span, unit);
  }

  /**
   * Return a TimeSpan that represents (approximately) the same time span, but whose unit is the
   * given unit. If the given unit is larger than the current unit, precision may be lost.
   *
   * @throws ArithmeticException If the value cannot be represented in the given unit
   */
  public TimeSpan toChecked(TimeUnit dest) {
    if (dest.equals(unit)) {
      return this;
    }
    return new TimeSpan(getChecked(dest), dest);
  }

  /**
   * Return a TimeSpan that represents (approximately) the same time span, but whose unit is the
   * given unit. If the given unit is larger than the current unit, precision may be lost. If the
   * value cannot be represented in the given unit due to overflow, Long.MAX_VALUE/Long.MIN_VALUE is
   * returned.
   */
  public TimeSpan toSaturated(TimeUnit dest) {
    if (dest.equals(unit)) {
      return this;
    }
    return new TimeSpan(getSaturated(dest), dest);
  }

  /**
   * Return a TimeSpan that represents (approximately) the same time span, but whose unit is the
   * given unit, if possible. If the given unit is larger than the current unit, precision may be
   * lost. If the value cannot be represented in the given unit due to overflow, the resulting
   * TimeSpan does not use the given unit, but the closest unit one that still allows to hold the
   * exact value.
   */
  @VisibleForTesting
  TimeSpan toIfPossible(@Var TimeUnit dest) {
    if (dest.equals(unit)) {
      return this;
    }
    if (dest.compareTo(unit) < 0) {
      // Example case: we have seconds, but we want milliseconds (can overflow).
      // Overflow is expected to be very rare.
      // Loop will terminate because at one time "dest" becomes equal to "this.unit"
      // and then toChecked succeeds for sure.
      while (true) {
        try {
          return toChecked(dest);
        } catch (ArithmeticException e) {
          dest = checkNotNull(ALL_UNITS.higher(dest));
        }
      }

    } else {
      // Example case: we have nanoseconds, but we want seconds (cannot overflow).
      return new TimeSpan(getSaturated(dest), dest);
    }
  }

  /**
   * Get the value of this TimeSpan as seconds. If the current unit is smaller than seconds,
   * precision may be lost.
   *
   * @throws ArithmeticException If the value cannot be represented as seconds due to overflow.
   */
  public long asSeconds() {
    return getChecked(SECONDS);
  }

  /**
   * Get the value of this TimeSpan as milliseconds. If the current unit is smaller than
   * milliseconds, precision may be lost.
   *
   * @throws ArithmeticException If the value cannot be represented as milliseconds due to overflow.
   */
  public long asMillis() {
    return getChecked(MILLISECONDS);
  }

  /**
   * Get the value of this TimeSpan as nanoseconds.
   *
   * @throws ArithmeticException If the value cannot be represented as nanoseconds due to overflow.
   */
  public long asNanos() {
    return getChecked(NANOSECONDS);
  }

  public TimeUnit getUnit() {
    return unit;
  }

  /**
   * Return a {@link Duration} instance that represents the same amount of time (but it won't be
   * possible to retrieve our unit as from {@link #getUnit()}). Note that not all possible {@link
   * TimeSpan} values can be represented by a {@link Duration}.
   *
   * @throws ArithmeticException If an overflow occurs.
   */
  public Duration asDuration() {
    return Duration.of(span, unit.toChronoUnit());
  }

  /**
   * Return a strings that represents (approximately) this time span, in the given unit if possible.
   * If the given unit is larger than the current unit, precision may be lost. If the value cannot
   * be represented in the given unit due to overflow, the result does not use the given unit, but
   * the closest unit one that still allows to hold the exact value.
   */
  public String formatAs(TimeUnit dest) {
    if (dest.compareTo(unit) <= 0) {
      // Example case: we have seconds, but we want milliseconds
      return toIfPossible(dest).toString();
    }

    // Example case: we have nanoseconds, but we want seconds
    long scaleFactor = unit.convert(1L, dest);
    assert scaleFactor > 0;
    return String.format(Locale.US, "%9.3f%s", (double) span / scaleFactor, TIME_UNITS.get(dest));
  }

  /** Check whether this time span is empty, i.e., represents 0ns (or 0ms or 0s or ...). */
  public boolean isEmpty() {
    return span == 0;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof TimeSpan)) {
      return false;
    }
    TimeSpan other = (TimeSpan) obj;
    if (unit == other.unit) {
      return span == other.span;
    }
    TimeUnit leastCommonUnit = leastCommonUnit(this, other);
    try {
      return getChecked(leastCommonUnit) == other.getChecked(leastCommonUnit);
    } catch (ArithmeticException e) {
      // In case of overflow, both values cannot be the same.
      return false;
    }
  }

  @Override
  public int hashCode() {
    // Need to use a fixed unit here to be consistent with equals:
    // 60s and 1min need to have the same hashCode.
    // Saturation is ok, all really large values just have the same hash code.
    return Longs.hashCode(getSaturated(NANOSECONDS));
  }

  @Override
  public int compareTo(TimeSpan other) {
    if (unit == other.unit) {
      return Long.compare(span, other.span);
    }
    TimeUnit leastCommonUnit = leastCommonUnit(this, other);
    try {
      return Long.compare(getChecked(leastCommonUnit), other.getChecked(leastCommonUnit));
    } catch (ArithmeticException e) {
      // Only one of the two calls can overflow,
      // and it has to be the one with the larger unit.
      // Thus in case of overflow the TimeSpan with the larger unit also has the larger value.
      return unit.compareTo(other.unit);
    }
  }

  private static TimeUnit leastCommonUnit(TimeSpan a, TimeSpan b) {
    return Ordering.natural().min(a.unit, b.unit);
  }

  @Override
  public String toString() {
    return DEFAULT_FORMAT.apply(this);
  }

  /**
   * Create a new time span that is the sum of two time spans. The unit of the returned time span is
   * the more precise one if possible, otherwise the closest unit that still allows to hold both
   * input values and the result. Note that this can loose precision when adding a very large and a
   * very small value.
   *
   * @throws ArithmeticException If no unit is large enough to represent the result value.
   */
  public static TimeSpan sum(TimeSpan a, TimeSpan b) {
    @Var TimeUnit leastCommonUnit = leastCommonUnit(a, b);
    while (true) {
      try {
        return new TimeSpan(
            Math.addExact(a.getChecked(leastCommonUnit), b.getChecked(leastCommonUnit)),
            leastCommonUnit);
      } catch (ArithmeticException e) {
        // Overflow is expected to be very rare, thus handle exception case instead of checking.
        // Try again with next unit.
        leastCommonUnit = ALL_UNITS.higher(leastCommonUnit);
        if (leastCommonUnit == null) {
          // overflow from addition
          throw e;
        }
      }
    }
  }

  /**
   * Create a new time span that is the sum of several time spans. The unit of the returned time
   * span is the most precise one if possible, otherwise the closest unit that still allows to hold
   * input values and the result. Note that this can loose precision when adding very large and very
   * small values.
   *
   * @throws ArithmeticException If no unit is large enough to represent the result value.
   */
  public static TimeSpan sum(Iterable<TimeSpan> timeSpans) {
    Iterator<TimeSpan> it = timeSpans.iterator();
    checkArgument(it.hasNext());

    @Var TimeSpan result = it.next();
    // TODO Summing in loop looses more precision than necessary.
    while (it.hasNext()) {
      result = sum(result, it.next());
    }
    return result;
  }

  /**
   * Create a new time span that is the sum of several time spans. The unit of the returned time
   * span is the most precise one.
   */
  public static TimeSpan sum(TimeSpan... t) {
    return sum(Arrays.asList(t));
  }

  /**
   * Create a new time span that is the difference of two time spans. The unit of the returned time
   * span is the more precise one if possible, otherwise the closest unit that still allows to hold
   * both input values and the result. Note that this can loose precision when subtracting a very
   * large and a very small value.
   */
  public static TimeSpan difference(TimeSpan a, TimeSpan b) {
    @Var TimeUnit leastCommonUnit = leastCommonUnit(a, b);
    while (true) {
      try {
        return new TimeSpan(
            Math.subtractExact(a.getChecked(leastCommonUnit), b.getChecked(leastCommonUnit)),
            leastCommonUnit);
      } catch (ArithmeticException e) {
        // Overflow is expected to be very rare, thus handle exception case instead of checking.
        // Try again with next unit.
        leastCommonUnit = ALL_UNITS.higher(leastCommonUnit);
        if (leastCommonUnit == null) {
          // overflow from subtraction
          throw e;
        }
      }
    }
  }

  /**
   * Create a new time span that is the current one multiplied by a non-negative integral factor.
   * The unit of the returned time span is the same as the current one if possible, otherwise the
   * closest unit that still allows to the result. Note that this can loose precision.
   */
  @CheckReturnValue
  public TimeSpan multiply(int factor) {
    checkArgument(factor >= 0, "Cannot multiply TimeSpan with negative value %s", factor);
    @Var TimeUnit dest = unit;
    while (true) {
      try {
        return new TimeSpan(Math.multiplyExact(getChecked(dest), factor), dest);
      } catch (ArithmeticException e) {
        // Overflow is expected to be very rare, thus handle exception case instead of checking.
        // Try again with next unit.
        dest = ALL_UNITS.higher(dest);
        if (dest == null) {
          // overflow from multiplication
          throw e;
        }
      }
    }
  }

  /**
   * Create a new time span that is the current one divided by a non-negative integral value. The
   * result of the division is rounded down (integer division). The unit of the returned time span
   * is the same as the current one.
   */
  @CheckReturnValue
  public TimeSpan divide(int divisor) {
    checkArgument(divisor >= 0, "Cannot divide TimeSpan by negative value %s", divisor);
    return new TimeSpan(span / divisor, unit);
  }

  // Methods for implementing TemporalAmount

  /** Only for implementing {@link TemporalAmount}. Use {@link #getUnit()} instead. */
  @Deprecated
  @Override
  public ImmutableList<TemporalUnit> getUnits() {
    return ImmutableList.of(unit.toChronoUnit());
  }

  /**
   * Only for implementing {@link TemporalAmount}. It is not possible to pass arbitrary units here.
   * Use {@link #getChecked(TimeUnit)} instead.
   */
  @Deprecated
  @Override
  public long get(TemporalUnit pUnit) {
    checkNotNull(pUnit);
    if (pUnit != unit.toChronoUnit()) {
      throw new UnsupportedTemporalTypeException("Unsupported unit: " + pUnit);
    }
    return span;
  }

  @Override
  public Temporal addTo(Temporal pTemporal) {
    return pTemporal.plus(span, unit.toChronoUnit());
  }

  @Override
  public Temporal subtractFrom(Temporal pTemporal) {
    return pTemporal.minus(span, unit.toChronoUnit());
  }

  // Code for formatting as string

  private String formatSimple() {
    return span + TIME_UNITS.get(unit);
  }

  @VisibleForTesting
  String formatHumanReadableLarge() {
    StringBuilder result = new StringBuilder();
    @Var boolean started = false;

    long years = getChecked(DAYS) / 365;
    if (years > 0) {
      started = true;
      result.append(years).append("a ");
    }

    long days = getChecked(DAYS) - years * 365;
    if (started || days > 0) {
      started = true;
      result.append(days).append("d ");
    }
    if (unit.equals(DAYS)) {
      return result.toString().trim();
    }

    long hours = getChecked(HOURS) - years * 365 * 24 - days * 24;
    if (started || hours > 0) {
      started = true;
      result.append(String.format("%02dh ", hours));
    }
    if (unit.equals(HOURS)) {
      return result.toString().trim();
    }

    long minutes = getChecked(MINUTES) - years * 365 * 24 * 60 - days * 24 * 60 - hours * 60;
    if (started || minutes > 0) {
      result.append(String.format("%02dmin ", minutes));
    }
    if (unit.equals(MINUTES)) {
      started = true;
      return result.toString().trim();
    }

    long seconds =
        getChecked(SECONDS)
            - years * 365 * 24 * 60 * 60
            - days * 24 * 60 * 60
            - hours * 60 * 60
            - minutes * 60;
    result.append(String.format("%02ds", seconds));

    return result.toString();
  }

  private static final String DEFAULT_FORMAT_PROPERTY_NAME =
      TimeSpan.class.getCanonicalName() + ".defaultFormat";

  private static final Function<TimeSpan, String> DEFAULT_FORMAT;

  static {
    String format =
        Ascii.toUpperCase(System.getProperty(DEFAULT_FORMAT_PROPERTY_NAME, "SIMPLE").trim());
    switch (format) {
      case "HUMAN_READABLE_LARGE":
        DEFAULT_FORMAT = TimeSpan::formatHumanReadableLarge;
        break;
      case "SIMPLE":
        DEFAULT_FORMAT = TimeSpan::formatSimple;
        break;
      default:
        DEFAULT_FORMAT = TimeSpan::formatSimple;
    }
  }
}
