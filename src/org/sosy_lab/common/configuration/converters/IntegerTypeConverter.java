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
