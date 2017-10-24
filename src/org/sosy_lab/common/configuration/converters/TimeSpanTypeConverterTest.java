/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.common.configuration.converters;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.auto.value.AutoAnnotation;
import com.google.common.reflect.TypeToken;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;

public class TimeSpanTypeConverterTest {

  @AutoAnnotation
  private static TimeSpanOption createAnnotation(TimeUnit codeUnit, TimeUnit defaultUserUnit) {
    return new AutoAnnotation_TimeSpanTypeConverterTest_createAnnotation(codeUnit, defaultUserUnit);
  }

  private static int convertToInt(String value, TimeUnit defaultUserUnit)
      throws InvalidConfigurationException {
    return (int)
        new TimeSpanTypeConverter()
            .convert(
                "dummy",
                value,
                TypeToken.of(Integer.class),
                createAnnotation(SECONDS, defaultUserUnit),
                Paths.get("dummy.properties"),
                LogManager.createTestLogManager());
  }

  private static TimeSpan convertToTimeSpan(String value, TimeUnit defaultUserUnit)
      throws InvalidConfigurationException {
    return (TimeSpan)
        new TimeSpanTypeConverter()
            .convert(
                "dummy",
                value,
                TypeToken.of(TimeSpan.class),
                createAnnotation(SECONDS, defaultUserUnit),
                Paths.get("dummy.properties"),
                LogManager.createTestLogManager());
  }

  @Test
  public void test_defaultUnit() throws InvalidConfigurationException {
    assertThat(convertToInt("10", SECONDS)).isEqualTo(10);
  }

  @Test
  public void test_differentDefaultUnit() throws InvalidConfigurationException {
    assertThat(convertToInt("10", MINUTES)).isEqualTo(10 * 60);
  }

  @Test
  public void test_defaultUnit_negative() throws InvalidConfigurationException {
    assertThat(convertToInt("-10", SECONDS)).isEqualTo(-10);
  }

  @Test
  public void test_sameUnit_spaces() throws InvalidConfigurationException {
    assertThat(convertToInt("10    s", SECONDS)).isEqualTo(10);
  }

  @Test
  public void test_sameUnit() throws InvalidConfigurationException {
    assertThat(convertToInt("10s", SECONDS)).isEqualTo(10);
  }

  @Test
  public void test_sameUnit_negative() throws InvalidConfigurationException {
    assertThat(convertToInt("-10s", SECONDS)).isEqualTo(-10);
  }

  @Test
  public void test_otherUnit() throws InvalidConfigurationException {
    assertThat(convertToInt("10min", SECONDS)).isEqualTo(10 * 60);
  }

  @Test(expected = InvalidConfigurationException.class)
  public void test_onlyUnit() throws InvalidConfigurationException {
    assertThat(convertToInt("s", SECONDS)).isEqualTo(10 * 60);
  }

  @Test(expected = InvalidConfigurationException.class)
  @SuppressWarnings("CheckReturnValue")
  public void test_invalidUnit() throws InvalidConfigurationException {
    convertToInt("10foo", SECONDS);
  }

  @Test(expected = InvalidConfigurationException.class)
  @SuppressWarnings("CheckReturnValue")
  public void test_textPrefix() throws InvalidConfigurationException {
    convertToInt("foo10s", SECONDS);
  }

  @Test(expected = InvalidConfigurationException.class)
  @SuppressWarnings("CheckReturnValue")
  public void test_onlyText() throws InvalidConfigurationException {
    convertToInt("foo", SECONDS);
  }

  @Test
  public void test_returnsTimeSpan() throws InvalidConfigurationException {
    TimeSpan result = convertToTimeSpan("10", SECONDS);
    assertThat(result).isEqualTo(TimeSpan.ofSeconds(10));
    assertThat(result.getUnit()).isEqualTo(SECONDS);
  }

  @Test
  public void test_returnsTimeSpan_otherUnit() throws InvalidConfigurationException {
    TimeSpan result = convertToTimeSpan("10min", SECONDS);
    assertThat(result).isEqualTo(TimeSpan.of(10, MINUTES));
    assertThat(result.getUnit()).isEqualTo(MINUTES);
  }

  @Test
  public void test_returnsTimeSpan_differentDefaultUnit() throws InvalidConfigurationException {
    TimeSpan result = convertToTimeSpan("10", MINUTES);
    assertThat(result).isEqualTo(TimeSpan.of(10, MINUTES));
    assertThat(result.getUnit()).isEqualTo(MINUTES);
  }
}
