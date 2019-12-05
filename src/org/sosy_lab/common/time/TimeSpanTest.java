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

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.sosy_lab.common.time.TimeSpan.sum;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

@SuppressWarnings("CheckReturnValue")
public class TimeSpanTest {

  private static final long LARGE_VALUE = 1125899906842624L; // 2^50
  private static final TimeSpan LARGE_AS_HOURS = TimeSpan.of(LARGE_VALUE * 24, HOURS);
  private static final TimeSpan LARGE_AS_MINUTES = TimeSpan.of(LARGE_VALUE * 24 * 60, MINUTES);
  private static final TimeSpan LARGE = TimeSpan.of(LARGE_VALUE, DAYS); // 2^50 days

  private static final long VERY_LARGE_VALUE = 4611686018427387905L; // 2^62 + 1

  @Test
  public void testValueOfZero() {
    TimeSpan result = TimeSpan.valueOf("0");
    TimeSpan expected = TimeSpan.empty();
    assertThat(result).isEqualTo(expected);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValueOfNegative() {
    TimeSpan.valueOf("-10");
  }

  @Test
  public void testValueOfNoUnit() {
    TimeSpan result = TimeSpan.valueOf("214");
    TimeSpan expected = TimeSpan.ofSeconds(214);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testValueOfSeconds() {
    TimeSpan result = TimeSpan.valueOf("13s");
    TimeSpan expected = TimeSpan.ofSeconds(13);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testValueOfSecondsSpaceBeforeUnit() {
    TimeSpan result = TimeSpan.valueOf("13 s");
    TimeSpan expected = TimeSpan.ofSeconds(13);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testValueOfMinutes() {
    TimeSpan result = TimeSpan.valueOf("5min");
    TimeSpan expected = TimeSpan.of(5, MINUTES);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testValueOfHours() {
    TimeSpan result = TimeSpan.valueOf("7h");
    TimeSpan expected = TimeSpan.of(7, HOURS);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testValueOfDays() {
    TimeSpan result = TimeSpan.valueOf("4d");
    TimeSpan expected = TimeSpan.of(4, DAYS);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testValueOfAlternativeDay() {
    TimeSpan result = TimeSpan.valueOf("1day");
    TimeSpan expected = TimeSpan.of(1, DAYS);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testValueOfAlternativeDays() {
    TimeSpan result = TimeSpan.valueOf("1days");
    TimeSpan expected = TimeSpan.of(1, DAYS);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testValueOfSecondsMinutes() {
    TimeSpan result = TimeSpan.valueOf("15min13s");
    TimeSpan expected = sum(TimeSpan.of(15, MINUTES), TimeSpan.of(13, SECONDS));
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testValueOfSecondsHours() {
    TimeSpan result = TimeSpan.valueOf("3h13s");
    TimeSpan expected = sum(TimeSpan.of(3, HOURS), TimeSpan.of(13, SECONDS));
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testValueOfSecondsHoursWhitespace() {
    TimeSpan result = TimeSpan.valueOf("2h 22s");
    TimeSpan expected = sum(TimeSpan.of(2, HOURS), TimeSpan.of(22, SECONDS));
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testValueOfSecondsHoursSuperflousWhitespace() {
    TimeSpan result = TimeSpan.valueOf("2h    22s");
    TimeSpan expected = sum(TimeSpan.of(2, HOURS), TimeSpan.of(22, SECONDS));
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testValueOfSecondsTrailingWhitespace() {
    TimeSpan result = TimeSpan.valueOf("222  ");
    TimeSpan expected = TimeSpan.ofSeconds(222);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testValueOfSecondsLeadingWhitespace() {
    TimeSpan result = TimeSpan.valueOf("   222");
    TimeSpan expected = TimeSpan.ofSeconds(222);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testValueOfSecondsHoursAllWhitespaceSeparated() {
    TimeSpan result = TimeSpan.valueOf("2 h 22 s");
    TimeSpan expected = sum(TimeSpan.of(2, HOURS), TimeSpan.of(22, SECONDS));
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testValueOfSecondsMinutesHours() {
    TimeSpan result = TimeSpan.valueOf("2h13min22s");
    TimeSpan expected =
        sum(TimeSpan.of(2, HOURS), TimeSpan.of(13, MINUTES), TimeSpan.of(22, SECONDS));
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testValueOfAll() {
    TimeSpan result = TimeSpan.valueOf("4d2h13min22s");
    TimeSpan expected =
        sum(
            TimeSpan.of(4, DAYS),
            TimeSpan.of(2, HOURS),
            TimeSpan.of(13, MINUTES),
            TimeSpan.of(22, SECONDS));
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testValueOfOverflow() {
    TimeSpan result = TimeSpan.valueOf("55h77s");
    TimeSpan expected = sum(TimeSpan.of(55, HOURS), TimeSpan.of(77, SECONDS));
    assertThat(result).isEqualTo(expected);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDoubleDeclaration() {
    TimeSpan.valueOf("77s314s");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNonsense() {
    TimeSpan.valueOf("1asdflkajsd1`;32asd fva");
  }

  @Test
  public void testGetCheckedNoOverflow() {
    assertThat(LARGE.getChecked(HOURS)).isEqualTo(LARGE_VALUE * 24);
    assertThat(LARGE.getChecked(MINUTES)).isEqualTo(LARGE_VALUE * 24 * 60);
  }

  @Test(expected = ArithmeticException.class)
  public void testGetCheckedOverflow() {
    LARGE.getChecked(SECONDS);
  }

  @Test
  public void testGetSaturatedNoOverflow() {
    assertThat(LARGE.getSaturated(HOURS)).isEqualTo(LARGE_VALUE * 24);
    assertThat(LARGE.getSaturated(MINUTES)).isEqualTo(LARGE_VALUE * 24 * 60);
  }

  @Test
  public void testGetSaturatedOverflow() {
    assertThat(LARGE.getSaturated(SECONDS)).isEqualTo(Long.MAX_VALUE);
    assertThat(LARGE.getSaturated(MILLISECONDS)).isEqualTo(Long.MAX_VALUE);
    assertThat(LARGE.getSaturated(MICROSECONDS)).isEqualTo(Long.MAX_VALUE);
    assertThat(LARGE.getSaturated(NANOSECONDS)).isEqualTo(Long.MAX_VALUE);
  }

  @Test
  public void testToCheckedNoOverflow() {
    assertThat(LARGE.toChecked(HOURS)).isEqualTo(LARGE_AS_HOURS);
    assertThat(LARGE.toChecked(MINUTES)).isEqualTo(LARGE_AS_MINUTES);
  }

  @Test(expected = ArithmeticException.class)
  public void testToGetCheckedOverflow() {
    LARGE.toChecked(SECONDS);
  }

  @Test
  public void testToIfPossibleNoOverflow() {
    TimeSpan resultAsHours = LARGE.toIfPossible(HOURS);
    assertThat(resultAsHours.getUnit()).isEqualTo(HOURS);
    assertThat(resultAsHours).isEqualTo(LARGE_AS_HOURS);

    TimeSpan resultAsMinutes = LARGE.toIfPossible(MINUTES);
    assertThat(resultAsMinutes.getUnit()).isEqualTo(MINUTES);
    assertThat(resultAsMinutes).isEqualTo(LARGE_AS_MINUTES);
  }

  @Test
  public void testToIfPossibleOverflow() {
    TimeSpan resultForSeconds = LARGE.toIfPossible(SECONDS);
    assertThat(resultForSeconds.getUnit()).isEqualTo(MINUTES);
    assertThat(resultForSeconds).isEqualTo(LARGE_AS_MINUTES);

    TimeSpan resultForMillis = LARGE.toIfPossible(MILLISECONDS);
    assertThat(resultForMillis.getUnit()).isEqualTo(MINUTES);
    assertThat(resultForMillis).isEqualTo(LARGE_AS_MINUTES);

    TimeSpan resultForMicros = LARGE.toIfPossible(MICROSECONDS);
    assertThat(resultForMicros.getUnit()).isEqualTo(MINUTES);
    assertThat(resultForMicros).isEqualTo(LARGE_AS_MINUTES);

    TimeSpan resultForNanos = LARGE.toIfPossible(NANOSECONDS);
    assertThat(resultForNanos.getUnit()).isEqualTo(MINUTES);
    assertThat(resultForNanos).isEqualTo(LARGE_AS_MINUTES);
  }

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(TimeSpan.empty(), TimeSpan.of(0, SECONDS), TimeSpan.of(0, NANOSECONDS))
        .addEqualityGroup(LARGE, LARGE_AS_HOURS, LARGE_AS_MINUTES)
        .testEquals();
  }

  @Test
  public void testCompareTo() {
    assertThat(TimeSpan.empty()).isEquivalentAccordingToCompareTo(TimeSpan.of(0, SECONDS));
    assertThat(TimeSpan.empty()).isEquivalentAccordingToCompareTo(TimeSpan.of(0, NANOSECONDS));
    assertThat(LARGE).isEquivalentAccordingToCompareTo(LARGE_AS_HOURS);
    assertThat(LARGE).isEquivalentAccordingToCompareTo(LARGE_AS_MINUTES);

    assertThat(LARGE).isGreaterThan(TimeSpan.empty());
    assertThat(LARGE).isGreaterThan(TimeSpan.of(0, HOURS));
    assertThat(LARGE).isGreaterThan(TimeSpan.of(0, SECONDS));
    assertThat(LARGE).isGreaterThan(TimeSpan.of(0, NANOSECONDS));
  }

  @Test
  public void testSumNoOverflow() {
    TimeSpan input = TimeSpan.of(VERY_LARGE_VALUE, HOURS);
    TimeSpan result = sum(input, input);
    assertThat(result.getUnit()).isEqualTo(DAYS);
    assertThat(result).isEqualTo(TimeSpan.of(2 * (VERY_LARGE_VALUE / 24), DAYS));
  }

  @Test(expected = ArithmeticException.class)
  public void testSumOverflow() {
    TimeSpan input = TimeSpan.of(VERY_LARGE_VALUE, DAYS);
    sum(input, input);
  }

  @Test
  public void testDifferenceNoOverflow() {
    TimeSpan result =
        TimeSpan.difference(
            TimeSpan.of(-VERY_LARGE_VALUE, HOURS), TimeSpan.of(VERY_LARGE_VALUE, HOURS));
    assertThat(result.getUnit()).isEqualTo(DAYS);
    assertThat(result).isEqualTo(TimeSpan.of(2 * (-VERY_LARGE_VALUE / 24), DAYS));
  }

  @Test(expected = ArithmeticException.class)
  public void testDifferenceOverflow() {
    TimeSpan.difference(TimeSpan.of(-VERY_LARGE_VALUE, DAYS), TimeSpan.of(VERY_LARGE_VALUE, DAYS));
  }

  @Test
  public void testMultiplyNoOverflow() {
    TimeSpan result = LARGE_AS_HOURS.multiply(1000);
    assertThat(result.getUnit()).isEqualTo(DAYS);
    assertThat(result).isEqualTo(LARGE.multiply(1000));
  }

  @Test(expected = ArithmeticException.class)
  @SuppressWarnings("CheckReturnValue")
  public void testMultiplyOverflow() {
    LARGE_AS_HOURS.multiply(1000 * 1000);
  }

  @Test
  public void testToStringMinute() {
    TimeSpan time = TimeSpan.of(1, MINUTES);
    assertThat(time.formatHumanReadableLarge()).isEqualTo("01min");
  }

  @Test
  public void testToStringSeconds() {
    TimeSpan time = TimeSpan.ofSeconds(45);
    assertThat(time.formatHumanReadableLarge()).isEqualTo("45s");
  }

  @Test
  public void testToStringHours() {
    TimeSpan time = TimeSpan.of(2, HOURS);
    assertThat(time.formatHumanReadableLarge()).isEqualTo("02h");
  }

  @Test
  public void testToStringHoursMinutes() {
    TimeSpan time = TimeSpan.of(2 * 60 + 16, MINUTES);
    assertThat(time.formatHumanReadableLarge()).isEqualTo("02h 16min");
  }

  @Test
  public void testToStringDaysHours() {
    TimeSpan time = TimeSpan.of(2 * 24, HOURS);
    assertThat(time.formatHumanReadableLarge()).isEqualTo("2d 00h");
  }

  @Test
  public void testToStringDaysMinutes() {
    TimeSpan time = TimeSpan.of(2 * 24 * 60 + 45, MINUTES);
    assertThat(time.formatHumanReadableLarge()).isEqualTo("2d 00h 45min");
  }
}
