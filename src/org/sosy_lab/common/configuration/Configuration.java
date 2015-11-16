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

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.MapConstraint;
import com.google.common.collect.MapConstraints;
import com.google.common.collect.Multiset;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.configuration.converters.BaseTypeConverter;
import org.sosy_lab.common.configuration.converters.ClassTypeConverter;
import org.sosy_lab.common.configuration.converters.IntegerTypeConverter;
import org.sosy_lab.common.configuration.converters.TimeSpanTypeConverter;
import org.sosy_lab.common.configuration.converters.TypeConverter;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.NullLogManager;
import org.sosy_lab.common.log.TestLogManager;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Immutable wrapper around a {@link Properties} instance, providing some
 * useful access helper methods.
 */
@Options
public final class Configuration {

  /**
   * Signal for the processor that the deprecated-prefix feature is not used.
   */
  static final String NO_DEPRECATED_PREFIX = "<NO_DEPRECATION>";

  /**
   * Dummy value used for source paths that have no correspondence to the file system.
   */
  static final String NO_NAMED_SOURCE = "manually set";

  private static ConfigurationBuilderFactory builderFactory =
      new ConfigurationBuilderFactory() {
        @Override
        public ConfigurationBuilder getBuilder() {
          return new Builder();
        }
      };

  private static boolean secureMode = false;

  /**
   * Create a new Builder instance.
   */
  public static ConfigurationBuilder builder() {
    return getBuilderFactory().getBuilder();
  }

  /**
   * Sets the factory that will be used to create a {@link ConfigurationBuilder}
   * instance.
   *
   * @param factory The factory to use in the future for creating all builders.
   */
  public static void setBuilderFactory(ConfigurationBuilderFactory factory) {
    builderFactory = checkNotNull(factory);
  }

  /**
   * Returns the factory that is used to create {@link ConfigurationBuilder}
   * instances.
   *
   * @return The factory.
   */
  @VisibleForTesting
  static ConfigurationBuilderFactory getBuilderFactory() {
    return builderFactory;
  }

  /**
   * Enable a secure mode, i.e., allow only injection of configuration options
   * marked as secure.
   * Once enabled, this can not be disabled.
   */
  public static void enableSecureModeGlobally() {
    secureMode = true;
  }

  /**
   * Creates a configuration with all values set to default.
   */
  public static Configuration defaultConfiguration() {
    return new Configuration(
        ImmutableMap.<String, String>of(),
        ImmutableMap.<String, Path>of(),
        "",
        ImmutableMap.copyOf(DEFAULT_CONVERTERS),
        new HashSet<String>(0),
        new HashSet<String>(0),
        null);
  }

  /**
   * Creates a copy of a configuration with just the prefix set to a new value.
   */
  public static Configuration copyWithNewPrefix(Configuration oldConfig, String newPrefix) {
    Configuration newConfig =
        new Configuration(
            oldConfig.properties,
            oldConfig.sources,
            newPrefix,
            oldConfig.converters,
            oldConfig.unusedProperties,
            oldConfig.deprecatedProperties,
            oldConfig.logger);

    // instead of calling inject() set options manually
    // this avoids the "throws InvalidConfigurationException" in the signature
    newConfig.exportUsedOptions = oldConfig.exportUsedOptions;

    return newConfig;
  }

  @Option(name = "log.usedOptions.export", description = "all used options are printed")
  private boolean exportUsedOptions = false;

  /** Splitter to create string arrays. */
  private static final Splitter ARRAY_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  /** Map that stores which implementation we use for the collection classes. */
  static final Map<Class<? extends Iterable<?>>, Class<? extends Iterable<?>>> COLLECTIONS;

  static {
    ImmutableMap.Builder<Class<? extends Iterable<?>>, Class<? extends Iterable<?>>> builder =
        ImmutableMap.builder();

    putSafely(builder, EnumSet.class, EnumSet.class); // Caution: needs special casing

    putSafely(builder, Iterable.class, ImmutableList.class);
    putSafely(builder, Collection.class, ImmutableList.class);
    putSafely(builder, List.class, ImmutableList.class);
    putSafely(builder, Set.class, ImmutableSet.class);
    putSafely(builder, SortedSet.class, ImmutableSortedSet.class);
    putSafely(builder, Multiset.class, ImmutableMultiset.class);

    putSafely(builder, ImmutableCollection.class, ImmutableList.class);
    putSafely(builder, ImmutableList.class, ImmutableList.class);
    putSafely(builder, ImmutableSet.class, ImmutableSet.class);
    putSafely(builder, ImmutableSortedSet.class, ImmutableSortedSet.class);
    putSafely(builder, ImmutableMultiset.class, ImmutableMultiset.class);

    COLLECTIONS = builder.build();
  }

  // using this method to put key-value pairs into the builder ensures that
  // each implementation really implements the interface
  private static <T extends Iterable<?>> void putSafely(
      ImmutableMap.Builder<Class<? extends Iterable<?>>, Class<? extends Iterable<?>>> builder,
      Class<T> iface,
      Class<? extends T> impl) {
    assert !impl.isInterface();
    builder.put(iface, impl);
  }

  // Mutable static state on purpose here!
  // See below for explanation.
  static final Map<Class<?>, TypeConverter> DEFAULT_CONVERTERS =
      Collections.synchronizedMap(createConverterMap());

  static {
    DEFAULT_CONVERTERS.put(Class.class, new ClassTypeConverter());
    DEFAULT_CONVERTERS.put(IntegerOption.class, new IntegerTypeConverter());
    DEFAULT_CONVERTERS.put(TimeSpanOption.class, new TimeSpanTypeConverter());
  }

  /**
   * Get the map of registered default {@link TypeConverter}s.
   * These type converters are used whenever a new Configuration instance is
   * created, except when the {@link Builder#copyFrom(Configuration)} method is
   * used.
   *
   * The returned map is mutable and changes have immediate effect on this class!
   * Callers are free to add and remove mappings as they wish.
   * However, as this is static state, this will affect all other callers as well!
   * Thus, it should be used only with caution, for example to add default type
   * converters in a large project at startup.
   * It is discouraged to change this map, if the same effect can easily be
   * achieved using {@link Builder#addConverter(Class, TypeConverter)}.
   *
   * @return A reference to the map of type converters used by this class.
   */
  public static Map<Class<?>, TypeConverter> getDefaultConverters() {
    return DEFAULT_CONVERTERS;
  }

  /**
   * Use this method to create a new map for storing type converters.
   * In addition to being a normal HashMap, the returned map will have some
   * additional checks on the entries.
   * @return A new map.
   */
  static Map<Class<?>, TypeConverter> createConverterMap() {
    return MapConstraints.constrainedMap(
        new HashMap<Class<?>, TypeConverter>(),
        new MapConstraint<Class<?>, TypeConverter>() {

          @Override
          public void checkKeyValue(@Nonnull Class<?> cls, @Nonnull TypeConverter pValue) {
            checkNotNull(cls);
            checkNotNull(pValue);
            if (cls.isAnnotation() && !cls.isAnnotationPresent(OptionDetailAnnotation.class)) {
              throw new IllegalArgumentException(
                  "Can register type converters"
                      + " only for annotations which are option detail annotations");
            }
          }

          @Override
          public String toString() {
            return "valid type converter registration";
          }
        });
  }

  final ImmutableMap<String, String> properties;
  final ImmutableMap<String, Path> sources;

  final String prefix;

  final ImmutableMap<Class<?>, TypeConverter> converters;

  final Set<String> unusedProperties;
  final Set<String> deprecatedProperties;

  private LogManager logger = NullLogManager.getInstance();

  LogManager getLogger() {
    return logger;
  }

  /*
   * This constructor does not set the fields annotated with @Option
   * to avoid the exception in the signature,
   * the caller needs to make sure to set the values or inject them.
   */
  @SuppressWarnings("options")
  Configuration(
      ImmutableMap<String, String> pProperties,
      ImmutableMap<String, Path> pSources,
      String pPrefix,
      ImmutableMap<Class<?>, TypeConverter> pConverters,
      Set<String> pUnusedProperties,
      Set<String> pDeprecatedProperties,
      @Nullable LogManager pLogger) {

    checkNotNull(pProperties);
    checkNotNull(pSources);
    checkNotNull(pPrefix);

    assert pProperties.keySet().equals(pSources.keySet());
    properties = pProperties;
    sources = pSources;
    prefix = (pPrefix.isEmpty() ? "" : (pPrefix + "."));
    converters = checkNotNull(pConverters);
    unusedProperties = checkNotNull(pUnusedProperties);
    deprecatedProperties = checkNotNull(pDeprecatedProperties);
    logger = firstNonNull(pLogger, NullLogManager.getInstance());
  }

  public void enableLogging(LogManager pLogger) {
    checkState(logger.equals(NullLogManager.getInstance()), "Logging already enabled.");
    logger = checkNotNull(pLogger);
  }

  /**
   * Get the value of an option.
   * USE OF THIS METHOD IS NOT RECOMMENDED!
   *
   * If possible, use {@link Option} and {@link #inject(Object)}.
   * This provides type safety, documentation, logging etc.
   */
  @Nullable
  public String getProperty(String key) {
    checkNotNull(key);
    String result = properties.get(prefix + key);
    unusedProperties.remove(prefix + key);

    if (result == null && !prefix.isEmpty()) {
      result = properties.get(key);
      unusedProperties.remove(key);
    }
    return result;
  }

  /**
   * Check whether an option has a specified value.
   * USE OF THIS METHOD IS NOT RECOMMENDED!
   *
   * If possible, use {@link Option} and {@link #inject(Object)}.
   * This provides type safety, documentation, logging, default values, etc.
   */
  public boolean hasProperty(String key) {
    checkNotNull(key);
    return properties.containsKey(prefix + key) || properties.containsKey(key);
  }

  public Set<String> getUnusedProperties() {
    return Collections.unmodifiableSet(unusedProperties);
  }

  public Set<String> getDeprecatedProperties() {
    return Collections.unmodifiableSet(deprecatedProperties);
  }

  public String asPropertiesString() {
    String[] lines = new String[properties.size()];
    int i = 0;
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      lines[i++] = entry.getKey() + " = " + entry.getValue();
    }
    Arrays.sort(lines, String.CASE_INSENSITIVE_ORDER);
    StringBuffer sb = new StringBuffer();
    for (String line : lines) {
      sb.append(line);
      sb.append('\n');
    }
    return sb.toString();
  }

  /**
   * Inject the values of configuration options into an object.
   * The class of the object has to have a {@link Options} annotation, and each
   * field to set / method to call has to have a {@link Option} annotation.
   *
   * Supported types for configuration options:
   * - all primitive types and their wrapper types
   * - all enum types
   * - {@link String} and arrays of it
   * - {@link File} (the field {@link Option#type()} is required in this case!)
   * - {@link Class <Something>}
   * - {@link java.nio.Charset}
   * - {@link java.util.logging.Level}
   * - {@link java.util.regex.Pattern}
   * - arrays of the above types
   * - collection types {@link Iterable}, {@link Collection}, {@link List},
   *   {@link Set}, {@link SortedSet}, {@link Multiset}, and {@link EnumSet}
   *   of the above types
   *
   * For the collection types an immutable instance will be created and injected.
   * Their type parameter has to be one of the other supported types.
   * For collection types and arrays the values of the configuration option are
   * assumed to be comma separated.
   *
   * @param obj The object in which the configuration options should be injected.
   * @throws InvalidConfigurationException If the user specified configuration is wrong.
   */
  public void inject(Object obj) throws InvalidConfigurationException {
    inject(obj, obj.getClass());
  }

  /**
   * Call {@link #inject(Object, Class)} for this object with its actual class
   * and all super class that have an {@link Options} annotation.
   *
   * @param obj The object in which the configuration options should be injected.
   * @throws InvalidConfigurationException If the user specified configuration is wrong.
   */
  public void recursiveInject(Object obj) throws InvalidConfigurationException {
    Class<?> cls = obj.getClass();
    checkArgument(
        cls.isAnnotationPresent(Options.class),
        "Class %s must have @Options annotation.",
        cls.getName());

    do {
      if (cls.isAnnotationPresent(Options.class)) {
        inject(obj, cls);
      }

      cls = cls.getSuperclass();
    } while (cls != null);
  }

  /**
   * @see #inject(Object)
   *
   * Use this method if the calling class is likely to be sub-classed, so that
   * the options of the calling class get injected, not the options of the
   * dynamic class type of the object.
   *
   * @param cls The static class type of the object to inject.
   */
  public void inject(Object obj, Class<?> cls) throws InvalidConfigurationException {
    checkNotNull(obj);
    checkNotNull(cls);
    checkArgument(cls.isAssignableFrom(obj.getClass()));

    final Options options = cls.getAnnotation(Options.class);
    checkArgument(
        options != null,
        "Class %s must have @Options annotation. "
            + "If you used inject(Object), try inject(Object, Class) instead.",
        cls.getName());

    /*
     * Get all injectable members and override their final & private modifiers.
     * Do not use Field.setAccessible(Object[], boolean) to do so as it will not work
     * on the Google App Engine!
     */
    final Field[] fields = cls.getDeclaredFields();
    for (Field field : fields) {
      field.setAccessible(true);
    }

    final Method[] methods = cls.getDeclaredMethods();
    for (Method method : methods) {
      method.setAccessible(true);
    }

    try {
      for (final Field field : fields) {
        // ignore all non-option fields
        if (field.isAnnotationPresent(Option.class)) {
          setOptionValueForField(obj, field, options);
        }
      }

      for (final Method method : methods) {
        // ignore all non-option methods
        if (method.isAnnotationPresent(Option.class)) {
          setOptionValueForMethod(obj, method, options);
        }
      }

    } catch (IllegalAccessException e) {
      // setAccessible() succeeded but member is still not accessible (should not happen)
      throw new AssertionError(e);
    }
  }

  /** This method sets a new value to a field with an {@link Options}-annotation.
   * It takes the name and the new value of an option,
   * checks it for allowed values and injects it into the object.
   *
   * @param obj the object to be injected
   * @param field the field of the value to be injected
   * @param options options-annotation of the class of the object */
  private <T> void setOptionValueForField(
      final Object obj, final Field field, final Options options)
      throws InvalidConfigurationException, IllegalAccessException {

    // check validity of field
    if (Modifier.isStatic(field.getModifiers())) {
      throw new UnsupportedOperationException("@Option is not allowed on static members");
    }
    if (Modifier.isFinal(field.getModifiers())) {
      throw new UnsupportedOperationException(
          "@Option is not allowed on final fields"
              + " because Java doesn't guarantee visibility of new value");
    }

    // try to read default value
    Object defaultValue = null;
    try {
      defaultValue = field.get(obj);
    } catch (IllegalArgumentException e) {
      assert false : "Type checks above were not successful apparently.";
    }

    @SuppressWarnings("unchecked")
    final T typedDefaultValue = (T) defaultValue;

    // determine type of option
    @SuppressWarnings("unchecked")
    final Class<T> type = (Class<T>) field.getType();
    final Type genericType = field.getGenericType();

    // get value
    final Option option = field.getAnnotation(Option.class);
    final String name = getOptionName(options, field, option);
    final Object value =
        getValue(options, field, typedDefaultValue, type, genericType, option, field);

    // options which were not changed need not to be set
    if (value == defaultValue) {
      logger.log(
          Level.CONFIG,
          "Option:",
          name,
          "Class:",
          field.getDeclaringClass().getName(),
          "field:",
          field.getName(),
          "value: <DEFAULT>");
      return;
    }

    logger.log(
        Level.CONFIG,
        "Option:",
        name,
        "Class:",
        field.getDeclaringClass().getName(),
        "field:",
        field.getName(),
        "value:",
        value);

    // set value to field
    try {
      field.set(obj, value);
    } catch (IllegalArgumentException e) {
      assert false : "Type checks above were not successful apparently.";
    }
  }

  /** This method sets a new value to a method with an {@link Options}-annotation.
   * It takes the name and the new value of an option,
   * checks it for allowed values and injects it into the object.
   *
   * @param obj the object to be injected
   * @param method the method of the value to be injected
   * @param options options-annotation of the class of the object */
  private void setOptionValueForMethod(final Object obj, final Method method, final Options options)
      throws InvalidConfigurationException, IllegalAccessException {

    // check validity of method
    if (Modifier.isStatic(method.getModifiers())) {
      throw new UnsupportedOperationException("@Option is not allowed on static members");
    }

    String exception =
        Classes.verifyDeclaredExceptions(method, InvalidConfigurationException.class);
    if (exception != null) {
      throw new UnsupportedOperationException("Method with @Option may not throw " + exception);
    }

    // determine type of option
    final Class<?>[] parameters = method.getParameterTypes();
    if (parameters.length != 1) {
      throw new UnsupportedOperationException(
          "Method with @Option must have exactly one parameter!");
    }
    final Class<?> type = parameters[0];
    final Type genericType = method.getGenericParameterTypes()[0];

    // get value
    final Option option = method.getAnnotation(Option.class);
    final String name = getOptionName(options, method, option);
    final Object value = getValue(options, method, null, type, genericType, option, method);

    logger.logf(
        Level.CONFIG,
        "Option: %s Class: %s method: %s value: %s",
        name,
        method.getDeclaringClass().getName(),
        method.getName(),
        value);

    // set value to field
    try {
      method.invoke(obj, value);
    } catch (IllegalArgumentException e) {
      assert false : "Type checks above were not successful apparently.";
    } catch (InvocationTargetException e) {
      // ITEs always have a wrapped exception which is the real one thrown by
      // the invoked method. We want to handle this exception.
      final Throwable t = e.getCause();

      if (t instanceof IllegalArgumentException) {
        // this is an expected exception if the value is wrong,
        // so create a nice message for the user
        throw new InvalidConfigurationException(
            String.format(
                "Invalid value in configuration file: \"%s = %s\"%s",
                name,
                value,
                (t.getMessage() != null ? " (" + t.getMessage() + ")" : "")),
            t);
      }

      Throwables.propagateIfPossible(t, InvalidConfigurationException.class);
      throw new UnexpectedCheckedException("configuration injection in method " + method, t);
    }
  }

  static String getOptionName(final Options options, final Member member, final Option option) {
    return getOptionName(options, member, option, false);
  }

  /** This function return the name of an {@link Option}.
   * If no option name is defined, the name of the member is returned.
   * If a prefix is defined, it is added in front of the name.
   *
   * @param options the @Options annotation of the class, that contains the member
   * @param member member with @Option annotation
   * @param option the @Option annotation
   * @param isDeprecated flag specifying whether the deprecated prefix should be
   *                     used.
   */
  private static String getOptionName(
      final Options options, final Member member, final Option option, boolean isDeprecated) {
    String name = option.name();
    if (name.isEmpty()) {
      name = member.getName();
    }
    String optsPrefix = !isDeprecated ? options.prefix() : options.deprecatedPrefix();
    if (!optsPrefix.isEmpty()) {
      optsPrefix += ".";
    }
    return optsPrefix + name;
  }

  /**
   * This method gets the value which needs to be assigned to the option.
   *
   * @param options Options annotation.
   * @param method Member the annotation is attached to.
   * @param defaultValue The default value (may be null).
   * @param type The type of the option.
   * @param genericType The type of the option.
   * @param option The annotation of the option.
   * @param member The member that declares the option.
   * @return The value to assign (may be null).
   *
   * @throws UnsupportedOperationException If the declaration of the option
   * in the source code is invalid.
   * @throws InvalidConfigurationException If the user specified an invalid value for the option.
   */
  @Nullable
  private <T> Object getValue(
      final Options options,
      final Member method,
      @Nullable final T defaultValue,
      final Class<T> type,
      final Type genericType,
      final Option option,
      final AnnotatedElement member)
      throws UnsupportedOperationException, InvalidConfigurationException {

    final String optionName = getOptionName(options, method, option);
    String valueStr = getValueString(optionName, option, type.isEnum());
    final Annotation secondaryOption = getSecondaryAnnotation(member);

    final Object value;
    if (!options.deprecatedPrefix().equals(NO_DEPRECATED_PREFIX)) {
      String optionDeprecatedName = getOptionName(options, method, option, true);
      String deprecatedValueStr = getValueString(optionDeprecatedName, option, type.isEnum());
      if (deprecatedValueStr != null && !deprecatedValueStr.equals(valueStr)) {
        if (valueStr == null) {
          valueStr = deprecatedValueStr;
          logger.logf(
              Level.WARNING,
              "Using deprecated name for option '%s'%s, "
                  + "please update your config to use the option name '%s' instead.",
              optionDeprecatedName,
              getOptionSourceForLogging(optionDeprecatedName),
              optionName);
        } else {
          logger.logf(
              Level.WARNING,
              "Option '%s'%s is set to a different value "
                  + "than its deprecated previous name '%s'%s, "
                  + "using the value '%s' of the former and ignoring the latter.",
              optionName,
              getOptionSourceForLogging(optionName),
              optionDeprecatedName,
              getOptionSourceForLogging(optionDeprecatedName),
              valueStr);
        }
      }
    }

    if (valueStr != null) {
      // option was specified

      if (secureMode && !option.secure()) {
        throw new InvalidConfigurationException(
            "Configuration option "
                + optionName
                + " was specified, but is not allowed in secure mode.");
      }

      value = convertValue(optionName, valueStr, type, genericType, secondaryOption);

      if (member.isAnnotationPresent(Deprecated.class)) {
        deprecatedProperties.add(optionName);
      }

    } else {
      if (option.required()) {
        throw new InvalidConfigurationException(
            "Required configuration option " + optionName + " is missing.");
      }

      value = convertDefaultValue(optionName, defaultValue, type, genericType, secondaryOption);
    }

    if (exportUsedOptions) {
      printOptionInfos(member, optionName, valueStr, defaultValue);
    }
    return value;
  }

  /**
   * Return a string describing the source of an option suitable for logging
   * (best-effort, may return an empty string).
   */
  private String getOptionSourceForLogging(String optionDeprecatedName) {
    if (sources.containsKey(optionDeprecatedName)) {
      String source = sources.get(optionDeprecatedName).toString();
      if (!source.isEmpty() && !source.equals(NO_NAMED_SOURCE)) {
        return " in file " + source;
      }
    }
    return "";
  }

  /**
   * This function takes the new value of an {@link Option}
   * in the property, checks it (allowed values, regexp) and returns it.
   *
   * @param name name of the value
   * @param option the option-annotation of the field of the value
   * @param alwaysUppercase how to write the value
   */
  @Nullable
  private String getValueString(
      final String name, final Option option, final boolean alwaysUppercase)
      throws InvalidConfigurationException {

    // get value in String representation
    String valueStr = trimToNull(getProperty(name));

    if (valueStr == null) {
      return null;
    }

    if (alwaysUppercase || option.toUppercase()) {
      valueStr = valueStr.toUpperCase();
    }

    // check if it is included in the allowed values list
    final String[] allowedValues = option.values();
    if (allowedValues.length > 0 && !java.util.Arrays.asList(allowedValues).contains(valueStr)) {
      throw new InvalidConfigurationException(
          String.format(
              "Invalid value in configuration file: \"%s = %s\" (not listed as allowed value)",
              name,
              valueStr));
    }

    // check if it matches the specification regexp
    final String regexp = option.regexp();
    if (!regexp.isEmpty() && !valueStr.matches(regexp)) {
      throw new InvalidConfigurationException(
          String.format(
              "Invalid value in configuration file: \"%s = %s\" (does not match RegExp \"%s\").",
              name,
              valueStr,
              regexp));
    }

    return valueStr;
  }

  /**
   * Find any annotation which itself is annotated with {@link OptionDetailAnnotation}
   * on a member.
   */
  private Annotation getSecondaryAnnotation(AnnotatedElement element) {
    Annotation result = null;
    for (Annotation a : element.getDeclaredAnnotations()) {
      if (a.annotationType().isAnnotationPresent(OptionDetailAnnotation.class)) {
        if (result != null) {
          throw new UnsupportedOperationException(
              "Both " + result + " and " + a + " are present at " + element.toString());
        }
        result = a;
      }
    }
    return result;
  }

  /**
   * Check whether a given annotation (which itself has to be annotated with
   * {@link OptionDetailAnnotation}!) is applicable to an option of a given type.
   *
   * @throws UnsupportedOperationException If the annotation is not applicable.
   */
  private void checkApplicability(Annotation annotation, final Class<?> optionType)
      throws UnsupportedOperationException {
    if (annotation == null) {
      return;
    }

    List<Class<?>> applicableTypes =
        Arrays.asList(
            annotation.annotationType().getAnnotation(OptionDetailAnnotation.class).applicableTo());

    if (!applicableTypes.contains(optionType)) {
      throw new UnsupportedOperationException(
          "Annotation "
              + annotation
              + " is not applicable for options of type "
              + optionType.getCanonicalName());
    }
  }

  private void printOptionInfos(
      final AnnotatedElement element,
      final String name,
      final String valueStr,
      final Object defaultValue) {

    final StringBuilder optionInfo = new StringBuilder();
    optionInfo.append(OptionCollector.getOptionDescription(element));
    optionInfo.append(name + "\n");

    if (defaultValue != null) {
      String defaultStr;

      if (defaultValue instanceof Object[]) {
        defaultStr = Arrays.deepToString((Object[]) defaultValue);
      } else {
        defaultStr = defaultValue.toString();
      }

      optionInfo.append("    default value:  ").append(defaultStr).append("\n");
    }

    if (valueStr != null) {
      optionInfo.append("--> used value:     " + valueStr + "\n");
    }

    System.out.println(optionInfo.toString());
  }

  /**
   * This function takes a value (String) and a type and
   * returns an Object of this type with the value as content.
   *
   * @param optionName name of option, only for error handling
   * @param valueStr new value of the option
   * @param pType type of the object
   * @param genericType type of the object
   * @param secondaryOption the optional second annotation of the option
   */
  private <T> Object convertValue(
      final String optionName,
      final String valueStr,
      final Class<?> pType,
      final Type genericType,
      final Annotation secondaryOption)
      throws UnsupportedOperationException, InvalidConfigurationException {
    // convert value to correct type

    Class<?> collectionClass = COLLECTIONS.get(pType);

    if (collectionClass == null && !pType.isArray()) {
      Class<?> type = Primitives.wrap(pType);

      // single value, easy case
      checkApplicability(secondaryOption, type);

      return convertSingleValue(optionName, valueStr, type, genericType, secondaryOption);
    }

    // first get the real type of a single value (i.e., String[] => String)
    Class<?> componentType;
    Type componentGenericType = null;
    if (pType.isArray()) {
      componentType = pType.getComponentType();
    } else {
      componentType = Classes.getComponentRawType(genericType);
      componentGenericType = Classes.getComponentType(genericType);
    }

    componentType = Primitives.wrap(componentType);

    checkApplicability(secondaryOption, componentType);

    List<?> values =
        convertMultipleValues(
            optionName, valueStr, componentType, componentGenericType, secondaryOption);

    if (pType.isArray()) {

      @SuppressWarnings("unchecked")
      Class<T> arrayComponentType = (Class<T>) componentType;
      T[] result = ObjectArrays.newArray(arrayComponentType, values.size());

      return values.toArray(result);
    }
    assert collectionClass != null;

    if (collectionClass == EnumSet.class) {
      assert componentType.isEnum() : "";
      return createEnumSetUnchecked(componentType, values);

    } else if (componentType.isEnum()
        && (collectionClass == Set.class || collectionClass == ImmutableSet.class)) {
      // There is a specialized ImmutableSet for enums in Guava that is more efficient.
      // We use it if we can.
      return BaseTypeConverter.invokeStaticMethod(
          Sets.class, "immutableEnumSet", Iterable.class, values, optionName);

    } else {
      // we now that it's a Collection<componentType> / Set<? extends componentType> etc.,
      // so we can safely assign to it

      // invoke ImmutableSet.copyOf(Iterable) etc.
      return BaseTypeConverter.invokeStaticMethod(
          collectionClass, "copyOf", Iterable.class, values, optionName);
    }
  }

  /**
   * This function takes a value (String) and a type and
   * returns an Object of this type with the value as content.
   *
   * The type may not be an array or a collection type, and the value may only
   * be a single value (not multiple values).
   *
   * @param optionName name of option, only for error handling
   * @param valueStr new value of the option
   * @param type type of the object
   * @param genericType type of the object
   * @param secondaryOption the optional second annotation of the option (needs to fit to the type)
   */
  private Object convertSingleValue(
      final String optionName,
      final String valueStr,
      final Class<?> type,
      final Type genericType,
      final Annotation secondaryOption)
      throws InvalidConfigurationException {

    // try to find a type converter, either for the type of the annotation
    // or for the type of the field
    TypeConverter converter = getConverter(type, secondaryOption);
    return converter.convert(
        optionName,
        valueStr,
        type,
        genericType,
        secondaryOption,
        sources.get(optionName),
        MoreObjects.firstNonNull(logger, TestLogManager.getInstance()));
  }

  /**
   * Convert a String which possibly contains multiple values into a list of objects
   * of the correct type.
   *
   * @param optionName name of option, only for error handling
   * @param valueStr new value of the option
   * @param type type of each object
   * @param genericType type of each object
   * @param secondaryOption the optional second annotation of the option
   * @return
   * @throws InvalidConfigurationException
   */
  private List<?> convertMultipleValues(
      final String optionName,
      final String valueStr,
      final Class<?> type,
      final Type genericType,
      final Annotation secondaryOption)
      throws InvalidConfigurationException {

    Iterable<String> values = ARRAY_SPLITTER.split(valueStr);

    List<Object> result = new ArrayList<>();

    for (String item : values) {
      result.add(convertSingleValue(optionName, item, type, genericType, secondaryOption));
    }

    return result;
  }

  private <T> Object convertDefaultValue(
      final String optionName,
      final T defaultValue,
      final Class<T> type,
      final Type genericType,
      final Annotation secondaryOption)
      throws InvalidConfigurationException {

    Class<?> innerType;
    if (type.isArray()) {
      innerType = type.getComponentType();
    } else if (COLLECTIONS.containsKey(type)) {
      innerType = Classes.getComponentRawType(genericType);
    } else {
      innerType = type;
    }

    innerType = Primitives.wrap(innerType);

    checkApplicability(secondaryOption, innerType);

    if (type == innerType) {
      // If its not a collection, we try to pass the default value to the
      // type converter if there is any.
      // TODO: Also pass default values inside a collection.

      TypeConverter converter = getConverter(type, secondaryOption);
      return converter.convertDefaultValue(
          optionName, defaultValue, type, genericType, secondaryOption);
    }

    return defaultValue;
  }

  /**
   * Find a type converter for an option.
   * @return A type converter.
   */
  private TypeConverter getConverter(final Class<?> type, final Annotation secondaryOption) {
    TypeConverter converter = null;
    if (secondaryOption != null) {
      converter = converters.get(secondaryOption.annotationType());
    }
    if (converter == null) {
      converter = converters.get(type);
    }
    if (converter == null) {
      converter = BaseTypeConverter.INSTANCE;
    }
    return converter;
  }

  /**
   * A null-safe combination of {@link String#trim()} and {@link Strings#emptyToNull(String)}.
   */
  @Nullable
  private static String trimToNull(String s) {
    if (s == null) {
      return null;
    }
    return Strings.emptyToNull(s.trim());
  }

  @SuppressWarnings("unchecked")
  private static <T extends Enum<T>> EnumSet<?> createEnumSetUnchecked(
      Class<?> enumType, Collection<?> values) {
    EnumSet<T> result = EnumSet.noneOf((Class<T>) enumType);
    result.addAll((Collection<? extends T>) values);
    return result;
  }

  @Override
  public String toString() {
    return "Configuration"
        + (!prefix.isEmpty() ? " with prefix " + prefix : "")
        + ": ["
        + Joiner.on(", ").withKeyValueSeparator("=").join(properties)
        + "]";
  }
}
