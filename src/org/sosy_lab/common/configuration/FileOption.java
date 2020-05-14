// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.configuration;

import java.io.File;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Path;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.io.PathCounterTemplate;
import org.sosy_lab.common.io.PathTemplate;

/**
 * This is an annotation providing more features for options of types {@link File} and {@link Path}.
 * In order to use it, you need to register an instance of {@link FileTypeConverter} as a converter
 * for {@link FileOption}.
 */
@OptionDetailAnnotation(
    applicableTo = {File.class, Path.class, PathTemplate.class, PathCounterTemplate.class})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface FileOption {

  /** More details for file options. */
  Type value();

  enum Type {
    /**
     * The file specified with this option is a required input file (a non-existing file will be
     * considered an invalid configuration).
     */
    REQUIRED_INPUT_FILE,

    /**
     * The file specified with this option is a file (i.e., no directory), but it needs not exist.
     */
    OPTIONAL_INPUT_FILE,

    /**
     * The file specified with this option will be created by the tool. I doesn't matter whether
     * this file already exists, but it may not be a directory.
     */
    OUTPUT_FILE,

    /**
     * The directory specified with this option will be created by the tool. I doesn't matter
     * whether this directory already exists, but it may not be a file.
     */
    OUTPUT_DIRECTORY,
  }
}
