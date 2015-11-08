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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharSource;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.converters.TypeConverter;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

public class Builder implements ConfigurationBuilder {

  private @Nullable Map<String, String> properties = null;
  private @Nullable Map<String, Path> sources = null;
  private @Nullable Configuration oldConfig = null;
  private @Nullable String prefix = null;
  private @Nullable Map<Class<?>, TypeConverter> converters = null;

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

  /* (non-Javadoc)
   * @see org.sosy_lab.common.configuration.ConfigurationBuilder#setOption(java.lang.String, java.lang.String)
   */
  @Override
  public ConfigurationBuilder setOption(String name, String value) {
    Preconditions.checkNotNull(name);
    Preconditions.checkNotNull(value);
    setupProperties();

    properties.put(name, value);
    sources.put(name, Paths.get(Configuration.NO_NAMED_SOURCE));

    return this;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.common.configuration.ConfigurationBuilder#clearOption(java.lang.String)
   */
  @Override
  public ConfigurationBuilder clearOption(String name) {
    Preconditions.checkNotNull(name);
    setupProperties();

    properties.remove(name);
    sources.remove(name);

    return this;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.common.configuration.ConfigurationBuilder#setOptions(java.util.Map)
   */
  @Override
  public ConfigurationBuilder setOptions(Map<String, String> options) {
    Preconditions.checkNotNull(options);
    setupProperties();

    properties.putAll(options);
    for (String name : options.keySet()) {
      sources.put(name, Paths.get(Configuration.NO_NAMED_SOURCE));
    }

    return this;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.common.configuration.ConfigurationBuilder#setPrefix(java.lang.String)
   */
  @Override
  public ConfigurationBuilder setPrefix(String newPrefix) {
    Preconditions.checkNotNull(newPrefix);

    this.prefix = newPrefix;

    return this;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.common.configuration.ConfigurationBuilder#copyFrom(org.sosy_lab.common.configuration.Configuration)
   */
  @Override
  public ConfigurationBuilder copyFrom(Configuration sourceConfig) {
    Preconditions.checkNotNull(sourceConfig);
    Preconditions.checkState(this.properties == null);
    Preconditions.checkState(this.sources == null);
    Preconditions.checkState(this.oldConfig == null);
    Preconditions.checkState(this.converters == null);

    this.oldConfig = sourceConfig;

    return this;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.common.configuration.ConfigurationBuilder#copyOptionFrom(org.sosy_lab.common.configuration.Configuration, java.lang.String)
   */
  @Override
  public ConfigurationBuilder copyOptionFrom(Configuration sourceConfig, String option)
      throws IllegalArgumentException {
    Preconditions.checkNotNull(sourceConfig);
    Preconditions.checkNotNull(option);
    Preconditions.checkArgument(sourceConfig.properties.containsKey(option));
    setupProperties();

    properties.put(option, sourceConfig.properties.get(option));
    sources.put(option, sourceConfig.sources.get(option));

    return this;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.common.configuration.ConfigurationBuilder#loadFromSource(com.google.common.io.CharSource, java.lang.String, java.lang.String)
   */
  @Override
  public ConfigurationBuilder loadFromSource(CharSource source, String basePath,
      String sourceName) throws IOException, InvalidConfigurationException {
    Preconditions.checkNotNull(source);
    Preconditions.checkNotNull(basePath);
    setupProperties();

    final Pair<Map<String, String>, Map<String, Path>> content =
        Parser.parse(source, basePath, sourceName);
    properties.putAll(content.getFirst());
    sources.putAll(content.getSecond());

    return this;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.common.configuration.ConfigurationBuilder#loadFromStream(java.io.InputStream, java.lang.String, java.lang.String)
   */
  @Deprecated
  @Override
  public ConfigurationBuilder loadFromStream(InputStream stream, String basePath,
      String sourceName) throws IOException, InvalidConfigurationException {
    Preconditions.checkNotNull(stream);
    Preconditions.checkNotNull(basePath);

    byte[] rawContent = ByteStreams.toByteArray(stream);
    CharSource source = ByteSource.wrap(rawContent)
                                  .asCharSource(StandardCharsets.UTF_8);

    return loadFromSource(source, basePath, sourceName);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.common.configuration.ConfigurationBuilder#loadFromStream(java.io.InputStream)
   */
  @Override
  @Deprecated
  public ConfigurationBuilder loadFromStream(InputStream stream)
      throws IOException, InvalidConfigurationException {
    return loadFromStream(stream, "", "unknown source");
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.common.configuration.ConfigurationBuilder#loadFromFile(java.lang.String)
   */
  @Override
  public ConfigurationBuilder loadFromFile(@Nullable String filename)
      throws IOException, InvalidConfigurationException {
    return loadFromFile(Paths.get(filename));
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.common.configuration.ConfigurationBuilder#loadFromFile(org.sosy_lab.common.io.Path)
   */
  @Override
  public ConfigurationBuilder loadFromFile(Path file)
      throws IOException, InvalidConfigurationException {
    Preconditions.checkNotNull(file);

    if (!file.exists()) {
      throw new IOException("The file does not exist.");
    }

    setupProperties();

    final Pair<Map<String, String>, Map<String, Path>> content = Parser.parse(file, "");
    properties.putAll(content.getFirst());
    sources.putAll(content.getSecond());

    return this;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.common.configuration.ConfigurationBuilder#addConverter(java.lang.Class, org.sosy_lab.common.configuration.converters.TypeConverter)
   */
  @Override
  public ConfigurationBuilder addConverter(Class<?> cls, TypeConverter converter) {
    Preconditions.checkNotNull(cls);
    Preconditions.checkNotNull(converter);

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

  /* (non-Javadoc)
   * @see org.sosy_lab.common.configuration.ConfigurationBuilder#build()
   */
  @Override
  public Configuration build() throws InvalidConfigurationException {
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

    Configuration newConfig = new Configuration(newProperties, newSources, newPrefix,
        newConverters, newUnusedProperties, newDeprecatedProperties,
        oldConfig != null ? oldConfig.getLogger() : null);
    newConfig.inject(newConfig);

    // reset builder instance so that it may be re-used
    properties = null;
    prefix = null;
    oldConfig = null;

    return newConfig;
  }
}