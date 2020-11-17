// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.configuration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.converters.TypeConverter;
import org.sosy_lab.common.io.IO;

/** Interface for constructing {@link Configuration} instances. */
@CanIgnoreReturnValue
public final class ConfigurationBuilder {

  @Nullable private Map<String, String> properties = null;
  @Nullable private Map<String, Path> sources = null;
  @Nullable private Configuration oldConfig = null;
  @Nullable private String prefix = null;

  /**
   * Map of to-be-used converters or null. If null, no converters have been explicitly set yet. If
   * not null, only the converters that have been explicitly added to this builder are in the map.
   */
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

  /** Set a single option. */
  public ConfigurationBuilder setOption(String name, String value) {
    checkNotNull(name);
    checkNotNull(value);
    setupProperties();

    properties.put(name, value);
    sources.put(name, Paths.get(Configuration.NO_NAMED_SOURCE));

    return this;
  }

  /** Reset a single option to its default value. */
  public ConfigurationBuilder clearOption(String name) {
    checkNotNull(name);
    setupProperties();

    properties.remove(name);
    sources.remove(name);

    return this;
  }

  /** Add all options from a map. */
  public ConfigurationBuilder setOptions(Map<String, String> options) {
    checkNotNull(options);
    setupProperties();

    properties.putAll(options);
    for (String name : options.keySet()) {
      sources.put(name, Paths.get(Configuration.NO_NAMED_SOURCE));
    }

    return this;
  }

  /** Set the optional prefix for new configuration. */
  public ConfigurationBuilder setPrefix(String newPrefix) {
    checkNotNull(newPrefix);

    this.prefix = newPrefix;

    return this;
  }

  /**
   * Copy everything from an existing Configuration instance. This also means that the new
   * configuration object created by this builder will share the set of unused properties with the
   * configuration instance passed to this class.
   *
   * <p>If this method is called, it has to be the first method call on this builder instance.
   *
   * <p>The converters registered on the given {@link Configuration} instance will have {@link
   * TypeConverter#getInstanceForNewConfiguration(Configuration)} called on them and the result will
   * be used as converter in the new configuration, except if overridden with {@link
   * #addConverter(Class, TypeConverter)}.
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
   * Copy one single option from another Configuration instance, overwriting the value in this
   * builder, if it is already set. The given Configuration instance needs to have a value for this
   * option.
   *
   * <p>It is better to use this method instead of <code>
   * setOption(option, oldConfig.getProperty(option))</code>, because it retains the mapping to the
   * source of this value, which allows better error messages and resolving relative file paths.
   *
   * @param sourceConfig A configuration instance with a value for option.
   * @param option The name of a configuration option.
   * @throws IllegalArgumentException If the given configuration does not specify a value for the
   *     given option.
   */
  public ConfigurationBuilder copyOptionFrom(Configuration sourceConfig, String option) {
    checkNotNull(sourceConfig);
    checkNotNull(option);
    checkArgument(sourceConfig.properties.containsKey(option));
    setupProperties();

    properties.put(option, sourceConfig.properties.get(option));
    sources.put(option, sourceConfig.sources.get(option));

    return this;
  }

  /**
   * Copy one single option from another Configuration instance, overwriting the value in this
   * builder, if it is already set. If the given Configuration instance does not have a value for
   * this option, nothing is changed.
   *
   * <p>It is better to use this method instead of <code>
   * setOption(option, oldConfig.getProperty(option))</code>, because it retains the mapping to the
   * source of this value, which allows better error messages and resolving relative file paths.
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
   * <p>A stream from this source is opened and closed by this method. This method may additionally
   * access more files from the file system if they are included.
   *
   * @param source The source to read from.
   * @param basePath The directory where relative #include directives should be based on.
   * @param sourceName A string to use as source of the file in error messages (this should usually
   *     be a filename or something similar).
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
    Parser parser = Parser.parse(source, Optional.of(base), sourceName);
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

    IO.checkReadableFile(file);

    setupProperties();

    Parser parser = Parser.parse(file);
    properties.putAll(parser.getOptions());
    sources.putAll(parser.getSources());

    return this;
  }

  /**
   * Load options from a class-loader resource with a "key = value" format.
   *
   * <p>There must not be any #include directives in the resource.
   *
   * @param contextClass The class to use for looking up the resource.
   * @param resourceName The name of the resource relative to {@code contextClass}.
   * @throws IllegalArgumentException If the resource cannot be found or read, or contains invalid
   *     syntax or #include directives.
   */
  @SuppressWarnings("try")
  public ConfigurationBuilder loadFromResource(Class<?> contextClass, String resourceName) {
    URL url = Resources.getResource(contextClass, resourceName);
    CharSource source = Resources.asCharSource(url, StandardCharsets.UTF_8);

    setupProperties();

    try {
      URI uri = url.toURI();
      try (FileSystem fs = getFileSystemForUriInJars(uri)) {
        // Path uses FileSystemProvider internally to access the file, thus fs is unused.
        Path sourcePath = Paths.get(uri);
        parseSource(
            contextClass, resourceName, source, Optional.of(sourcePath), sourcePath.toString());
      }
    } catch (URISyntaxException
        | FileSystemNotFoundException
        | IllegalArgumentException
        | IOException e) {
      // If this fails, e.g., because url is a HTTP URL, we can also use the raw string.
      // This will not allow resolving relative path names, but everything else works.
      parseSource(contextClass, resourceName, source, Optional.empty(), url.toString());
    }

    return this;
  }

  private void parseSource(
      Class<?> contextClass,
      String resourceName,
      CharSource source,
      Optional<Path> sourcePath,
      String sourceString) {
    try {
      Parser parser = Parser.parse(source, sourcePath, sourceString);
      properties.putAll(parser.getOptions());
      sources.putAll(parser.getSources());
    } catch (InvalidConfigurationException | IOException e) {
      throw new IllegalArgumentException(
          "Error in resource " + resourceName + " relative to " + contextClass.getName(), e);
    }
  }

  /**
   * If the URI is part of a JAR file, we open the file system from the JAR. We only register/open a
   * new file system for the JAR file, if it was not already open before. The paths for this file
   * system are valid as long as the file system not closed. If anything fails, e.g., if the URI
   * does not point to a JAR file or the JAR is already open, we return <code>null</code>.
   *
   * <p>A JAR-based file system uses ZipFileSystem that can be opened and closed several times.
   *
   * @return the opened file system of the JAR file if it was not open before or <code>null</code>.
   */
  private static FileSystem getFileSystemForUriInJars(URI uri) throws IOException {
    if ("jar".equals(uri.getScheme())) {
      for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
        if (provider.getScheme().equalsIgnoreCase("jar")) {
          try {
            // try to register a new file system (provider) for the JAR.
            return provider.newFileSystem(uri, ImmutableMap.of());
          } catch (FileSystemAlreadyExistsException e) {
            // file system already exists: ignore it and return null after the loop
          }
        }
      }
    }
    return null; // default case: we do not need an extra file system
  }

  /**
   * Add a type converter for options with a certain type. This will enable the Configuration
   * instance to parse strings into values of the given type and inject them just as the base option
   * types.
   *
   * <p>As an alternative, the type of an option detail annotation ({@link OptionDetailAnnotation})
   * can be given. In this case, the type converter will be called for options annotated with this
   * type.
   *
   * <p>Previous type converters for the same type will be overwritten (this also works for types
   * usually handled by the Configuration class, however not for collection and array types).
   *
   * <p>The same converter may be used for several types.
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
    }

    converters.put(cls, converter);

    return this;
  }

  /**
   * Create a Configuration instance with the settings specified by method calls on this builder
   * instance.
   *
   * <p>This method resets the builder instance, so that after this method has returned it is
   * exactly in the same state as directly after instantiation.
   *
   * @throws InvalidConfigurationException if calling {@link
   *     TypeConverter#getInstanceForNewConfiguration(Configuration)} fails
   */
  @CheckReturnValue
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
        @Var String tempPrefix = oldConfig.prefix;
        if (tempPrefix.length() > 1) {
          // need to remove trailing dot because Configuration constructor will re-add it
          assert tempPrefix.charAt(tempPrefix.length() - 1) == '.';
          tempPrefix = tempPrefix.substring(0, tempPrefix.length() - 1);
        }
        newPrefix = tempPrefix;
      } else {
        newPrefix = "";
      }
    } else {
      newPrefix = prefix;
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

    ImmutableMap<Class<?>, TypeConverter> newConverters =
        buildNewConverters(
            newProperties, newSources, newPrefix, newUnusedProperties, newDeprecatedProperties);

    @SuppressWarnings("resource")
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

  private ImmutableMap<Class<?>, TypeConverter> buildNewConverters(
      ImmutableMap<String, String> newProperties,
      ImmutableMap<String, Path> newSources,
      String newPrefix,
      Set<String> newUnusedProperties,
      Set<String> newDeprecatedProperties)
      throws InvalidConfigurationException {
    // For converters, we first create a map of all previously-existing converters
    // and those explicitly set on this builder
    Map<Class<?>, TypeConverter> oldConverters =
        oldConfig != null ? oldConfig.converters : Configuration.DEFAULT_CONVERTERS;
    Map<Class<?>, TypeConverter> newConverters = Configuration.createConverterMap();
    newConverters.putAll(oldConverters);
    if (converters != null) {
      newConverters.putAll(converters);
    }

    // For the previously existing converters we need to call getInstanceForNewConfiguration,
    // so we create a temp config and iterate over all previously existing and still used converters
    @SuppressWarnings("resource")
    Configuration tempConfig =
        new Configuration(
            newProperties,
            newSources,
            newPrefix,
            ImmutableMap.copyOf(newConverters),
            newUnusedProperties,
            newDeprecatedProperties,
            oldConfig != null ? oldConfig.getUsedOptionsPrintStream() : null,
            oldConfig != null ? oldConfig.getLogger() : null);

    // This map serves as a cache: if a single TypeConverter is used for multiple types,
    // we call getInstanceForNewConfiguration only once and use the new instance several times.
    Map<TypeConverter, TypeConverter> adjustedConverters =
        new IdentityHashMap<>(oldConverters.size());
    for (Map.Entry<Class<?>, TypeConverter> oldConverter : oldConverters.entrySet()) {
      if (converters != null && converters.containsKey(oldConverter.getKey())) {
        continue; // ignore this one
      }
      assert newConverters.get(oldConverter.getKey()) == oldConverter.getValue();
      if (!adjustedConverters.containsKey(oldConverter.getValue())) {
        TypeConverter adjustedConverter =
            oldConverter.getValue().getInstanceForNewConfiguration(tempConfig);
        adjustedConverters.put(oldConverter.getValue(), adjustedConverter);
      }
      newConverters.put(oldConverter.getKey(), adjustedConverters.get(oldConverter.getValue()));
    }
    return ImmutableMap.copyOf(newConverters);
  }
}
