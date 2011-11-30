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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.Files;
import org.sosy_lab.common.Pair;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.ObjectArrays;
import com.google.common.io.Closeables;
import com.google.common.primitives.Primitives;


/**
 * Immutable wrapper around a {@link Properties} instance, providing some
 * useful access helper methods.
 */
@Options
public class Configuration {

  public static class Builder {

    private Map<String, String> properties = null;
    private Configuration oldConfig = null;
    private String prefix = null;

    private Builder() { }

    private void setupProperties() {
      if (properties == null) {
        properties = new HashMap<String, String>();
      }
      if (oldConfig != null) {
        properties.putAll(oldConfig.properties);
      }
    }

    /**
     * Set a single option.
     */
    public Builder setOption(String name, String value) {
      Preconditions.checkNotNull(name);
      Preconditions.checkNotNull(value);
      setupProperties();

      properties.put(name, value);

      return this;
    }

    /**
     * Reset a single option to its default value.
     */
    public Builder clearOption(String name) {
      Preconditions.checkNotNull(name);
      setupProperties();

      properties.remove(name);

      return this;
    }

    /**
     * Add all options from a map.
     */
    public Builder setOptions(Map<String, String> options) {
      Preconditions.checkNotNull(options);
      setupProperties();

      properties.putAll(options);

      return this;
    }

    /**
     * Set the optional prefix for new configuration.
     */
    public Builder setPrefix(String prefix) {
      Preconditions.checkNotNull(prefix);

      this.prefix = prefix;

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
    public Builder copyFrom(Configuration oldConfig) {
      Preconditions.checkNotNull(oldConfig);
      Preconditions.checkState(this.properties == null);
      Preconditions.checkState(this.oldConfig == null);

      this.oldConfig = oldConfig;

      return this;
    }

    /**
     * Load options from an InputStream with a "key = value" format.
     * @see Properties#load(InputStream)
     *
     * If this method is called, it has to be the first method call on this
     * builder instance.
     * @throws IOException If the stream cannot be read.
     */
    public Builder loadFromStream(InputStream stream) throws IOException {
      Preconditions.checkNotNull(stream);
      Preconditions.checkState(properties == null);
      Preconditions.checkState(oldConfig == null);

      Properties p = new Properties();
      p.load(stream);

      properties = new HashMap<String, String>(p.size());
      for (Map.Entry<Object, Object> e : p.entrySet()) {
        properties.put((String)e.getKey(), (String)e.getValue());
      }

      return this;
    }

    /**
     * Load options from a file with a "key = value" format.
     * @see Properties#load(InputStream)
     *
     * If this method is called, it has to be the first method call on this
     * builder instance.
     * @throws IOException If the file cannot be read.
     */
    public Builder loadFromFile(String filename) throws IOException {
      return loadFromFile(new File(filename));
    }

    /**
     * Load options from a file with a "key = value" format.
     * @see Properties#load(InputStream)
     *
     * If this method is called, it has to be the first method call on this
     * builder instance.
     * @throws IOException If the file cannot be read.
     */
    public Builder loadFromFile(File file) throws IOException {
      Preconditions.checkNotNull(file);

      InputStream stream = null;
      try {
        stream = new FileInputStream(file);
        loadFromStream(stream);
      } finally {
        Closeables.closeQuietly(stream);
      }
      return this;
    }

    /**
     * Create a Configuration instance with the settings specified by method
     * calls on this builder instance.
     *
     * This method resets the builder instance, so that after this method has
     * returned it is exactly in the same state as directly after instantiation.
     *
     * @throws InvalidConfigurationException if the settings contained invalid values for the configuration options of the Configuration class
     */
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

      Set<String> newUnusedProperties;
      Set<String> newDeprecatedProperties;
      if (oldConfig != null) {
        // share the same set of unused properties
        newUnusedProperties = oldConfig.unusedProperties;
        newDeprecatedProperties = oldConfig.deprecatedProperties;
      } else {
        newUnusedProperties = new HashSet<String>(newProperties.keySet());
        newDeprecatedProperties = new HashSet<String>(0);
      }

      Configuration newConfig = new Configuration(newProperties, newPrefix, newUnusedProperties, newDeprecatedProperties);
      newConfig.inject(newConfig);

      // reset builder instance so that it may be re-used
      properties = null;
      prefix = null;
      oldConfig = null;

      return newConfig;
    }
  }

  /**
   * Create a new Builder instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a configuration with all values set to default.
   */
  public static Configuration defaultConfiguration() {
    return new Configuration(ImmutableMap.<String, String>of(), "", new HashSet<String>(0), new HashSet<String>(0));
  }

  /**
   * Creates a copy of a configuration with just the prefix set to a new value.
   */
  public static Configuration copyWithNewPrefix(Configuration oldConfig, String newPrefix) {
    Configuration newConfig = new Configuration(oldConfig.properties, newPrefix, oldConfig.unusedProperties, oldConfig.deprecatedProperties);

    // instead of calling inject() set options manually
    // this avoids the "throws InvalidConfigurationException" in the signature
    newConfig.disableOutput = oldConfig.disableOutput;
    newConfig.outputDirectory = oldConfig.outputDirectory;
    newConfig.rootDirectory = oldConfig.rootDirectory;
    newConfig.exportUsedOptions = oldConfig.exportUsedOptions;

    return newConfig;
  }

  @Option(name="output.path", description="directory to put all output files in")
  private String outputDirectory = "output/";

  @Option(name="output.disable", description="disable all default output files"
    + "\n(any explicitly given file will still be written)")
  private boolean disableOutput = false;

  @Option (description="base directory for all input & output files"
    + "\n(except for the configuration file itself)")
  private String rootDirectory = ".";

  @Option (name="log.usedOptions.export",
      description="all used options are printed")
  private boolean exportUsedOptions = false;

  /** Splitter to create string arrays */
  private static final Splitter ARRAY_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  /** Map that stores which implementation we use for the collection classes */
  private static final Map<Class<? extends Iterable<?>>, Class<? extends Iterable<?>>> COLLECTIONS;
  static {
    ImmutableMap.Builder<Class<? extends Iterable<?>>, Class<? extends Iterable<?>>> builder = ImmutableMap.builder();

    putSafely(builder, Iterable.class,   ImmutableList.class);
    putSafely(builder, Collection.class, ImmutableList.class);
    putSafely(builder, List.class,       ImmutableList.class);
    putSafely(builder, Set.class,        ImmutableSet.class);
    putSafely(builder, SortedSet.class,  ImmutableSortedSet.class);
    putSafely(builder, Multiset.class,   ImmutableMultiset.class);

    putSafely(builder, ImmutableCollection.class, ImmutableList.class);
    putSafely(builder, ImmutableList.class,       ImmutableList.class);
    putSafely(builder, ImmutableSet.class,        ImmutableSet.class);
    putSafely(builder, ImmutableSortedSet.class,  ImmutableSortedSet.class);
    putSafely(builder, ImmutableMultiset.class,   ImmutableMultiset.class);

    COLLECTIONS = builder.build();
  }

  // using this method to put key-value pairs into the builder ensures that
  // each implementation really implements the interface
  private static <T extends Iterable<?>> void putSafely(
      ImmutableMap.Builder<Class<? extends Iterable<?>>, Class<? extends Iterable<?>>> builder,
      Class<T> iface, Class<? extends T> impl) {
    assert !impl.isInterface();
    builder.put(iface, impl);
  }

  private final ImmutableMap<String, String> properties;

  private final String prefix;

  private final Set<String> unusedProperties;
  private final Set<String> deprecatedProperties;

  /*
   * This constructor does not set the fields annotated with @Option!
   */
  private Configuration(ImmutableMap<String, String> pProperties, String pPrefix,
      Set<String> pUnusedProperties, Set<String> pDeprecatedProperties) {
    properties = pProperties;
    prefix = (pPrefix.isEmpty() ? "" : (pPrefix + "."));
    unusedProperties = pUnusedProperties;
    deprecatedProperties = pDeprecatedProperties;
  }

  /**
   * @see Properties#getProperty(String)
   */
  public String getProperty(String key) {
    String result = properties.get(prefix + key);
    unusedProperties.remove(prefix + key);

    if (result == null && !prefix.isEmpty()) {
      result = properties.get(key);
      unusedProperties.remove(key);
    }
    return result;
  }

  /**
   * @see Properties#getProperty(String, String)
   */
  @Deprecated
  public String getProperty(String key, String defaultValue) {
    String result = getProperty(key);
    if (result == null) {
      result = defaultValue;
    }
    return result;
  }

  /**
   * If there are a number of properties for a given key, this method will split them
   * on commas (trimming the parts) and return the array of properties
   * @param key the key for the property
   * @return array of properties or empty array if property is not specified
   */
  public String[] getPropertiesArray(String key){
    String s = getProperty(key);
    return (s != null) ? Iterables.toArray(ARRAY_SPLITTER.split(s), String.class) : new String[0];
  }

  public Set<String> getUnusedProperties() {
    return Collections.unmodifiableSet(unusedProperties);
  }

  public Set<String> getDeprecatedProperties() {
    return Collections.unmodifiableSet(deprecatedProperties);
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
   * - {@link Class<Something>}
   * - {@link java.util.logging.Level}
   * - arrays of the above types
   * - collection types {@link Iterable}, {@link Collection}, {@link List},
   *   {@link Set}, {@link SortedSet} and {@link Multiset} of the above types
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
    Preconditions.checkNotNull(cls.getAnnotation(Options.class), "Class must have @Options annotation.");

    do {
      if (cls.getAnnotation(Options.class) != null) {
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
    Preconditions.checkNotNull(obj);
    Preconditions.checkNotNull(cls);
    Preconditions.checkArgument(cls.isAssignableFrom(obj.getClass()));

    final Options options = cls.getAnnotation(Options.class);
    Preconditions.checkNotNull(options, "Class must have @Options annotation.");

    // handle fields of the class, override all final & private modifiers
    final Field[] fields = cls.getDeclaredFields();
    Field.setAccessible(fields, true);

    for (final Field field : fields) {
      // ignore all non-option fields
      if (field.isAnnotationPresent(Option.class)) {
        setOptionValueForField(obj, field, options);
      }
    }

    // handle methods of the class, override all final & private modifiers
    final Method[] methods = cls.getDeclaredMethods();
    Method.setAccessible(methods, true);

    for (final Method method : methods) {
      // ignore all non-option methods
      if (method.isAnnotationPresent(Option.class)) {
        setOptionValueForMethod(obj, method, options);
      }
    }
  }

  /** This method sets a new value to a field with an {@link Options}-annotation.
   * It takes the name and the new value of an option,
   * checks it for allowed values and injects it into the object.
   *
   * @param obj the object to be injected
   * @param field the field of the value to be injected
   * @param options options-annotation of the class of the object */
  private void setOptionValueForField(final Object obj, final Field field,
      final Options options) throws InvalidConfigurationException {
    final Class<?> type = field.getType();
    final Type genericType = field.getGenericType();

    // try to read default value
    Object defaultValue = null;
    try {
      defaultValue = field.get(obj);
    } catch (IllegalArgumentException e) {
      assert false : "Type checks above were not successful apparently.";
    } catch (IllegalAccessException e) {
      assert false : "Accessibility setting failed silently above.";
    }

    final Option option = field.getAnnotation(Option.class);
    final String name = getOptionName(options, field, option);
    final Object value = getValue(name, defaultValue, type, genericType, option, field);

    // options which were not changed need not to be set
    if (value == defaultValue) { return; }

    // set value to field
    try {
      field.set(obj, value);
    } catch (IllegalArgumentException e) {
      assert false : "Type checks above were not successful apparently.";
    } catch (IllegalAccessException e) {
      assert false : "Accessibility setting failed silently above.";
    }
  }

  private Annotation getSecondaryAnnotation(AnnotatedElement element) {
    Annotation result = null;
    for (Annotation a : element.getDeclaredAnnotations()) {
      if (a.annotationType().isAnnotationPresent(OptionDetailAnnotation.class)) {
        if (result != null) {
          throw new UnsupportedOperationException("Both " + result + " and " + a + " are present at " + element.toString());
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
  private void checkApplicability(Annotation annotation, final Class<?> optionType) throws UnsupportedOperationException {
    if (annotation == null) {
      return;
    }

    Class<?>[] applicableTypes = annotation.annotationType().getAnnotation(OptionDetailAnnotation.class).applicableTo();

    boolean applicable = false;
    for (Class<?> candidate : applicableTypes) {
      if (candidate.isAssignableFrom(optionType)) {
        applicable = true;
        break;
      }
    }

    if (!applicable) {
      throw new UnsupportedOperationException("Annotation " + annotation + " is not applicable for options of type " + optionType.getCanonicalName());
    }
  }

  private void printOptionInfos(final Class<?> type, final Option option,
      final String name, final String valueStr, final Object defaultValue) {

    // create optionInfo for file
    final StringBuilder optionInfo =
        new StringBuilder(OptionCollector.formatText(option.description()));
    optionInfo.append(name + "\n");
    if (type.isArray()) {
      optionInfo.append("    default value:  "
          + java.util.Arrays.deepToString((Object[]) defaultValue) + "\n");
    } else if (type.equals(String.class)) {
      optionInfo.append("    default value:  '" + defaultValue + "'\n");
    } else {
      optionInfo.append("    default value:  " + defaultValue + "\n");
    }
    if (valueStr != null) {
      optionInfo.append("--> used value:     " + valueStr + "\n");
    }

    System.out.println(optionInfo.toString());
  }

  /** This method sets a new value to a method with an {@link Options}-annotation.
   * It takes the name and the new value of an option,
   * checks it for allowed values and injects it into the object.
   *
   * @param obj the object to be injected
   * @param method the method of the value to be injected
   * @param options options-annotation of the class of the object */
  private void setOptionValueForMethod(final Object obj, final Method method,
      final Options options) throws InvalidConfigurationException{

    final Class<?>[] parameters = method.getParameterTypes();
    if (parameters.length != 1) {
      throw new IllegalArgumentException(
          "Method with @Option must have exactly one parameter!");
    }
    final Class<?> type = parameters[0];
    final Type genericType = method.getGenericParameterTypes()[0];

    String exception = Classes.verifyDeclaredExceptions(method, InvalidConfigurationException.class);
    if (exception != null) {
      throw new IllegalArgumentException("Method with @Option may not throw " + exception);
    }

    final Option option = method.getAnnotation(Option.class);
    final String name = getOptionName(options, method, option);
    final Object value = getValue(name, null, type, genericType, option, method);

    // set value to field
    try {
      method.invoke(obj, value);
    } catch (IllegalArgumentException e) {
      assert false : "Type checks above were not successful apparently.";
    } catch (IllegalAccessException e) {
      assert false : "Accessibility setting failed silently above.";
    } catch (InvocationTargetException e) {
      // ITEs always have a wrapped exception which is the real one thrown by
      // the invoked method. We want to handle this exception.
      final Throwable t = e.getCause();

      if (t instanceof IllegalArgumentException) {
        // this is an expected exception if the value is wrong, so create a nice message for the user
        throw new InvalidConfigurationException("Invalid value in configuration file: \""
            + name + " = " + value + '\"'
            + (t.getMessage() != null ? " (" + t.getMessage() + ")" : ""), t);
      }

      Throwables.propagateIfPossible(t, InvalidConfigurationException.class);
      throw new UnexpectedCheckedException("configuration injection in method " + method, t);
    }
  }

  /** This function return the name of an {@link Option}.
   * If no optionname is defined, the name of the field is returned.
   * If a prefix is defined, it is added in front of the name.
   *
   * @param options the options-annotation of the class, that contains the field
   * @param field field with option-annotation
   * @param option the option-annotation */
  private String getOptionName(final Options options, final Member field, final Option option) {
    String prefix = options.prefix();
    if (!prefix.isEmpty()) {
      prefix += ".";
    }
    String name = option.name();
    if (name.isEmpty()) {
      name = field.getName();
    }
    return prefix + name;
  }

  /**
   * This method gets the value which needs to be assigned to the option.
   *
   * @param optionName The name of the option.
   * @param defaultValue The default value (may be null).
   * @param type The type of the option.
   * @param genericType The type of the option.
   * @param option The annotation of the option.
   * @param member The member that declares the option.
   * @return The value to assign (may be null).
   *
   * @throws UnsupportedOperationException If the declaration of the option in the source code is invalid.
   * @throws InvalidConfigurationException If the user specified an invalid value for the option.
   */
  private Object getValue(final String optionName, final Object defaultValue,
      final Class<?> type, final Type genericType, final Option option,
      final AnnotatedElement member)
      throws UnsupportedOperationException, InvalidConfigurationException {

    final String valueStr = getValueString(optionName, option, type.isEnum());
    final Annotation secondaryOption = getSecondaryAnnotation(member);

    final Object value;
    if (valueStr != null) {
      // option was specified
      value = convertValue(optionName, valueStr, type, genericType, secondaryOption);

      if (member.getAnnotation(Deprecated.class) != null) {
        deprecatedProperties.add(optionName);
      }

    } else {
      if (option.required()) {
        throw new InvalidConfigurationException(
          "Required configuration option " + optionName  + " is missing!");
      }

      value = convertDefaultValue(optionName, defaultValue, type, genericType, secondaryOption);
    }

    if (exportUsedOptions) {
      printOptionInfos(type, option, optionName, valueStr, null);
    }
    return value;
  }

  /**
   * This function takes the new value of an {@link Option}
   * in the property, checks it (allowed values, regexp) and returns it.
   *
   * @param name name of the value
   * @param option the option-annotation of the field of the value
   * @param alwaysUppercase how to write the value
   */
  private String getValueString(final String name, final Option option,
      final boolean alwaysUppercase) throws InvalidConfigurationException {

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
    if (allowedValues.length > 0
        && !java.util.Arrays.asList(allowedValues).contains(valueStr)) {
      throw new InvalidConfigurationException(
        "Invalid value in configuration file: \"" + name + " = " + valueStr
            + '\"' + " (not listed as allowed value)");
    }

    // check if it matches the specification regexp
    final String regexp = option.regexp();
    if (!regexp.isEmpty() && !valueStr.matches(regexp)) {
      throw new InvalidConfigurationException(
        "Invalid value in configuration file: \"" + name + " = " + valueStr
            + '\"' + " (does not match RegExp \"" + regexp + "\")");
    }

    return valueStr;
  }

  /**
   * A null-safe combination of {@link String#trim()} and {@link Strings#emptyToNull(String)}.
   */
  private static String trimToNull(String s) {
    if (s == null) {
      return null;
    }
    return Strings.emptyToNull(s.trim());
  }

  /**
   * This function takes a value (String) and a type and
   * returns an Object of this type with the value as content.
   *
   * @param optionName name of option, only for error handling
   * @param valueStr new value of the option
   * @param type type of the object
   * @param genericType type of the object
   * @param secondaryOption the optional second annotation of the option
   */
  private <T> Object convertValue(final String optionName, final String valueStr,
      final Class<?> type, final Type genericType, final Annotation secondaryOption)
      throws UnsupportedOperationException, InvalidConfigurationException {
    // convert value to correct type

    Class<?> collectionClass = COLLECTIONS.get(type);

    if (collectionClass == null && !type.isArray()) {
      // single value, easy case
      checkApplicability(secondaryOption, type);

      return convertSingleValue(optionName, valueStr, type, genericType, secondaryOption);
    }

    // first get the real type of a single value (i.e., String[] => String)
    Pair<Class<?>, Type> p = getComponentType(type, genericType);

    checkApplicability(secondaryOption, p.getFirst());

    List<?> values = convertMultipleValues(optionName, valueStr, p.getFirst(), p.getSecond(), secondaryOption);

    if (type.isArray()) {

      @SuppressWarnings("unchecked")
      Class<T> componentType = (Class<T>) p.getFirst();
      T[] result = ObjectArrays.newArray(componentType, values.size());

      return values.toArray(result);

    } else {
      assert collectionClass != null;
      // we now that it's a Collection<componentType> / Set<? extends componentType> etc., so we can safely assign to it

      // invoke ImmutableSet.copyOf(Iterable) etc.
      return invokeMethod(collectionClass, "copyOf", Iterable.class, values, optionName);
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
  private Object convertSingleValue(final String optionName, final String valueStr,
      final Class<?> type, final Type genericType, final Annotation secondaryOption)
      throws InvalidConfigurationException {

    if (type.isPrimitive()) {
      // get wrapper type in order to use valueOf method
      final Class<?> wrapperType = Primitives.wrap(type);
      Object value = valueOf(wrapperType, optionName, valueStr);

      checkRange(optionName, value, secondaryOption);

      return value;

    } else if (Primitives.isWrapperType(type)) {
      // all wrapper types have valueOf method
      Object value = valueOf(type, optionName, valueStr);

      checkRange(optionName, value, secondaryOption);

      return value;

    } else if (type.isEnum()) {
      // all enums have valueOf method
      return valueOf(type, optionName, valueStr);

    } else if (type.equals(String.class)) {
      return valueStr;

    } else if (type.equals(File.class)) {
      if (!(secondaryOption instanceof FileOption)) {
        throw new UnsupportedOperationException("Options of type File need to be annotated with @FileOption");
      }

      return handleFileOption(optionName, new File(valueStr), ((FileOption)secondaryOption).value());

    } else if (type.equals(Class.class)) {
      return handleClassOption(optionName, valueStr, genericType, secondaryOption);

    } else if (type.equals(Level.class)) {
      try {
        return Level.parse(valueStr);
      } catch (IllegalArgumentException e) {
        throw new InvalidConfigurationException("Illegal log level " + valueStr + " in option " + optionName);
      }

    } else {
      throw new UnsupportedOperationException(
          "Unimplemented type for option: " + type.getSimpleName());
    }
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
  private List<?> convertMultipleValues(final String optionName, final String valueStr,
      final Class<?> type, final Type genericType, final Annotation secondaryOption)
      throws InvalidConfigurationException {

    checkApplicability(secondaryOption, type);

    Iterable<String> values = ARRAY_SPLITTER.split(valueStr);

    List<Object> result = new ArrayList<Object>();

    for (String item : values) {
      result.add(convertSingleValue(optionName, item, type, genericType, secondaryOption));
    }

    return result;
  }

  private Object valueOf(final Class<?> type, final String optionName, final String value)
      throws InvalidConfigurationException {
    return invokeMethod(type, "valueOf", String.class, value, optionName);
  }

  private <T> Object invokeMethod(final Class<?> type, final String method,
      final Class<T> paramType, final T value, final String optionName)
          throws InvalidConfigurationException {
    try {
      Method m = type.getMethod(method, paramType);
      if (!m.isAccessible()) {
        m.setAccessible(true);
      }
      return m.invoke(null, value);

    } catch (NoSuchMethodException e) {
      throw new AssertionError("Class " + type.getSimpleName() + " without "
          + method + "(" + paramType.getSimpleName() + ") method!");
    } catch (SecurityException e) {
      throw new AssertionError("Class " + type.getSimpleName() + " without accessible "
          + method + "(" + paramType.getSimpleName() + ") method!");
    } catch (IllegalAccessException e) {
      throw new AssertionError("Class " + type.getSimpleName() + " without accessible "
          + method + "(" + paramType.getSimpleName() + ") method!");
    } catch (InvocationTargetException e) {
      throw new InvalidConfigurationException("Could not parse \"" + optionName +
          " = " + value + "\" (" + e.getTargetException().getMessage() + ")", e);
    }
  }

  private Object convertDefaultValue(final String optionName, final Object defaultValue,
      final Class<?> type, final Type genericType, final Annotation secondaryOption) throws InvalidConfigurationException {

    Class<?> innerType;
    if (type.isArray()) {
      innerType = type.getComponentType();
    } else if (COLLECTIONS.containsKey(type)) {
      innerType = getComponentType(type, genericType).getFirst();
    } else {
      innerType = type;
    }

    checkApplicability(secondaryOption, innerType);

    // TODO check for forbidden collections of Files with default values

    if (defaultValue == null || !(defaultValue instanceof File)) {
      return defaultValue;
    }

    if (!(secondaryOption instanceof FileOption)) {
      throw new UnsupportedOperationException("Options of type File need to be annotated with @FileOption");
    }

    FileOption.Type typeInfo = ((FileOption)secondaryOption).value();
    File file = (File)defaultValue;

    if (typeInfo == FileOption.Type.OUTPUT_FILE) {
      if (disableOutput) {
        // if output is disabled and option was not specified explicitly
        return null;
      }
    }

    return handleFileOption(optionName, file, typeInfo);
  }

  /** This function returns a file. It sets the path of the file to
   * the given outputDirectory in the given rootDirectory.
   *
   * @param optionName name of option only for error handling
   * @param file the file name to adjust
   * @param typeInfo info about the type of the file (outputfile, inputfile) */
  private File handleFileOption(final String optionName, File file, final FileOption.Type typeInfo)
          throws InvalidConfigurationException {

    if (typeInfo == FileOption.Type.OUTPUT_FILE) {
      if (!file.isAbsolute()) {
        file = new File(outputDirectory, file.getPath());
      }
    }

    if (!file.isAbsolute()) {
      file = new File(rootDirectory, file.getPath());
    }

    if (file.isDirectory()) {
      throw new InvalidConfigurationException("Option " + optionName + " needs to specify a file and not a directory");
    }

    if (typeInfo == FileOption.Type.REQUIRED_INPUT_FILE) {
      try {
        Files.checkReadableFile(file);
      } catch (FileNotFoundException e) {
        throw new InvalidConfigurationException("Option " + optionName
            + " specifies an invalid input file: " + e.getMessage(), e);
      }
    }

    return file;
  }

  private Class<?> handleClassOption(final String optionName, final String valueStr,
      Type genericType, Annotation secondaryOption) throws InvalidConfigurationException {

    // get optional package prefix
    String packagePrefix = "";
    if (secondaryOption != null) {
      if (!(secondaryOption instanceof ClassOption)) {
        throw new UnsupportedOperationException("Options of type Class may not be annotated with " + secondaryOption);
      }
      packagePrefix = ((ClassOption) secondaryOption).packagePrefix();
    }

    assert genericType != null : "Need a generic type for class options";

    // get value of type parameter
    assert genericType instanceof ParameterizedType : "Class type that is not a ParameterizedType";
    ParameterizedType pType = (ParameterizedType)genericType;
    Type[] parameterTypes = pType.getActualTypeArguments();
    assert parameterTypes.length == 1 : "Class type with more than one type parameter";
    Type paramType = parameterTypes[0];

    paramType = extractUpperBoundFromType(paramType);
    if (paramType instanceof ParameterizedType) {
      paramType = ((ParameterizedType)paramType).getRawType();
    }

    Class<?> targetType;
    if (paramType instanceof Class<?>) {
      targetType = (Class<?>)paramType;
    } else {
      throw new UnsupportedOperationException("Currently types like \"" + paramType + "\" are not supported");
    }

    // get class object
    Class<?> cls;
    try {
      cls = Classes.forName(valueStr, packagePrefix);
    } catch (ClassNotFoundException e) {
      throw new InvalidConfigurationException("Class " + valueStr + " specified in option " + optionName + " not found");
    }

    // check type
    if (!targetType.isAssignableFrom(cls)) {
      throw new InvalidConfigurationException("Class " + valueStr + " specified in option " + optionName + " is not an instance of " + targetType.getCanonicalName());
    }

    return cls;
  }

  private Object handleCollectionOption(String optionName, String valueStr,
      Class<?> type, Type genericType, final Annotation secondaryOption) throws UnsupportedOperationException,
      InvalidConfigurationException {

    Pair<Class<?>, Type> componentType = getComponentType(type, genericType);

    // now we now that it's a Collection<componentType> / Set<? extends componentType> etc., so we can safely assign to it

    Class<?> implementationClass = COLLECTIONS.get(type);
    assert implementationClass != null : "Only call this method with a class that has a mapping in COLLECTIONS";

    Iterable<?> values = convertMultipleValues(optionName, valueStr, componentType.getFirst(), componentType.getSecond(), secondaryOption);

    // invoke ImmutableSet.copyOf(Iterable) etc.
    return invokeMethod(implementationClass, "copyOf", Iterable.class, values, optionName);
  }

  private static Pair<Class<?>, Type> getComponentType(Class<?> type, Type genericType) {
    if (type.isArray()) {
      return Pair.<Class<?>, Type>of((Class<?>)type.getComponentType(), null);
    }

    // it's a collections class, get value of type parameter
    assert genericType instanceof ParameterizedType : "Collections type that is not a ParameterizedType";
    ParameterizedType pType = (ParameterizedType)genericType;
    Type[] parameterTypes = pType.getActualTypeArguments();
    assert parameterTypes.length == 1 : "Collections type with more than one type parameter";
    Type paramType = parameterTypes[0];

    paramType = extractUpperBoundFromType(paramType);

    Class<?> componentType;
    Type componentGenericType = null;
    if (paramType instanceof ParameterizedType) {
      componentGenericType = paramType;
      paramType = ((ParameterizedType)paramType).getRawType();
    }
    if (paramType instanceof Class<?>) {
      componentType = (Class<?>)paramType;
    } else {
      throw new UnsupportedOperationException("Currently types like \"" + type + "\" are not supported");
    }

    return Pair.<Class<?>, Type>of(componentType, componentGenericType);
  }

  private static Type extractUpperBoundFromType(Type type) {
    if (type instanceof WildcardType) {
      WildcardType wcType = (WildcardType)type;
      if (wcType.getLowerBounds().length > 0) {
        throw new UnsupportedOperationException("Currently wildcard types with a lower bound like \"" + type + "\" are not supported ");
      }
      Type[] upperBounds = ((WildcardType)type).getUpperBounds();
      if (upperBounds.length != 1) {
        throw new UnsupportedOperationException("Currently only type bounds with one upper bound are supported, not \"" + type + "\"");
      }
      type = upperBounds[0];
    }
    return type;
  }

  private static void checkRange(String name, Object value, Annotation secondaryOption) throws InvalidConfigurationException {
    if (secondaryOption != null) {

      if (!(secondaryOption instanceof IntegerOption)
          || !(value instanceof Integer || value instanceof Long)) {
        throw new UnsupportedOperationException("Option " + name + " may not be annotated with " + secondaryOption);
      }

      IntegerOption intOption = (IntegerOption)secondaryOption;

      long n = ((Number)value).longValue();
      if (intOption.min() > n || n > intOption.max()) {
        throw new InvalidConfigurationException("Invalid value in configuration file: \""
            + name + " = " + value + '\"'
            + " (not in range [" + intOption.min() + ", " + intOption.max() + "])");
      }
    }
  }

  public String getRootDirectory() {
    return this.rootDirectory;
  }

  public String getOutputDirectory() {
    return disableOutput ? null : outputDirectory;
  }

}
