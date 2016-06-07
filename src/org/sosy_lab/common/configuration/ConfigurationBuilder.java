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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import org.sosy_lab.common.configuration.converters.TypeConverter;
import org.sosy_lab.common.io.MoreFiles;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 * Interface for constructing {@link Configuration} instances.
 */
@CanIgnoreReturnValue
public final class ConfigurationBuilder {

  @Nullable private Map<String, String> properties = null;
  @Nullable private Map<String, Path> sources = null;
  @Nullable private Configuration oldConfig = null;
  @Nullable private String prefix = null;
  @Nullable private Map<Class<?>, TypeConverter> converters = null;

  ConfigurationBuilder() {}

  private void setupProperties() {
    if (properties == null) {
      properties = new HashMap<>();
      sources = new HashMap<>();

      if (oldConfig != null) {
        properties.putAll(oldConfig.properties);
        sources.putAll(oldConfig.sources);
      }
    }
  }

  /**
   * Set a single option.
   */
  public ConfigurationBuilder setOption(String name, String value) {
    checkNotNull(name);
    checkNotNull(value);
    setupProperties();

    properties.put(name, value);
    sources.put(name, Paths.get(Configuration.NO_NAMED_SOURCE));

    return this;
  }

  /**
   * Reset a single option to its default value.
   */
  public ConfigurationBuilder clearOption(String name) {
    checkNotNull(name);
    setupProperties();

    properties.remove(name);
    sources.remove(name);

    return this;
  }

  /**
   * Add all options from a map.
   */
  public ConfigurationBuilder setOptions(Map<String, String> options) {
    checkNotNull(options);
    setupProperties();

    properties.putAll(options);
    for (String name : options.keySet()) {
      sources.put(name, Paths.get(Configuration.NO_NAMED_SOURCE));
    }

    return this;
  }

  /**
   * Set the optional prefix for new configuration.
   */
  public ConfigurationBuilder setPrefix(String newPrefix) {
    checkNotNull(newPrefix);

    this.prefix = newPrefix;

    return this;
  }

  /**
   * Copy everything from an existing Configuration instance. This also means
   * that the new configuration object created by this builder will share the
   * set of unused properties with the configuration instance passed to this
   * class.
   *
   * If this method is called, it has to be the first method call on this
   * builder instance.
   */
  public ConfigurationBuilder copyFrom(Configuration sourceConfig) {
    checkNotNull(sourceConfig);
    checkState(this.properties == null);
    checkState(this.sources == null);
    checkState(this.oldConfig == null);
    checkState(this.converters == null);

    this.oldConfig = sourceConfig;

    return this;
  }

  /**
   * Copy one single option from another Configuration instance,
   * overwriting the value in this builder, if it is already set.
   * The given Configuration instance needs to have a value for this option.
   *
   * It is better to use this method instead of
   * <code>setOption(option, oldConfig.getProperty(option))</code>,
   * because it retains the mapping to the source of this value,
   * which allows better error messages and resolving relative file paths.
   *
   * @param sourceConfig A configuration instance with a value for option.
   * @param option The name of a configuration option.
   * @throws IllegalArgumentException If the given configuration
   * does not specify a value for the given option.
   */
  public ConfigurationBuilder copyOptionFrom(Configuration sourceConfig, String option)
      throws IllegalArgumentException {
    checkNotNull(sourceConfig);
    checkNotNull(option);
    checkArgument(sourceConfig.properties.containsKey(option));
    setupProperties();

    properties.put(option, sourceConfig.properties.get(option));
    sources.put(option, sourceConfig.sources.get(option));

    return this;
  }

  /**
   * Copy one single option from another Configuration instance,
   * overwriting the value in this builder, if it is already set.
   * If the given Configuration instance does not have a value for this option,
   * nothing is changed.
   *
   * It is better to use this method instead of
   * <code>setOption(option, oldConfig.getProperty(option))</code>,
   * because it retains the mapping to the source of this value,
   * which allows better error messages and resolving relative file paths.
   *
   * @param sourceConfig A configuration instance.
   * @param option The name of a configuration option.
   */
  public ConfigurationBuilder copyOptionFromIfPresent(Configuration sourceConfig, String option) {
    checkNotNull(option);
    if (sourceConfig.properties.containsKey(option)) {
      copyOptionFrom(sourceConfig, option);
    }

    return this;
  }

  /**
   * Load options from a {@link CharSource} with a "key = value" format.
   *
   * A stream from this source is opened and closed by this method.
   * This method may additionally access more files from the file system
   * if they are included.
   *
   * @param source The source to read from.
   * @param basePath The directory where relative #include directives should be based on.
   * @param sourceName A string to use as source of the file in error messages
   * (this should usually be a filename or something similar).
   * @throws IOException If the stream cannot be read.
   * @throws InvalidConfigurationException If the stream contains an invalid format.
   */
  public ConfigurationBuilder loadFromSource(CharSource source, String basePath, String sourceName)
      throws IOException, InvalidConfigurationException {
    checkNotNull(source);
    checkNotNull(basePath);
    setupProperties();

    // Need to append something to base path because resolveSibling() is used.
    Path base = Paths.get(basePath).resolve("dummy");
    final Parser parser = Parser.parse(source, Optional.of(base), sourceName);
    properties.putAll(parser.getOptions());
    sources.putAll(parser.getSources());

    return this;
  }

  /**
   * Load options from a file with a "key = value" format.
   *
   * @throws IOException If the file cannot be read.
   * @throws InvalidConfigurationException If the file contains an invalid format.
   */
  public ConfigurationBuilder loadFromFile(String filename)
      throws IOException, InvalidConfigurationException {
    return loadFromFile(Paths.get(filename));
  }

  /**
   * Load options from a file with a "key = value" format.
   *
   * @throws IOException If the file cannot be read.
   * @throws InvalidConfigurationException If the file contains an invalid format.
   */
  public ConfigurationBuilder loadFromFile(Path file)
      throws IOException, InvalidConfigurationException {
    checkNotNull(file);

    MoreFiles.checkReadableFile(file);

    setupProperties();

    final Parser parser = Parser.parse(file);
    properties.putAll(parser.getOptions());
    sources.putAll(parser.getSources());

    return this;
  }

  /**
   * Load options from a class-loader resource with a "key = value" format.
   *
   * There must not be any #include directives in the resource.
   *
   * @param contextClass The class to use for looking up the resource.
   * @param resourceName The name of the resource relative to {@code contextClass}.
   * @throws IllegalArgumentException If the resource cannot be found or read,
   * or contains invalid syntax or #include directives.
   */
  public ConfigurationBuilder loadFromResource(Class<?> contextClass, String resourceName) {
    URL url = Resources.getResource(contextClass, resourceName);
    CharSource source = Resources.asCharSource(url, StandardCharsets.UTF_8);

    setupProperties();

    try {
      final Parser parser = Parser.parse(source, Optional.empty(), url.toString());
      properties.putAll(parser.getOptions());
      sources.putAll(parser.getSources());
    } catch (InvalidConfigurationException | IOException e) {
      throw new IllegalArgumentException(
          "Error in resource " + resourceName + " relative to " + contextClass.getName(), e);
    }

    return this;
  }

  /**
   * Add a type converter for options with a certain type.
   * This will enable the Configuration instance to parse strings into values
   * of the given type and inject them just as the base option types.
   *
   * As an alternative, the type of an option detail annotation
   * ({@link OptionDetailAnnotation}) can be given. In this case, the type
   * converter will be called for options annotated with this type.
   *
   * Previous type converters for the same type will be overwritten
   * (this also works for types usually handled by the Configuration class,
   * however not for collection and array types).
   *
   * The same converter may be used for several types.
   *
   * @param cls The type the type converter handles.
   * @param converter A converter instance.
   * @return this
   */
  public ConfigurationBuilder addConverter(Class<?> cls, TypeConverter converter) {
    checkNotNull(cls);
    checkNotNull(converter);

    if (converters == null) {
      converters = Configuration.createConverterMap();
      if (oldConfig != null) {
        converters.putAll(oldConfig.converters);
      } else {
        converters.putAll(Configuration.DEFAULT_CONVERTERS);
      }
    }

    converters.put(cls, converter);

    return this;
  }

  /**
   * Create a Configuration instance with the settings specified by method
   * calls on this builder instance.
   *
   * This method resets the builder instance, so that after this method has
   * returned it is exactly in the same state as directly after instantiation.
   */
  @CheckReturnValue
  public Configuration build() {
    ImmutableMap<String, String> newProperties;
    if (properties == null) {
      // we can re-use the old properties instance because it is immutable
      if (oldConfig != null) {
        newProperties = oldConfig.properties;
      } else {
        newProperties = ImmutableMap.of();
      }
    } else {
      newProperties = ImmutableMap.copyOf(properties);
    }

    ImmutableMap<String, Path> newSources;
    if (sources == null) {
      // we can re-use the old sources instance because it is immutable
      if (oldConfig != null) {
        newSources = oldConfig.sources;
      } else {
        newSources = ImmutableMap.of();
      }
    } else {
      newSources = ImmutableMap.copyOf(sources);
    }

    String newPrefix;
    if (prefix == null) {
      if (oldConfig != null) {
        newPrefix = oldConfig.prefix;
      } else {
        newPrefix = "";
      }
    } else {
      newPrefix = prefix;
    }

    ImmutableMap<Class<?>, TypeConverter> newConverters;
    if (converters == null) {
      // we can re-use the old converters instance because it is immutable
      if (oldConfig != null) {
        newConverters = oldConfig.converters;
      } else {
        newConverters = ImmutableMap.copyOf(Configuration.DEFAULT_CONVERTERS);
      }
    } else {
      newConverters = ImmutableMap.copyOf(converters);
    }

    Set<String> newUnusedProperties;
    Set<String> newDeprecatedProperties;
    if (oldConfig != null) {
      // share the same set of unused properties
      newUnusedProperties = oldConfig.unusedProperties;
      newDeprecatedProperties = oldConfig.deprecatedProperties;
    } else {
      newUnusedProperties = new HashSet<>(newProperties.keySet());
      newDeprecatedProperties = new HashSet<>(0);
    }

    Configuration newConfig =
        new Configuration(
            newProperties,
            newSources,
            newPrefix,
            newConverters,
            newUnusedProperties,
            newDeprecatedProperties,
            oldConfig != null ? oldConfig.getUsedOptionsPrintStream() : null,
            oldConfig != null ? oldConfig.getLogger() : null);

    // reset builder instance so that it may be re-used
    properties = null;
    prefix = null;
    oldConfig = null;

    return newConfig;
  }
}
