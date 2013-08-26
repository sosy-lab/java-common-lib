/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.common.configuration.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

import com.google.common.primitives.Primitives;

/**
 * A {@link TypeConverter} which handles all the trivial cases like ints, Strings,
 * log levels, regexps, etc.
 *
 * This class should not have any relevance outside the {@link Configuration} class.
 */
public enum BaseTypeConverter implements TypeConverter {

  INSTANCE;

  @Override
  public Object convert(String optionName, String valueStr, Class<?> type, Type pGenericType,
      Annotation pSecondaryOption, Path pSource) throws InvalidConfigurationException {

    if (Primitives.isWrapperType(type)) {
      // all wrapper types have valueOf method
      return valueOf(type, optionName, valueStr);

    } else if (type.isEnum()) {
      try {
        return convertEnum(type, valueStr);
      } catch (IllegalArgumentException e) {
        throw new InvalidConfigurationException("Invalid value " + valueStr + " for option " + optionName);
      }

    } else if (type.equals(String.class)) {
      return valueStr;

    } else if (type.equals(Level.class)) {
      try {
        return Level.parse(valueStr);
      } catch (IllegalArgumentException e) {
        throw new InvalidConfigurationException("Illegal log level " + valueStr + " in option " + optionName);
      }

    } else if (type.equals(Pattern.class)) {
      try {
        return Pattern.compile(valueStr);
      } catch (PatternSyntaxException e) {
        throw new InvalidConfigurationException("Illegal regular expression " + valueStr + " in option " + optionName + " (" + e.getMessage() + ")", e);
      }

    } else {
      throw new UnsupportedOperationException(
          "Unimplemented type for option: " + type.getSimpleName());
    }
  }

  @Override
  public <T> T convertDefaultValue(String pOptionName, T pValue, Class<T> pType, Type pGenericType,
      Annotation pSecondaryOption) throws InvalidConfigurationException {

    return pValue;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Object convertEnum(Class<?> cls, String value) {
    return Enum.valueOf((Class)cls, value);
  }

  /**
   * Invoke the static "valueOf(String)" method on a class.
   * Helpful for type converters.
   */
  public static Object valueOf(final Class<?> type, final String optionName, final String value)
      throws InvalidConfigurationException {
    return invokeStaticMethod(type, "valueOf", String.class, value, optionName);
  }

  public static <T> Object invokeStaticMethod(final Class<?> type, final String method,
      final Class<T> paramType, final T value, final String optionName)
          throws InvalidConfigurationException {
    try {
      Method m = type.getMethod(method, paramType);
      if (!m.isAccessible()) {
        m.setAccessible(true);
      }
      return m.invoke(null, value);

    } catch (NoSuchMethodException e) {
      throw new AssertionError("Class " + type.getSimpleName() + " without "
          + method + "(" + paramType.getSimpleName() + ") method!");
    } catch (SecurityException e) {
      throw new AssertionError("Class " + type.getSimpleName() + " without accessible "
          + method + "(" + paramType.getSimpleName() + ") method!");
    } catch (IllegalAccessException e) {
      throw new AssertionError("Class " + type.getSimpleName() + " without accessible "
          + method + "(" + paramType.getSimpleName() + ") method!");
    } catch (InvocationTargetException e) {
      throw new InvalidConfigurationException("Could not parse \"" + optionName +
          " = " + value + "\" (" + e.getTargetException().getMessage() + ")", e);
    }
  }
}
