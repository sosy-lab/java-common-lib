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

import com.google.common.primitives.Primitives;
import com.google.common.reflect.TypeToken;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.rationals.Rational;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A {@link TypeConverter} which handles all the trivial cases like ints, Strings,
 * log levels, regexps, etc.
 *
 * This class should not have any relevance outside the {@link Configuration} class.
 */
public enum BaseTypeConverter implements TypeConverter {
  INSTANCE;

  @Override
  public Object convert(
      String optionName,
      String valueStr,
      TypeToken<?> pType,
      Annotation pSecondaryOption,
      Path pSource,
      LogManager logger)
      throws InvalidConfigurationException {
    Class<?> type = pType.getRawType();

    if (Primitives.isWrapperType(type)) {
      // all wrapper types have valueOf method
      return valueOf(type, optionName, valueStr);

    } else if (type.isEnum()) {
      try {
        return convertEnum(type, valueStr);
      } catch (IllegalArgumentException e) {
        throw new InvalidConfigurationException(
            "Invalid value " + valueStr + " for option " + optionName);
      }

    } else if (type.equals(String.class)) {
      return valueStr;

    } else if (type.equals(Charset.class)) {
      try {
        return Charset.forName(valueStr);
      } catch (IllegalCharsetNameException e) {
        throw new InvalidConfigurationException(
            "Illegal charset '" + valueStr + "' in option " + optionName, e);
      } catch (UnsupportedCharsetException e) {
        throw new InvalidConfigurationException(
            "Unsupported charset " + valueStr + " in option " + optionName, e);
      }

    } else if (type.equals(Level.class)) {
      try {
        return Level.parse(valueStr);
      } catch (IllegalArgumentException e) {
        throw new InvalidConfigurationException(
            "Illegal log level " + valueStr + " in option " + optionName);
      }

    } else if (type.equals(Pattern.class)) {
      try {
        return Pattern.compile(valueStr);
      } catch (PatternSyntaxException e) {
        throw new InvalidConfigurationException(
            String.format(
                "Illegal regular expression %s in option  %s (%s).",
                valueStr,
                optionName,
                e.getMessage()),
            e);
      }
    } else if (type.equals(Rational.class)) {
      try {
        return Rational.of(valueStr);
      } catch (IllegalArgumentException e) {
        throw new InvalidConfigurationException(
            String.format("Illegal rational %s in option  %s (%s).",
                valueStr, optionName, e.getMessage()), e);
      }

    } else {
      throw new UnsupportedOperationException(
          "Unimplemented type for option: " + type.getSimpleName());
    }
  }

  @Override
  public <T> T convertDefaultValue(
      String pOptionName, T pValue, TypeToken<T> pType, Annotation pSecondaryOption)
      throws InvalidConfigurationException {

    return pValue;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Object convertEnum(Class<?> cls, String value) {
    return Enum.valueOf((Class) cls, value);
  }

  /**
   * Invoke the static "valueOf(String)" method on a class.
   * Helpful for type converters.
   */
  public static Object valueOf(final Class<?> type, final String optionName, final String value)
      throws InvalidConfigurationException {
    return invokeStaticMethod(type, "valueOf", String.class, value, optionName);
  }

  public static <T> Object invokeStaticMethod(
      final Class<?> type,
      final String method,
      final Class<T> paramType,
      final T value,
      final String optionName)
      throws InvalidConfigurationException {
    try {
      Method m = type.getMethod(method, paramType);
      if (!m.isAccessible()) {
        m.setAccessible(true);
      }
      return m.invoke(null, value);

    } catch (NoSuchMethodException | SecurityException | IllegalAccessException e) {
      throw new AssertionError(
          String.format(
              "Class %s without usable %s(%s) method.",
              type.getSimpleName(),
              method,
              paramType.getSimpleName()),
          e);
    } catch (InvocationTargetException e) {
      throw new InvalidConfigurationException(
          String.format(
              "Could not parse \"%s = %s\" (%s).",
              optionName,
              value,
              e.getTargetException().getMessage()),
          e);
    }
  }
}
