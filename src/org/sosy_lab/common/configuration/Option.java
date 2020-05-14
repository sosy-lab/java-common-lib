// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to mark fields or methods which should get configuration values injected. Such a
 * field or method must be contained in a class annotated with {@link Options}.
 *
 * <p>While it is possible to mark final fields with this annotation, the java documentation says
 * that the behavior of setting final fields will be undetermined. It might happen that some parts
 * of the code do not see the new value.
 *
 * <p>It is possible to mark private fields with this annotation, all access modifiers will be
 * ignored when setting the value.
 *
 * <p>If a method is marked with this annotation, it has to have exactly one parameter. An result
 * value would be ignored, so the method should by of type void. If the method throws an {@link
 * IllegalArgumentException}, the configuration will be rejected as invalid. If the method throws
 * any other exception, the behavior is undefined (so it should not throw any other exception!).
 *
 * <p>If an option is not present in the configuration file, the corresponding field is not touched.
 * Similarly, a corresponding method is not called.
 *
 * <p>It is possible to specify one option more than once, both for fields and methods. All of the
 * fields will be set and all of the methods will be called.
 *
 * @see Configuration
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Option {

  /**
   * An optional name for the option as it appears in the configuration file. If not specified, the
   * name of the field will be used. In both cases it will be prefixed with the prefix specified in
   * the {@link Options} annotation (if given).
   */
  String name() default "";

  /**
   * When the prefix needs to be renamed, often it is desirable to maintain the backwards
   * compatibility with the previous config. In that case, the previous name can be moved to the
   * field {@code deprecatedName}. Both normal and deprecated name would work, with latter printing
   * the deprecation warning.
   */
  String deprecatedName() default "";

  /**
   * An optional flag that this configuration option is secure, i.e., setting it to an arbitrary
   * attacker-controlled value may not allow any harm (like abitrary code execution).
   */
  boolean secure() default false;

  /**
   * An optional flag if this option needs to be specified in the configuration file. The default is
   * false. If set to true, an exception will be thrown if the option is not in the file.
   */
  boolean required() default false;

  /**
   * An optional flag that specifies if a configuration value should be converted to upper case
   * after it was read. For options with enum types, this flag is always assumed to be true.
   */
  boolean toUppercase() default false;

  /**
   * If regexp is specified, the value of this option (prior to conversion to the correct type) will
   * be checked against this regular expression. If it does not match, an exception will be thrown.
   */
  String regexp() default "";

  /**
   * If values is non-empty, the value of this option (prior to conversion to the correct type) will
   * be checked if it is listed in this array. If it is not contained, an exception will be thrown.
   */
  String[] values() default {};

  /** A text that describes the current option (this will be part of the user documentation). */
  String description();
}
