/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;

import com.google.common.collect.Iterables;


public class ClassTypeConverter implements TypeConverter {

  @Override
  public Object convert(String optionName, String value, Class<?> type, Type genericType,
      Annotation secondaryOption, Path pSource, LogManager logger) throws InvalidConfigurationException {

    Iterable<String> packagePrefixes = Collections.<String>singleton(null); // null means "no prefix"

    // get optional package prefix
    if (secondaryOption != null) {
      if (!(secondaryOption instanceof ClassOption)) {
        throw new UnsupportedOperationException("Options of type Class may not be annotated with " + secondaryOption);
      }
      packagePrefixes = Iterables.concat(packagePrefixes,
          Arrays.asList(((ClassOption) secondaryOption).packagePrefix()));
    }

    // get value of type parameter
    Class<?> targetType = Classes.getComponentType(genericType).getFirst();

    // get class object
    Class<?> cls = null;
    for (String prefix : packagePrefixes) {
      try {
        cls = Classes.forName(value, prefix);
      } catch (ClassNotFoundException _) {
        // Ignore, we try next prefix and throw below if none is found.
      }
    }
    if (cls == null) {
      throw new InvalidConfigurationException("Class " + value + " specified in option " + optionName + " not found");
    }

    // check type
    if (!targetType.isAssignableFrom(cls)) {
      throw new InvalidConfigurationException("Class " + value + " specified in option " + optionName + " is not an instance of " + targetType.getCanonicalName());
    }

    Classes.produceClassLoadingWarning(logger, cls, targetType);

    return cls;
  }

  @Override
  public <T> T convertDefaultValue(String pOptionName, T pValue, Class<T> pType, Type pGenericType,
      Annotation pSecondaryOption) {

    return pValue;
  }

}
