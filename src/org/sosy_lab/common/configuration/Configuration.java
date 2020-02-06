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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multiset;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.errorprone.annotations.Var;
import java.io.File;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.configuration.converters.BaseTypeConverter;
import org.sosy_lab.common.configuration.converters.ClassTypeConverter;
import org.sosy_lab.common.configuration.converters.IntegerTypeConverter;
import org.sosy_lab.common.configuration.converters.TimeSpanTypeConverter;
import org.sosy_lab.common.configuration.converters.TypeConverter;
import org.sosy_lab.common.log.LogManager;

/** Immutable wrapper around a map with properties, providing useful access helper methods. */
public final class Configuration {

  /** Signal for the processor that the deprecated-prefix feature is not used. */
  static final String NO_DEPRECATED_PREFIX = "<NO_DEPRECATION>";

  /** Dummy value used for source paths that have no correspondence to the file system. */
  static final String NO_NAMED_SOURCE = "manually set";

  private static boolean secureMode = false;

  /** Create a new Builder instance. */
  public static ConfigurationBuilder builder() {
    return new ConfigurationBuilder();
  }

  /**
   * Enable a secure mode, i.e., allow only injection of configuration options marked as secure.
   * Once enabled, this can not be disabled.
   */
  public static void enableSecureModeGlobally() {
    secureMode = true;
  }

  /** Creates a configuration with all values set to default. */
  public static Configuration defaultConfiguration() {
    // We do not call TypeConverter.getInstanceForNewConfiguration
    // because the new Configuration instance has no values, which makes injection pointless.
    return new Configuration(
        ImmutableMap.of(),
        ImmutableMap.of(),
        "",
        ImmutableMap.copyOf(DEFAULT_CONVERTERS),
        new HashSet<>(0),
        new HashSet<>(0),
        null,
        null);
  }

  /** Creates a copy of a configuration with just the prefix set to a new value. */
  public static Configuration copyWithNewPrefix(Configuration oldConfig, String newPrefix) {
    // We do not call TypeConverter.getInstanceForNewConfiguration
    // because the new Configuration instance has exactly the same option values.
    return new Configuration(
        oldConfig.properties,
        oldConfig.sources,
        newPrefix,
        oldConfig.converters,
        oldConfig.unusedProperties,
        oldConfig.deprecatedProperties,
        oldConfig.printUsedOptions,
        oldConfig.logger);
  }

  /** Splitter to create string arrays. */
  private static final Splitter ARRAY_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  /** Splitter that separates the option value and the annotation. */
  private static final Splitter ANNOTATION_VALUE_SPLITTER =
      Splitter.on("::").limit(2).trimResults();

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
    DEFAULT_CONVERTERS.put(ClassOption.class, new ClassTypeConverter());
    DEFAULT_CONVERTERS.put(IntegerOption.class, new IntegerTypeConverter());
    DEFAULT_CONVERTERS.put(TimeSpanOption.class, new TimeSpanTypeConverter());
  }

  /**
   * Get the map of registered default {@link TypeConverter}s. These type converters are used
   * whenever a new Configuration instance is created, except when the {@link
   * ConfigurationBuilder#copyFrom(Configuration)} method is used.
   *
   * <p>For all instances in this map the method {@link
   * TypeConverter#getInstanceForNewConfiguration(Configuration)} will be called before the type
   * converter is actually added to a {@link Configuration} instance.
   *
   * <p>The returned map is mutable and changes have immediate effect on this class! Callers are
   * free to add and remove mappings as they wish. However, as this is static state, this will
   * affect all other callers as well! Thus, it should be used only with caution, for example to add
   * default type converters in a large project at startup. It is discouraged to change this map, if
   * the same effect can easily be achieved using {@link ConfigurationBuilder#addConverter(Class,
   * TypeConverter)}.
   *
   * @return A reference to the map of type converters used by this class.
   */
  public static Map<Class<?>, TypeConverter> getDefaultConverters() {
    return DEFAULT_CONVERTERS;
  }

  /**
   * Use this method to create a new map for storing type converters. In addition to being a normal
   * HashMap, the returned map will have some additional checks on the entries.
   *
   * @return A new map.
   */
  static Map<Class<?>, TypeConverter> createConverterMap() {
    return new ForwardingMap<>() {

      private final Map<Class<?>, TypeConverter> delegate = new HashMap<>();

      @Override
      protected Map<Class<?>, TypeConverter> delegate() {
        return delegate;
      }

      private void check(Class<?> cls, TypeConverter pValue) {
        checkNotNull(cls);
        checkNotNull(pValue);
        checkArgument(
            !cls.isAnnotation() || cls.isAnnotationPresent(OptionDetailAnnotation.class),
            "Can register type converters"
                + " only for annotations which are option detail annotations");
      }

      @Override
      public TypeConverter put(Class<?> cls, TypeConverter pValue) {
        check(cls, pValue);
        return super.put(cls, pValue);
      }

      @Override
      public void putAll(Map<? extends Class<?>, ? extends TypeConverter> pMap) {
        pMap.forEach(this::check);
        super.putAll(pMap);
      }

      @Override
      public Set<java.util.Map.Entry<Class<?>, TypeConverter>> entrySet() {
        return Collections.unmodifiableSet(super.entrySet());
      }
    };
  }

  final ImmutableMap<String, String> properties;
  final ImmutableMap<String, Path> sources;

  final String prefix;

  final ImmutableMap<Class<?>, TypeConverter> converters;

  final Set<String> unusedProperties;
  final Set<String> deprecatedProperties;

  private @Nullable PrintStream printUsedOptions;

  PrintStream getUsedOptionsPrintStream() {
    return printUsedOptions;
  }

  private LogManager logger = LogManager.createNullLogManager();

  LogManager getLogger() {
    return logger;
  }

  /*
   * This constructor does not set the fields annotated with @Option
   * to avoid the exception in the signature,
   * the caller needs to make sure to set the values or inject them.
   */
  @SuppressWarnings("checkstyle:parameternumber")
  Configuration(
      ImmutableMap<String, String> pProperties,
      ImmutableMap<String, Path> pSources,
      String pPrefix,
      ImmutableMap<Class<?>, TypeConverter> pConverters,
      Set<String> pUnusedProperties,
      Set<String> pDeprecatedProperties,
      @Nullable PrintStream pPrintUsedOptions,
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
    printUsedOptions = pPrintUsedOptions;
    logger = Objects.requireNonNullElse(pLogger, LogManager.createNullLogManager());
  }

  public void enableLogging(LogManager pLogger) {
    checkState(logger.equals(LogManager.createNullLogManager()), "Logging already enabled.");
    logger = checkNotNull(pLogger);
  }

  /**
   * Let this instance write human-readable information about every option that is used to the given
   * stream.
   */
  public void dumpUsedOptionsTo(PrintStream out) {
    checkNotNull(out);
    checkState(printUsedOptions == null);
    printUsedOptions = out;
  }

  /**
   * Get the value of an option. USE OF THIS METHOD IS NOT RECOMMENDED!
   *
   * <p>Use configuration injection with {@link Option} and {@link #inject(Object)} instead. This
   * provides type safety, documentation, logging etc.
   */
  @Nullable
  @Deprecated
  public String getProperty(String key) {
    checkNotNull(key);
    @Var String result = properties.get(prefix + key);
    unusedProperties.remove(prefix + key);

    if (result == null && !prefix.isEmpty()) {
      result = properties.get(key);
      unusedProperties.remove(key);
    }
    return result;
  }

  /**
   * Check whether an option has a specified value. USE OF THIS METHOD IS NOT RECOMMENDED!
   *
   * <p>Use configuration injection with {@link Option} and {@link #inject(Object)} instead. This
   * provides type safety, documentation, logging, default values, etc.
   */
  @Deprecated
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
    return Collections3.zipMapEntries(properties, (key, value) -> key + " = " + value + "\n")
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .collect(Collectors.joining());
  }

  /**
   * Inject the values of configuration options into an object. The class of the object has to have
   * a {@link Options} annotation, and each field to set / method to call has to have a {@link
   * Option} annotation.
   *
   * <p>Supported types for configuration options:
   *
   * <ul>
   *   <li>all primitive types and their wrapper types
   *   <li>all enum types
   *   <li>{@link String} and arrays of it
   *   <li>{@link File} and {@link Path} (the field {@link FileOption#value()} is required in this
   *       case!)
   *   <li>{@code Class<Something>}
   *   <li>{@link java.nio.charset.Charset}
   *   <li>{@link java.util.logging.Level}
   *   <li>{@link java.util.regex.Pattern}
   *   <li>arbitrary factory interfaces as supported by {@link Classes#createFactory(TypeToken,
   *       Class)}
   *   <li>arrays of the above types
   *   <li>{@link AnnotatedValue} with types of the above as value type (users can specify an
   *       annotation string after a "::" separator)
   *   <li>collection types {@link Iterable}, {@link Collection}, {@link List}, {@link Set}, {@link
   *       SortedSet}, {@link Multiset}, and {@link EnumSet} of the above types
   * </ul>
   *
   * <p>For the collection types an immutable instance will be created and injected. Their type
   * parameter has to be one of the other supported types. For collection types and arrays the
   * values of the configuration option are assumed to be comma separated.
   *
   * @param obj The object in which the configuration options should be injected.
   * @throws InvalidConfigurationException If the user specified configuration is wrong.
   */
  public void inject(Object obj) throws InvalidConfigurationException {
    inject(obj, obj.getClass());
  }

  /**
   * Use this method if the calling class is likely to be sub-classed, so that the options of the
   * calling class get injected, not the options of the dynamic class type of the object.
   *
   * @see #inject(Object)
   * @param cls The static class type of the object to inject.
   */
  public void inject(Object obj, Class<?> cls) throws InvalidConfigurationException {
    inject(obj, cls, obj);
  }

  /**
   * Same as {@link #inject(Object, Class)}, but if this Configuration instance does not contain a
   * value for a requested configuration option, use the value that is set in the given {@code
   * defaultsInstance} instead of the value that is set as default in the to-be-injected object.
   * This can be used to create a copy of an object but with some options changed according to this
   * Configuration instance.
   *
   * <p>Note that this only works for configuration options that are specified as fields, not for
   * those specified as setters.
   *
   * @param obj The to-be-injected instance.
   * @param cls The static class type of the object to inject.
   * @param defaultsObject The instance from which default values should be read.
   */
  public <T> void injectWithDefaults(T obj, Class<T> cls, T defaultsObject)
      throws InvalidConfigurationException {
    inject(obj, cls, defaultsObject);
  }

  private void inject(Object obj, Class<?> cls, Object defaultsObject)
      throws InvalidConfigurationException {
    checkNotNull(obj);
    checkNotNull(cls);
    checkNotNull(defaultsObject);
    checkArgument(cls.isAssignableFrom(obj.getClass()));

    Options options = cls.getAnnotation(Options.class);
    checkArgument(
        options != null,
        "Class %s must have @Options annotation. "
            + "If you used inject(Object), try inject(Object, Class) instead.",
        cls.getName());

    Field[] fields = cls.getDeclaredFields();
    AccessibleObject.setAccessible(fields, /*flag=*/ true);

    Method[] methods = cls.getDeclaredMethods();
    AccessibleObject.setAccessible(methods, /*flag=*/ true);

    try {
      for (Field field : fields) {
        // ignore all non-option fields
        if (field.isAnnotationPresent(Option.class)) {
          setOptionValueForField(obj, field, options, defaultsObject);
        }
      }

      for (Method method : methods) {
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

  /**
   * Call {@link #inject(Object, Class)} for this object with its actual class and all super class
   * that have an {@link Options} annotation.
   *
   * @param obj The object in which the configuration options should be injected.
   * @throws InvalidConfigurationException If the user specified configuration is wrong.
   */
  public void recursiveInject(Object obj) throws InvalidConfigurationException {
    @Var Class<?> cls = obj.getClass();
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
   * This method sets a new value to a field with an {@link Options}-annotation. It takes the name
   * and the new value of an option, checks it for allowed values and injects it into the object.
   *
   * @param obj the object to be injected
   * @param field the field of the value to be injected
   * @param options options-annotation of the class of the object
   * @param defaultsObject the object from which the default values of options should be read
   */
  private <T> void setOptionValueForField(
      Object obj, Field field, Options options, Object defaultsObject)
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
    @Var Object defaultValue = null;
    try {
      defaultValue = field.get(defaultsObject);
    } catch (IllegalArgumentException e) {
      throw new AssertionError("Type checks above were not successful apparently.", e);
    }

    @SuppressWarnings("unchecked")
    T typedDefaultValue = (T) defaultValue;

    // determine type of option
    @SuppressWarnings("unchecked")
    TypeToken<T> type = (TypeToken<T>) TypeToken.of(field.getGenericType());

    // get value
    Option option = field.getAnnotation(Option.class);
    String name = getOptionName(options, field, option);
    Object value =
        getValue(
            options,
            field,
            typedDefaultValue,
            type,
            option,
            field,
            /*defaultIsFromOtherInstance=*/ obj != defaultsObject);

    // options which were not changed need not to be set
    if (value == defaultValue && obj == defaultsObject) {
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
      throw new AssertionError("Type checks above were not successful apparently.", e);
    }
  }

  /**
   * This method sets a new value to a method with an {@link Options}-annotation. It takes the name
   * and the new value of an option, checks it for allowed values and injects it into the object.
   *
   * @param obj the object to be injected
   * @param method the method of the value to be injected
   * @param options options-annotation of the class of the object
   */
  private void setOptionValueForMethod(Object obj, Method method, Options options)
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
    Type[] parameters = method.getGenericParameterTypes();
    if (parameters.length != 1) {
      throw new UnsupportedOperationException(
          "Method with @Option must have exactly one parameter!");
    }
    TypeToken<?> type = TypeToken.of(parameters[0]);

    // get value
    Option option = method.getAnnotation(Option.class);
    String name = getOptionName(options, method, option);
    Object value =
        getValue(
            options, method, null, type, option, method, /*defaultIsFromOtherInstance=*/ false);

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
      throw new AssertionError("Type checks above were not successful apparently.", e);
    } catch (InvocationTargetException e) {
      // ITEs always have a wrapped exception which is the real one thrown by
      // the invoked method. We want to handle this exception.
      Throwable t = e.getCause();

      if (t instanceof IllegalArgumentException) {
        // this is an expected exception if the value is wrong,
        // so create a nice message for the user
        throw new InvalidConfigurationException(
            String.format(
                "Invalid value in configuration file: \"%s = %s\"%s",
                name, value, (t.getMessage() != null ? " (" + t.getMessage() + ")" : "")),
            t);
      }

      Throwables.propagateIfPossible(t, InvalidConfigurationException.class);
      throw new UnexpectedCheckedException("configuration injection in method " + method, t);
    }
  }

  static String getOptionName(Options options, Member member, Option option) {
    return getOptionName(options, member, option, /* isDeprecated= */ false);
  }

  /**
   * This function return the name of an {@link Option}. If no option name is defined, the name of
   * the member is returned. If a prefix is defined, it is added in front of the name.
   *
   * @param options the @Options annotation of the class, that contains the member
   * @param member member with @Option annotation
   * @param option the @Option annotation
   * @param isDeprecated flag specifying whether the deprecated prefix should be used.
   */
  private static String getOptionName(
      Options options, Member member, Option option, boolean isDeprecated) {
    @Var String name = "";
    if (isDeprecated) {
      name = option.deprecatedName();
      if (name.isEmpty()) {
        name = option.name();
      }
    } else {
      name = option.name();
    }
    if (name.isEmpty()) {
      name = member.getName();
    }

    @Var String optsPrefix;
    if (isDeprecated) {
      optsPrefix = options.deprecatedPrefix();
      if (optsPrefix.isEmpty()) {
        optsPrefix = options.prefix();
      }
    } else {
      optsPrefix = options.prefix();
    }
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
   * @param option The annotation of the option.
   * @param member The member that declares the option.
   * @return The value to assign (may be null).
   * @throws UnsupportedOperationException If the declaration of the option in the source code is
   *     invalid.
   * @throws InvalidConfigurationException If the user specified an invalid value for the option.
   */
  @Nullable
  private <T> Object getValue(
      Options options,
      Member method,
      @Nullable T defaultValue,
      TypeToken<T> type,
      Option option,
      AnnotatedElement member,
      boolean defaultIsFromOtherInstance)
      throws InvalidConfigurationException {

    boolean isEnum = type.getRawType().isEnum();
    String optionName = getOptionName(options, method, option);
    @Var String valueStr = getValueString(optionName, option, isEnum);
    Annotation secondaryOption = getSecondaryAnnotation(member);

    Object value;
    if (!options.deprecatedPrefix().equals(NO_DEPRECATED_PREFIX)) {
      String optionDeprecatedName =
          getOptionName(options, method, option, /* isDeprecated= */ true);
      String deprecatedValueStr = getValueString(optionDeprecatedName, option, isEnum);
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

      value = convertValue(optionName, valueStr, type, secondaryOption);

      if (member.isAnnotationPresent(Deprecated.class)) {
        deprecatedProperties.add(optionName);
      }

    } else {
      if (option.required()) {
        throw new InvalidConfigurationException(
            "Required configuration option " + optionName + " is missing.");
      }

      value =
          convertDefaultValue(
              optionName, defaultValue, type, secondaryOption, defaultIsFromOtherInstance);
    }

    if (printUsedOptions != null) {
      printOptionInfos(member, optionName, valueStr, defaultValue);
    }
    return value;
  }

  /**
   * Return a string describing the source of an option suitable for logging (best-effort, may
   * return an empty string).
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
   * This function takes the new value of an {@link Option} in the property, checks it (allowed
   * values, regexp) and returns it.
   *
   * @param name name of the value
   * @param option the option-annotation of the field of the value
   * @param alwaysUppercase how to write the value
   */
  @Nullable
  private String getValueString(String name, Option option, boolean alwaysUppercase)
      throws InvalidConfigurationException {

    // get value in String representation
    @Var String valueStr = trimToNull(getProperty(name));

    if (valueStr == null) {
      return null;
    }

    if (alwaysUppercase || option.toUppercase()) {
      valueStr = valueStr.toUpperCase(Locale.getDefault());
    }

    // check if it is included in the allowed values list
    String[] allowedValues = option.values();
    if (allowedValues.length > 0 && !Arrays.asList(allowedValues).contains(valueStr)) {
      throw new InvalidConfigurationException(
          String.format(
              "Invalid value in configuration file: \"%s = %s\" (not listed as allowed value)",
              name, valueStr));
    }

    // check if it matches the specification regexp
    String regexp = option.regexp();
    if (!regexp.isEmpty() && !valueStr.matches(regexp)) {
      throw new InvalidConfigurationException(
          String.format(
              "Invalid value in configuration file: \"%s = %s\" (does not match RegExp \"%s\").",
              name, valueStr, regexp));
    }

    return valueStr;
  }

  /**
   * Find any annotation which itself is annotated with {@link OptionDetailAnnotation} on a member.
   */
  private static @Nullable Annotation getSecondaryAnnotation(AnnotatedElement element) {
    @Var Annotation result = null;
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
   * Check whether a given annotation (which itself has to be annotated with {@link
   * OptionDetailAnnotation}!) is applicable to an option of a given type.
   *
   * @throws UnsupportedOperationException If the annotation is not applicable.
   */
  private static void checkApplicability(
      @Nullable Annotation annotation, @Var TypeToken<?> optionType) {
    if (annotation == null) {
      return;
    }

    List<Class<?>> applicableTypes =
        Arrays.asList(
            annotation.annotationType().getAnnotation(OptionDetailAnnotation.class).applicableTo());

    if (optionType.getRawType() == AnnotatedValue.class) {
      optionType = Classes.getSingleTypeArgument(optionType);
    }

    if (!applicableTypes.isEmpty() && !applicableTypes.contains(optionType.getRawType())) {
      throw new UnsupportedOperationException(
          String.format(
              "Annotation %s is not applicable for options of type %s.", annotation, optionType));
    }
  }

  private void printOptionInfos(
      AnnotatedElement element,
      String name,
      @Nullable String valueStr,
      @Nullable Object defaultValue) {

    StringBuilder optionInfo = new StringBuilder();
    optionInfo
        .append(OptionPlainTextWriter.getOptionDescription(element))
        .append(name)
        .append('\n');

    if (defaultValue != null) {
      String defaultStr;

      if (defaultValue instanceof Object[]) {
        defaultStr = Arrays.deepToString((Object[]) defaultValue);
      } else {
        defaultStr = defaultValue.toString();
      }

      optionInfo.append("    default value:  ").append(defaultStr).append('\n');
    }

    if (valueStr != null) {
      optionInfo.append("--> used value:     ").append(valueStr).append('\n');
    }

    printUsedOptions.println(optionInfo.toString());
  }

  /**
   * This function takes a value (String) and a type and returns an Object of this type with the
   * value as content.
   *
   * @param optionName name of option, only for error handling
   * @param valueStr new value of the option
   * @param pType type of the object
   * @param secondaryOption the optional second annotation of the option
   */
  private @Nullable <T> Object convertValue(
      String optionName, String valueStr, TypeToken<?> pType, @Nullable Annotation secondaryOption)
      throws InvalidConfigurationException {
    // convert value to correct type

    Class<?> collectionClass = COLLECTIONS.get(pType.getRawType());

    if (collectionClass == null && !pType.isArray()) {
      TypeToken<?> type = pType.wrap();

      // single value, easy case
      checkApplicability(secondaryOption, type);

      return convertSingleValue(optionName, valueStr, type, secondaryOption);
    }

    // first get the real type of a single value (i.e., String[] => String)
    @Var TypeToken<?> componentType;
    if (pType.isArray()) {
      componentType = checkNotNull(pType.getComponentType());
    } else {
      componentType = Classes.getSingleTypeArgument(pType);
    }

    componentType = componentType.wrap();

    checkApplicability(secondaryOption, componentType);

    List<?> values = convertMultipleValues(optionName, valueStr, componentType, secondaryOption);

    if (pType.isArray()) {

      @SuppressWarnings("unchecked")
      Class<T> arrayComponentType = (Class<T>) componentType.getRawType();
      T[] result = ObjectArrays.newArray(arrayComponentType, values.size());

      // noinspection SuspiciousToArrayCall
      return values.toArray(result);
    }
    assert collectionClass != null;

    if (collectionClass == EnumSet.class) {
      assert componentType.getRawType().isEnum();
      return createEnumSetUnchecked(componentType.getRawType(), values);

    } else if (componentType.getRawType().isEnum()
        && (collectionClass == Set.class || collectionClass == ImmutableSet.class)) {
      // There is a specialized ImmutableSet for enums in Guava that is more efficient.
      // We use it if we can.
      return BaseTypeConverter.invokeStaticMethod(
          Sets.class, "immutableEnumSet", Iterable.class, values, optionName);

    } else {
      // we know that it's a Collection<componentType> / Set<? extends componentType> etc.,
      // so we can safely assign to it

      // invoke ImmutableSet.copyOf(Iterable) etc.
      return BaseTypeConverter.invokeStaticMethod(
          collectionClass, "copyOf", Iterable.class, values, optionName);
    }
  }

  /**
   * This function takes a value (String) and a type and returns an Object of this type with the
   * value as content.
   *
   * <p>The type may not be an array or a collection type, and the value may only be a single value
   * (not multiple values).
   *
   * @param optionName name of option, only for error handling
   * @param valueStr new value of the option
   * @param type type of the object
   * @param secondaryOption the optional second annotation of the option (needs to fit to the type)
   */
  private @Nullable Object convertSingleValue(
      String optionName,
      @Var String valueStr,
      @Var TypeToken<?> type,
      @Nullable Annotation secondaryOption)
      throws InvalidConfigurationException {

    boolean isAnnotated = type.getRawType() == AnnotatedValue.class;
    @Var String annotation = null;
    if (isAnnotated) {
      type = Classes.getSingleTypeArgument(type);
      Iterator<String> parts = ANNOTATION_VALUE_SPLITTER.split(valueStr).iterator();
      valueStr = parts.next();
      annotation = Iterators.getNext(parts, null);
    }

    // try to find a type converter, either for the type of the annotation
    // or for the type of the field
    TypeConverter converter = getConverter(type, secondaryOption);
    @Var
    Object result =
        converter.convert(
            optionName,
            valueStr,
            type,
            secondaryOption,
            sources.get(optionName),
            Objects.requireNonNullElse(logger, LogManager.createNullLogManager()));

    if (result != null && isAnnotated) {
      result = AnnotatedValue.create(result, Optional.ofNullable(annotation));
    }

    return result;
  }

  /**
   * Convert a String which possibly contains multiple values into a list of objects of the correct
   * type.
   *
   * @param optionName name of option, only for error handling
   * @param valueStr new value of the option
   * @param type type of each object
   * @param secondaryOption the optional second annotation of the option
   * @return a list of instances of arbitrary objects
   * @throws InvalidConfigurationException if conversion fails
   */
  private List<?> convertMultipleValues(
      String optionName, String valueStr, TypeToken<?> type, @Nullable Annotation secondaryOption)
      throws InvalidConfigurationException {

    Iterable<String> values = ARRAY_SPLITTER.split(valueStr);

    List<Object> result = new ArrayList<>();

    for (String item : values) {
      result.add(convertSingleValue(optionName, item, type, secondaryOption));
    }

    return result;
  }

  private @Nullable <T> Object convertDefaultValue(
      String optionName,
      @Nullable T defaultValue,
      TypeToken<T> type,
      @Nullable Annotation secondaryOption,
      boolean fromOtherInstance)
      throws InvalidConfigurationException {

    @Var TypeToken<?> innerType;
    if (type.isArray()) {
      innerType = checkNotNull(type.getComponentType());
    } else if (COLLECTIONS.containsKey(type.getRawType())) {
      innerType = Classes.getSingleTypeArgument(type);
    } else {
      innerType = type;
    }

    innerType = innerType.wrap();

    checkApplicability(secondaryOption, innerType);

    if (type.equals(innerType)) {
      // If its not a collection, we try to pass the default value to the
      // type converter if there is any.
      // TODO: Also pass default values inside a collection.

      TypeConverter converter = getConverter(type, secondaryOption);
      if (fromOtherInstance) {
        return converter.convertDefaultValueFromOtherInstance(
            optionName, defaultValue, type, secondaryOption);
      } else {
        return converter.convertDefaultValue(optionName, defaultValue, type, secondaryOption);
      }
    }

    return defaultValue;
  }

  /**
   * Find a type converter for an option.
   *
   * @return A type converter.
   */
  private TypeConverter getConverter(TypeToken<?> type, @Nullable Annotation secondaryOption) {
    @Var TypeConverter converter = null;
    if (secondaryOption != null) {
      converter = converters.get(secondaryOption.annotationType());
    }
    if (converter == null) {
      converter = converters.get(type.getRawType());
    }
    if (converter == null) {
      converter = BaseTypeConverter.INSTANCE;
    }
    return converter;
  }

  /** A null-safe combination of {@link String#trim()} and {@link Strings#emptyToNull(String)}. */
  @Nullable
  private static String trimToNull(@Nullable String s) {
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

  /**
   * Construct a configuration object from the array of command line arguments.
   *
   * <p>The input format is as follows:
   *
   * <pre>
   * <code>
   *   --option=Value
   * </code>
   * </pre>
   *
   * @param args Command line arguments
   * @return Constructed {@link Configuration} instance
   * @throws InvalidConfigurationException On incorrect format or when configuration options for
   *     Configurations class are invalid
   */
  public static Configuration fromCmdLineArguments(String[] args)
      throws InvalidConfigurationException {
    ConfigurationBuilder builder = Configuration.builder();
    for (String arg : args) {
      if (!arg.startsWith("--")) {
        throw new InvalidConfigurationException(
            "Invalid command-line argument '" + arg + "', --option=value syntax expected.");
      }

      List<String> tokens = Splitter.on("=").omitEmptyStrings().trimResults().splitToList(arg);
      if (tokens.size() != 2) {
        throw new InvalidConfigurationException(
            "Invalid command-line argument '" + arg + "', --option=value syntax expected.");
      }
      builder.setOption(tokens.get(0).substring(2), tokens.get(1));
    }
    return builder.build();
  }
}
