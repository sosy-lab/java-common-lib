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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This is an optional annotation for all configuration options (i.e., elements
 * that are annotated with {@link Option}) whose type is an integer number
 * (i.e., int, long, Integer and Long):
 *
 * It serves to specify minimal and/or maximal values.
 */
@OptionDetailAnnotation(applicableTo= {Integer.class, Long.class})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface IntegerOption {

  /**
   * An optional minimum value for this option.
   */
  long min() default Long.MIN_VALUE;

  /**
   * An optional maximum value for this option.
   */
  long max() default Long.MAX_VALUE;
}
