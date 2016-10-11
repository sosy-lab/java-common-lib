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
import java.util.concurrent.TimeUnit;
import org.sosy_lab.common.time.TimeSpan;
/**
 * This is an annotation for all integer options that specify some sort of time duration (e.g., a
 * timeout). Values for options with this annotation can be given with units. Examples: 10s 5min 3h
 * Supported units are "ns", "ms", "s", "min", and "h". Microseconds are not supported.
 */
@OptionDetailAnnotation(applicableTo = {Integer.class, Long.class, TimeSpan.class})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface TimeSpanOption {

  /** The unit which should be assumed when the user does not explicitly specify a unit. */
  TimeUnit defaultUserUnit() default TimeUnit.SECONDS;

  /**
   * The unit which will be used to write the value from the user into the annotated field if the
   * field is of type int or long. This is also the unit of the default value of this option (if one
   * is given), and of the minimum and maximum value!
   */
  TimeUnit codeUnit();

  /** An optional minimum value for this option. The unit of the minimum is {@link #codeUnit()}. */
  long min() default Long.MIN_VALUE;

  /** An optional maximum value for this option. The unit of the maximum is {@link #codeUnit()}. */
  long max() default Long.MAX_VALUE;
}
