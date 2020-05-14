// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.configuration.converters;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.reflect.TypeToken;
import com.google.errorprone.annotations.Var;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;

/** Type converter for options annotated with {@link TimeSpanOption}. */
public class TimeSpanTypeConverter implements TypeConverter {

  private static final ImmutableBiMap<String, TimeUnit> TIME_UNITS =
      ImmutableBiMap.of(
          "ns", TimeUnit.NANOSECONDS,
          "ms", TimeUnit.MILLISECONDS,
          "s", TimeUnit.SECONDS,
          "min", TimeUnit.MINUTES,
          "h", TimeUnit.HOURS);

  private static final CharMatcher LETTER_MATCHER = CharMatcher.inRange('a', 'z');

  @Override
  @SuppressWarnings("UnusedException") // NumberFormatException is expected, cause has no value
  public Object convert(
      String optionName,
      String valueStr,
      TypeToken<?> pType,
      Annotation pOption,
      Path pSource,
      LogManager logger)
      throws InvalidConfigurationException {
    Class<?> type = pType.getRawType();

    if (!(pOption instanceof TimeSpanOption)) {
      throw new UnsupportedOperationException(
          "Time span options need to be annotated with @TimeSpanOption");
    }
    TimeSpanOption option = (TimeSpanOption) pOption;

    // find unit in input string
    @Var int i = valueStr.length() - 1;
    while (i >= 0 && LETTER_MATCHER.matches(valueStr.charAt(i))) {
      i--;
    }
    if (i < 0) {
      throw new InvalidConfigurationException("Option " + optionName + " contains no number");
    }

    // convert unit string to TimeUnit
    String userUnitStr = valueStr.substring(i + 1).trim();
    TimeUnit userUnit =
        userUnitStr.isEmpty() ? option.defaultUserUnit() : TIME_UNITS.get(userUnitStr);
    if (userUnit == null) {
      throw new InvalidConfigurationException("Option " + optionName + " contains invalid unit");
    }

    // Parse string without unit
    long rawValue;
    try {
      rawValue = Long.parseLong(valueStr.substring(0, i + 1).trim());
    } catch (NumberFormatException e) {
      throw new InvalidConfigurationException("Option " + optionName + " contains invalid number");
    }

    // convert value from user unit to code unit
    TimeUnit codeUnit = option.codeUnit();
    long value = codeUnit.convert(rawValue, userUnit);

    if (option.min() > value || value > option.max()) {
      String codeUnitStr = TIME_UNITS.inverse().get(codeUnit);
      throw new InvalidConfigurationException(
          String.format(
              "Invalid value in configuration file: \"%s = %s (not in range [%d %s, %d %s])",
              optionName, value, option.min(), codeUnitStr, option.max(), codeUnitStr));
    }

    Object result;

    if (type.equals(Integer.class)) {
      if (value > Integer.MAX_VALUE) {
        throw new InvalidConfigurationException(
            "Value for option " + optionName + " is larger than " + Integer.MAX_VALUE);
      }
      result = (int) value;
    } else if (type.equals(Long.class)) {
      result = value;
    } else {
      assert type.equals(TimeSpan.class);
      result = TimeSpan.of(rawValue, userUnit);
    }
    return result;
  }

  @Override
  public <T> T convertDefaultValue(
      String pOptionName, T pValue, TypeToken<T> pType, Annotation pSecondaryOption) {

    return pValue;
  }
}
