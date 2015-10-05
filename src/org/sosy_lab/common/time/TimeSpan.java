/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.common.time;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import com.google.common.collect.EnumHashBiMap;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;

/**
 * This is an immutable representation of some time span,
 * using a {@link TimeUnit} and a value.
 *
 * The value may be positive or negative.
 *
 * Two instances are considered equal if they represent the exact same time span
 * regardless of their unit,
 * for example, 60s and 1min are considered equal.
 */
public final class TimeSpan implements Comparable<TimeSpan> {

  private static final EnumHashBiMap<TimeUnit, String> TIME_UNITS =
      EnumHashBiMap.create(TimeUnit.class);
  static {
    TIME_UNITS.put(NANOSECONDS,  "ns");
    TIME_UNITS.put(MICROSECONDS, "µs");
    TIME_UNITS.put(MILLISECONDS, "ms");
    TIME_UNITS.put(SECONDS,      "s");
    TIME_UNITS.put(MINUTES,      "min");
    TIME_UNITS.put(HOURS,        "h");
    TIME_UNITS.put(DAYS,         "d");
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

  public long get(TimeUnit dest) {
    return dest.convert(span, unit);
  }

  public TimeSpan to(TimeUnit dest) {
    if (dest.equals(unit)) {
      return this;
    }
    return new TimeSpan(get(dest), dest);
  }

  public long asSeconds() {
    return get(SECONDS);
  }

  public long asMillis() {
    return get(MILLISECONDS);
  }

  public long asNanos() {
    return get(NANOSECONDS);
  }

  public TimeUnit getUnit() {
    return unit;
  }

  public String formatAs(TimeUnit dest) {
    if (dest.compareTo(unit) <= 0) {
      // Example case: we have seconds, but we want milliseconds
      return to(dest).toString();
    }

    // Example case: we have nanoseconds, but we want seconds
    long scaleFactor = unit.convert(1L, dest);
    assert scaleFactor > 0;
    return String.format(Locale.US, "%9.3f%s", (double) span / scaleFactor, TIME_UNITS.get(dest));
  }

  /**
   * Check whether this time span is empty,
   * i.e., represents 0ns (or 0ms or 0s or ...).
   */
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
    if (this.unit == other.unit) {
      return this.span == other.span;
    }
    TimeUnit leastCommonUnit = leastCommonUnit(this, other);
    return this.get(leastCommonUnit) == other.get(leastCommonUnit);
  }

  @Override
  public int hashCode() {
    // Need to use a fixed unit here to be consistent with equals:
    // 60s and 1min need to have the same hashCode.
    return Longs.hashCode(unit.toNanos(span));
  }

  @Override
  public int compareTo(TimeSpan other) {
    if (this.unit == other.unit) {
      return Long.compare(this.span, other.span);
    }
    TimeUnit leastCommonUnit = leastCommonUnit(this, other);
    return Long.compare(this.get(leastCommonUnit),
                        other.get(leastCommonUnit));
  }

  private static TimeUnit leastCommonUnit(TimeSpan a, TimeSpan b) {
    return Ordering.natural().min(a.unit, b.unit);
  }

  @Override
  public String toString() {
    return span + TIME_UNITS.get(unit);
  }

  /**
   * Create a new time span that is the sum of two time spans.
   * The unit of the returned time span is the more precise one.
   */
  public static TimeSpan sum(TimeSpan a, TimeSpan b) {
    TimeUnit leastCommonUnit = leastCommonUnit(a, b);
    return new TimeSpan(a.get(leastCommonUnit) + b.get(leastCommonUnit),
                        leastCommonUnit);
  }

  /**
   * Create a new time span that is the sum of several time spans.
   * The unit of the returned time span is the most precise one.
   */
  public static TimeSpan sum(TimeSpan... t) {
    checkArgument(t.length > 0);

    TimeSpan result = t[0];
    for (int i = 1; i < t.length; i++) {
      result = sum(result, t[i]);
    }
    return result;
  }
}
