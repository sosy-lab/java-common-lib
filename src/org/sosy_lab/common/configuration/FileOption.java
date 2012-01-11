/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import java.io.File;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is an annotation providing more features for options of type {@link File}.
 * In order to use it, you need to register an instance of {@link FileTypeConverter}
 * as a converter for {@link FileOption}.
 */
@OptionDetailAnnotation(applicableTo=File.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface FileOption {

  /**
   * More details for file options.
   */
  public Type value();

  public static enum Type {
    /**
     * The file specified with this option is a required input file
     * (a non-existing file will be considered an invalid configuration).
     */
    REQUIRED_INPUT_FILE,

    /**
     * The file specified with this option is a file (i.e., no directory),
     * but it needs not exist.
     */
    OPTIONAL_INPUT_FILE,

    /**
     * The file specified with this option will be created by the tool.
     * I doesn't matter whether this file already exists, but it may not be a directory.
     */
    OUTPUT_FILE,
  }
}
