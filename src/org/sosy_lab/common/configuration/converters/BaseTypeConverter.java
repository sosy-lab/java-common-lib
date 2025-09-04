// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.configuration.converters;

import com.google.common.primitives.Primitives;
import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.rationals.Rational;

/**
 * A {@link TypeConverter} which handles all the trivial cases like ints, Strings, log levels,
 * regexps, etc.
 *
 * <p>This class should not have any relevance outside the {@link Configuration} class.
 */
public enum BaseTypeConverter implements TypeConverter {
  INSTANCE;

  @Override
  @SuppressWarnings("UnusedException") // unchecked exceptions are expected, cause has no value
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
                valueStr, optionName, e.getMessage()),
            e);
      }
    } else if (type.equals(Rational.class)) {
      try {
        return Rational.of(valueStr);
      } catch (IllegalArgumentException e) {
        throw new InvalidConfigurationException(
            String.format(
                "Illegal rational %s in option  %s (%s).", valueStr, optionName, e.getMessage()),
            e);
      }

    } else {
      throw new UnsupportedOperationException(
          "Unimplemented type for option: " + type.getSimpleName());
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Object convertEnum(Class<?> cls, String value) {
    return Enum.valueOf((Class) cls, value);
  }

  /** Invoke the static "valueOf(String)" method on a class. Helpful for type converters. */
  public static Object valueOf(Class<?> type, String optionName, String value)
      throws InvalidConfigurationException {
    return invokeStaticMethod(type, "valueOf", String.class, value, optionName);
  }

  @SuppressWarnings(
      "RethrowReflectiveOperationExceptionAsLinkageError") // LinkageError does not fit
  public static <T> Object invokeStaticMethod(
      Class<?> type, String method, Class<T> paramType, T value, String optionName)
      throws InvalidConfigurationException {
    try {
      return type.getMethod(method, paramType).invoke(null, value);

    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(
          String.format(
              "Class %s without usable %s(%s) method.",
              type.getSimpleName(), method, paramType.getSimpleName()),
          e);
    } catch (InvocationTargetException e) {
      throw new InvalidConfigurationException(
          String.format(
              "Could not parse \"%s = %s\" (%s).",
              optionName, value, e.getTargetException().getMessage()),
          e);
    }
  }
}
