// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.configuration.converters;

import com.google.common.collect.FluentIterable;
import com.google.common.reflect.TypeToken;
import com.google.errorprone.annotations.Var;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.Collections;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.Classes.UnsuitedClassException;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;

public class ClassTypeConverter implements TypeConverter {

  @Override
  public Object convert(
      String optionName,
      String value,
      TypeToken<?> type,
      Annotation secondaryOption,
      Path pSource,
      LogManager logger)
      throws InvalidConfigurationException {

    // null means "no prefix"
    @Var Iterable<String> packagePrefixes = Collections.<String>singleton(null);

    // get optional package prefix
    if (secondaryOption != null) {
      if (!(secondaryOption instanceof ClassOption)) {
        throw new UnsupportedOperationException(
            "Options of type Class may not be annotated with " + secondaryOption);
      }
      packagePrefixes =
          FluentIterable.from(packagePrefixes)
              .append(((ClassOption) secondaryOption).packagePrefix());
    }

    // get class object
    @Var Class<?> cls = null;
    for (String prefix : packagePrefixes) {
      try {
        cls = Classes.forName(value, prefix);
      } catch (ClassNotFoundException ignored) {
        // Ignore, we try next prefix and throw below if none is found.
      }
    }
    if (cls == null) {
      throw new InvalidConfigurationException(
          "Class " + value + " specified in option " + optionName + " not found");
    }

    Object result;
    if (type.getRawType().equals(Class.class)) {
      // get value of type parameter
      TypeToken<?> targetType = Classes.getSingleTypeArgument(type);

      // check type
      if (!targetType.isSupertypeOf(cls)) {
        throw new InvalidConfigurationException(
            String.format(
                "Class %s specified in option %s is not an instance of %s",
                value, optionName, targetType));
      }

      result = cls;
      Classes.produceClassLoadingWarning(logger, cls, targetType.getRawType());

    } else {
      // This should be a factory interface for which we create a proxy implementation.
      try {
        result = Classes.createFactory(type, cls);
      } catch (UnsuitedClassException e) {
        throw new InvalidConfigurationException(
            String.format(
                "Class %s specified in option %s is invalid (%s)",
                value, optionName, e.getMessage()));
      }
      Classes.produceClassLoadingWarning(logger, cls, type.getRawType());
    }

    return result;
  }
}
