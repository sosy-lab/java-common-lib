// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package provides the possibility to convert String values into appropriate objects of
 * certain types. This is used by the {@link org.sosy_lab.common.configuration.Configuration} class
 * to convert configuration options into objects before injecting them. The primary interface is
 * {@link org.sosy_lab.common.configuration.converters.TypeConverter}, and some default
 * implementations for commonly-used classes are also provided.
 */
@com.google.errorprone.annotations.CheckReturnValue
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
package org.sosy_lab.common.configuration.converters;
