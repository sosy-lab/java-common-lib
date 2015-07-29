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
package org.sosy_lab.common.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for a class which has fields or methods with an {@link Option} annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Options {

  /**
   * An optional prefix for all configuration options of the class annotated
   * with this type. Prefix and name of the option will be separated by a dot.
   */
  String prefix() default "";

  /**
   * When the prefix needs to be renamed, often it is desirable to maintain
   * the backwards compatibility with the previous config.
   * In that case, the previous prefix can be moved to the field
   * {@code deprecatedPrefix}.
   * Both normal and deprecated prefixes would work,
   * with latter printing the deprecation warning.
   */
  String deprecatedPrefix() default Configuration.NO_DEPRECATED_PREFIX;

  /**
   * An optional text, that describes the current options.
   */
  String description() default "";

}
