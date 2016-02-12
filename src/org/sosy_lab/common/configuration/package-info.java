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
/**
 * Java-Config is a library for injecting configuration options
 * in a decentralized way.
 *
 * <p>{@link org.sosy_lab.common.configuration.Configuration}
 * objects can be generated either from {@code .properties}
 * configuration files, or from command line options.
 * The usability is geared towards configuration files, but command line
 * generation is also supported.
 *
 * <p>The library is conceptually similar to
 * <a href="http://gflags.github.io/gflags/">GFlags</a>
 * and allows arbitrary option injection throughout used classes.
 *
 * <p><strong>Annotating classes with options</strong></p>
 * <p>The example below demonstrates defining options for a class:
 *
 * <code>
 * <pre>
 * {@literal @}Options(prefix="grep")
 * public class Grep {
 *   {@literal @}Option(description="Ignore case of the query", secure=true)
 *   private boolean ignoreCase = false;
 *
 *   {@literal @}Option(description="File to search", secure=true)
 *   {@literal @}FileOption(Type.REQUIRED_INPUT_FILE)
 *   private PathCounterTemplate haystack = null;
 *
 *   public Grep(Configuration c) {
 *     c.inject(this);
 *   }
 *
 *   public boolean search(String needle) {
 *      // ... search for a needle in a haystack.
 *   }
 * </pre>
 * </code>
 *
 * <p>Note the following features:
 * <ul>
 *   <li>{@code @Option} annotations are used to define various
 *     <i>options</i> associated with a class.
 *     Options are decentralized, the only requirement is that the
 *     {@link org.sosy_lab.common.configuration.Configuration} object is injected
 *     (preferably in the constructor).
 *   </li>
 *
 *   <li>
 *     The fields defining options can be private.
 *     The injector contains reflection calls to set them to the arbitrary file.
 *   </li>
 *
 *   <li>Normally, the <em>type</em> of the option is defined by the type
 *   of the field.
 *   For complex cases (e.g. files) additional decorators are used.
 *   </li>
 *
 *   <li>
 *     Option name is either derived from the field name (prefixed with a base
 *      <em>prefix</em>), or set explicitly in the {@link org.sosy_lab.common.configuration.Option}
 *      annotation.
 *   </li>
 * </ul>
 *
 * <p>Configuration options instance can be constructed in three different ways:
 * <ul>
 *   <li>
 *     Most common if you have a <em>lot</em> of options and a large project:
 *     load them from the
 *     configuration file.
 *     See {@link org.sosy_lab.common.configuration.ConfigurationBuilder#loadFromFile}
 *     for details.
 *   </li>
 *   <li>
 *     Useful for smaller programs: construct an instance from command line
 *     options. See
 *     {@link org.sosy_lab.common.configuration.Configuration#fromCmdLineArguments}.
 *   </li>
 *   <li>
 *     Most rare, useful for small scripts: construct an instance by hand,
 *     using {@link org.sosy_lab.common.configuration.ConfigurationBuilder}.
 *   </li>
 * </ul>
 */
@javax.annotation.CheckReturnValue
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
package org.sosy_lab.common.configuration;
