// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.configuration.converters;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import com.google.auto.value.AutoAnnotation;
import com.google.common.reflect.TypeToken;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;

public class TimeSpanTypeConverterTest {

  private static final Path DUMMY_PATH = Path.of("dummy.properties");
  private static final TypeToken<TimeSpan> TYPE_TOKEN = TypeToken.of(TimeSpan.class);

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
                DUMMY_PATH,
                LogManager.createTestLogManager());
  }

  private static TimeSpan convertToTimeSpan(String value, TimeUnit defaultUserUnit)
      throws InvalidConfigurationException {
    return (TimeSpan)
        new TimeSpanTypeConverter()
            .convert(
                "dummy",
                value,
                TYPE_TOKEN,
                createAnnotation(SECONDS, defaultUserUnit),
                DUMMY_PATH,
                LogManager.createTestLogManager());
  }

  private static void testInvalid(String value) {
    TimeSpanTypeConverter conv = new TimeSpanTypeConverter();
    TimeSpanOption annotation = createAnnotation(SECONDS, SECONDS);
    LogManager logger = LogManager.createTestLogManager();

    assertThrows(
        InvalidConfigurationException.class,
        () -> conv.convert("dummy", value, TYPE_TOKEN, annotation, DUMMY_PATH, logger));
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

  @Test
  public void test_onlyUnit() {
    testInvalid("s");
  }

  @Test
  public void test_invalidUnit() {
    testInvalid("10foo");
  }

  @Test
  public void test_textPrefix() {
    testInvalid("foo10s");
  }

  @Test
  public void test_onlyText() {
    testInvalid("foo");
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
