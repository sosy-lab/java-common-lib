// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.configuration.converters;

import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;

/**
 * Type converter for options of types Integer/Long annotated with {@link IntegerOption} (not for
 * integer options without this annotation).
 */
public class IntegerTypeConverter implements TypeConverter {

  @Override
  public Object convert(
      String optionName,
      String valueStr,
      TypeToken<?> pType,
      Annotation pOption,
      Path pSource,
      LogManager logger)
      throws InvalidConfigurationException {
    Class<?> type = pType.getRawType();

    if (!(pOption instanceof IntegerOption)) {
      throw new UnsupportedOperationException(
          "IntegerTypeConverter needs options annotated with @IntegerOption");
    }
    IntegerOption option = (IntegerOption) pOption;

    assert type.equals(Integer.class) || type.equals(Long.class);

    Object value = BaseTypeConverter.valueOf(type, optionName, valueStr);

    long n = ((Number) value).longValue();
    if (option.min() > n || n > option.max()) {
      throw new InvalidConfigurationException(
          String.format(
              "Invalid value in configuration file: \"%s = %s\" (not in range [%d, %d]).",
              optionName, value, option.min(), option.max()));
    }

    return value;
  }

  @Override
  public <T> T convertDefaultValue(
      String pOptionName, T pValue, TypeToken<T> pType, Annotation pSecondaryOption) {

    return pValue;
  }
}
