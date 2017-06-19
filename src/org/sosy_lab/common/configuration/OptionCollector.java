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

import static com.google.common.base.Verify.verifyNotNull;

import com.google.auto.value.AutoValue;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.io.MoreFiles;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** This class collects all {@link Option}s of a program. */
public class OptionCollector {

  private static final String OPTIONS_FILE = "ConfigurationOptions.txt";
  private static final Pattern IGNORED_CLASSES =
      Pattern.compile("^org\\.sosy_lab\\.common\\..*Test(\\$.*)?$");

  private static final Pattern IMMUTABLE_SET_PATTERN =
      Pattern.compile("ImmutableSet\\.(<.*>)?of\\((.*)\\)", Pattern.DOTALL);
  private static final Pattern IMMUTABLE_LIST_PATTERN =
      Pattern.compile("ImmutableList\\.(<.*>)?of\\((.*)\\)", Pattern.DOTALL);

  /**
   * The main-method collects all classes of a program and then it searches for all {@link Option}s.
   *
   * @param args use '-v' for verbose output
   */
  public static void main(final String[] args) {

    // parse args
    boolean verbose = false;
    boolean includeLibraryOptions = false;
    for (String arg : args) {
      if ("-v".equals(arg) || "-verbose".equals(arg)) {
        verbose = true;
      } else if ("-includeLibraryOptions".equals(arg)) {
        includeLibraryOptions = true;
      }
    }

    collectOptions(verbose, includeLibraryOptions, System.out);
  }

  /**
   * This function collects options from all classes and outputs them. Error message are written to
   * System.err.
   *
   * @param verbose short or long output?
   * @param includeLibraryOptions whether options defined by libraries on the classpath should be
   *     included
   * @param out the output target
   */
  public static void collectOptions(
      final boolean verbose, final boolean includeLibraryOptions, final PrintStream out) {
    OptionCollector optionCollector = new OptionCollector(verbose, includeLibraryOptions);
    try {
      optionCollector.collectOptions(out);
    } finally {
      optionCollector.errorMessages.forEach(System.err::println);
    }
  }

  private final Set<String> errorMessages = Collections.synchronizedSet(new LinkedHashSet<>());

  private final LoadingCache<CodeSource, Path> codeSourceToSourcePath =
      CacheBuilder.newBuilder()
          // Most projects have only one source folder and thus we expect only one entry here.
          .initialCapacity(1)
          .concurrencyLevel(1)
          .build(
              new CacheLoader<CodeSource, Path>() {
                @Override
                public Path load(CodeSource codeSource) throws URISyntaxException {
                  return getSourcePath(codeSource);
                }
              });

  private final boolean verbose;
  private final boolean includeLibraryOptions;

  private OptionCollector(boolean pVerbose, boolean pIncludeLibraryOptions) {
    verbose = pVerbose;
    includeLibraryOptions = pIncludeLibraryOptions;
  }

  /** This function collects options from all classes and writes them to the output. */
  private void collectOptions(final PrintStream out) {
    ClassPath classPath;
    try {
      classPath = ClassPath.from(Thread.currentThread().getContextClassLoader());
    } catch (IOException e) {
      errorMessages.add(
          "INFO: Could not scan class path for getting Option annotations: " + e.getMessage());
      return;
    }

    if (includeLibraryOptions) {
      copyOptionFilesToOutput(classPath, out);
    }

    // We want a deterministic ordering for cases where the same option is declared multiple times.
    Comparator<AnnotationInfo> annotationComparator =
        Comparator.comparing(a -> a.owningClass().getName());

    OptionPlainTextWriter outputWriter = new OptionPlainTextWriter(verbose, out);

    // Collect and dump all options
    getClassesWithOptions(classPath)
        .flatMap(this::collectOptions)
        .collect(groupingBySorted(AnnotationInfo::name, Ordering.natural(), annotationComparator))
        .values()
        .forEach(outputWriter::writeOption);
  }

  /** Copy files with options documentation found on the class path to the output. */
  private void copyOptionFilesToOutput(ClassPath classPath, final PrintStream out) {
    for (ClassPath.ResourceInfo resourceInfo : classPath.getResources()) {
      if (new File(resourceInfo.getResourceName()).getName().equals(OPTIONS_FILE)) {
        try {
          resourceInfo.asCharSource(StandardCharsets.UTF_8).copyTo(out);
        } catch (IOException e) {
          errorMessages.add("Could not find the required resource " + resourceInfo.url());
        }
      }
    }
  }

  /**
   * Collects classes with options from the given {@link ClassPath}. Ignores classes that do not
   * have file:// URLs (e.g., packaged inside JARs) certain blacklisted classes, and interfaces,
   * classes without options.
   *
   * @return stream of classes with options
   */
  private Stream<Class<?>> getClassesWithOptions(ClassPath classPath) {
    return classPath
        .getAllClasses()
        .parallelStream()
        .filter(clsInfo -> clsInfo.url().getProtocol().equals("file"))
        .filter(clsInfo -> !IGNORED_CLASSES.matcher(clsInfo.getName()).matches())
        .flatMap(this::tryLoadClass)
        .filter(cls -> !Modifier.isInterface(cls.getModifiers())) // ignore interfaces
        .filter(cls -> cls.isAnnotationPresent(Options.class));
  }

  private Stream<Class<?>> tryLoadClass(ClassInfo cls) {
    try {
      return Stream.of(cls.load());
    } catch (LinkageError e) {
      // Because ClassInfo.load() does not link or initialize the class
      // like Class.forName() does, most common problems with class loading
      // actually never occur, e.g., ExceptionInInitializerError and UnsatisfiedLinkError.
      // Currently no case is known why a LinkageError would occur..
      errorMessages.add(
          String.format(
              "INFO: Could not load '%s' for getting Option annotations: %s: %s",
              cls.getResourceName(), e.getClass().getName(), e.getMessage()));
      return Stream.empty();
    }
  }

  /**
   * This method collects every {@link Option} of a class.
   *
   * @param c class where to take the Option from
   */
  private Stream<AnnotationInfo> collectOptions(final Class<?> c) {
    Stream.Builder<AnnotationInfo> result = Stream.builder();
    String classSource = getSourceCode(c);

    final Options classOption = c.getAnnotation(Options.class);
    verifyNotNull(classOption, "Class without @Options annotation");
    result.accept(OptionsInfo.create(c, classOption.prefix()));

    for (final Field field : c.getDeclaredFields()) {
      if (field.isAnnotationPresent(Option.class)) {
        Option option = field.getAnnotation(Option.class);
        final String optionName = Configuration.getOptionName(classOption, field, option);
        final String defaultValue = getDefaultValue(field, classSource);
        result.accept(OptionInfo.createForField(field, optionName, defaultValue));
      }
    }

    for (final Method method : c.getDeclaredMethods()) {
      if (method.isAnnotationPresent(Option.class)) {
        Option option = method.getAnnotation(Option.class);
        final String optionName = Configuration.getOptionName(classOption, method, option);
        result.accept(OptionInfo.createForMethod(method, optionName));
      }
    }
    return result.build();
  }

  /**
   * This function returns the content of a sourcefile as String.
   *
   * @param cls the class whose sourcefile should be retrieved
   */
  private String getSourceCode(final Class<?> cls) {
    // get name of sourcefile
    String filename = cls.getName().replace('.', File.separatorChar);

    // encapsulated classes have a "$" in filename
    if (filename.contains("$")) {
      filename = filename.substring(0, filename.indexOf('$'));
    }
    filename += ".java";

    try {
      Path path = codeSourceToSourcePath.get(cls.getProtectionDomain().getCodeSource());
      return MoreFiles.asCharSource(path.resolve(filename), StandardCharsets.UTF_8).read();
    } catch (ExecutionException | IOException e) {
      // Do not print exception message to avoid having a different error message for each file.
      errorMessages.add(
          "INFO: Could not find source files for classes in "
              + cls.getProtectionDomain().getCodeSource().getLocation());
      return "";
    }
  }

  /**
   * This method tries to get Source-Path. This path is used to get default values for options
   * without instantiating the classes.
   */
  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  private static Path getSourcePath(CodeSource codeSource) throws URISyntaxException {
    // Get base folder for classes, go via URI to handle escaping
    Path basePath = Paths.get(codeSource.getLocation().toURI());

    // check the folders known as source, depending on the current folder
    // structure for the class files

    if (basePath.endsWith("bin")) {
      // this could be a usual eclipse environment, therefore src is the appropriate
      // folder to search for sources
      basePath = basePath.getParent();

    } else if (basePath.endsWith("build/classes/main")) {
      // this is a typical project layout for gradle, the sources should be in
      // src/main/java/
      basePath = basePath.getParent().getParent().getParent();
    }

    // gradle projects do also in eclipse have another folder for sources
    // so check which folder is the actual source folder
    List<Path> candidates = ImmutableList.of(Paths.get("src", "main", "java"), Paths.get("src"));
    for (Path candidate : candidates) {
      Path sourcePath = basePath.resolve(candidate);
      if (Files.isDirectory(sourcePath)) {
        return sourcePath;
      }
    }
    return basePath;
  }

  /**
   * This function searches for the default field value of an {@link Option} in the sourcefile of
   * the actual field/class and returns it or an emtpy String, if the value not found.
   *
   * <p>This part only works, if you have the source code.
   *
   * @param field where to get the default value
   */
  private static String getDefaultValue(final Field field, final String classSource) {
    // genericType: "boolean" or "java.util.List<java.util.logging.Level>"
    String typeString = field.getGenericType().toString();
    if (typeString.matches(".*<.*>")) {

      // remove package-definition at front:
      // java.util.List<?> --> List<?>
      typeString = typeString.replaceAll("^[^<]*\\.", "");

      // remove package-definition in middle:
      // List<package.name.X> --> Li)st<X>
      // (without special case "? extends X", that is handled below)
      typeString = typeString.replaceAll("<[^\\?][^<]*\\.", "<");

      // remove package-definition in middle:
      // List<? extends package.name.X> --> List<? extends X>
      // we use the string as regex later, so we use 4 "\" in the string.
      typeString = typeString.replaceAll("<\\?[^<]*\\.", "<\\\\?\\\\s+extends\\\\s+");

    } else {
      typeString = field.getType().getSimpleName();
    }

    // remove prefix of inner classes
    typeString = typeString.replaceAll("[^<>, ]*\\$([^<>, $]*)", "$1");

    String defaultValue =
        getDefaultValueFromContent(classSource, getFieldMatchingPattern(field, typeString));

    // enums can be written with the whole classname, example:
    // 'Waitlist.TraversalMethod traversalMethod = ...;'
    // then fieldString is different.
    if (field.getType().isEnum()) {
      if (defaultValue.isEmpty()) {
        String type = field.getType().toString();
        type = type.substring(type.lastIndexOf('.') + 1).replace("$", ".");
        defaultValue =
            getDefaultValueFromContent(classSource, getFieldMatchingPattern(field, type));
      }
      if (defaultValue.contains(".")) {
        defaultValue = defaultValue.substring(defaultValue.lastIndexOf('.') + 1);
      }
    }

    if (defaultValue.equals("null")) { // do we need this??
      defaultValue = "";
    }
    return defaultValue;
  }

  /**
   * Get pattern for matching a field declaration in a source file. Example: 'private boolean
   * shouldCheck'
   */
  private static String getFieldMatchingPattern(final Field field, String type) {
    return Modifier.toString(field.getModifiers()) + "\\s+" + type + "\\s+" + field.getName();
  }

  /**
   * This function searches for fieldstring in content and returns the value of the field.
   *
   * @param content sourcecode where to search
   * @param fieldPattern regexp specifying the name of the field, whose value is returned
   */
  private static String getDefaultValueFromContent(
      final String content, final String fieldPattern) {
    // search for fieldString and get the whole content after it (=rest),
    // in 'rest' search for ';' and return all before it (=defaultValue)
    String defaultValue = "";
    String[] splitted = content.split(fieldPattern);
    if (splitted.length > 1) { // first part is before fieldString, second part is after it
      final String rest = splitted[1];
      defaultValue = rest.substring(0, rest.indexOf(';')).trim();

      // remove unnecessary parts of field
      if (defaultValue.startsWith("=")) {
        defaultValue = defaultValue.substring(1).trim();

        // remove comments
        while (defaultValue.contains("/*")) {
          defaultValue =
              defaultValue.substring(0, defaultValue.indexOf("/*"))
                  + defaultValue.substring(defaultValue.indexOf("*/") + 2);
        }
        if (defaultValue.contains("//")) {
          defaultValue = defaultValue.substring(0, defaultValue.indexOf("//"));
        }

        // remove brackets from file: new File("example.txt") --> "example.txt"
        defaultValue = stripSurroundingFunctionCall(defaultValue, "new File");
        defaultValue = stripSurroundingFunctionCall(defaultValue, "Paths.get");
        defaultValue = stripSurroundingFunctionCall(defaultValue, "new Path");
        defaultValue = stripSurroundingFunctionCall(defaultValue, "Pattern.compile");
        defaultValue = stripSurroundingFunctionCall(defaultValue, "PathTemplate.ofFormatString");
        defaultValue =
            stripSurroundingFunctionCall(defaultValue, "PathCounterTemplate.ofFormatString");

        if (defaultValue.startsWith("TimeSpan.ofNanos(")) {
          defaultValue =
              defaultValue.substring("TimeSpan.ofNanos(".length(), defaultValue.length() - 1)
                  + "ns";
        }
        if (defaultValue.startsWith("TimeSpan.ofMillis(")) {
          defaultValue =
              defaultValue.substring("TimeSpan.ofMillis(".length(), defaultValue.length() - 1)
                  + "ms";
        }
        if (defaultValue.startsWith("TimeSpan.ofSeconds(")) {
          defaultValue =
              defaultValue.substring("TimeSpan.ofSeconds(".length(), defaultValue.length() - 1)
                  + "s";
        }

        Matcher match = IMMUTABLE_SET_PATTERN.matcher(defaultValue);
        if (match.matches()) {
          defaultValue = "{" + match.group(2) + "}";
        }
        match = IMMUTABLE_LIST_PATTERN.matcher(defaultValue);
        if (match.matches()) {
          defaultValue = "[" + match.group(2) + "]";
        }
      }
    } else {

      // special handling for generics
      final String stringSetFieldPattern =
          fieldPattern.replace("\\s+Set\\s+", "\\s+Set<String>\\s+");
      if (content.contains(stringSetFieldPattern)) {
        return getDefaultValueFromContent(content, stringSetFieldPattern);
      }
      // TODO: other types of generics?
    }
    return defaultValue.trim();
  }

  /**
   * If the string matches something like "<func>(<args>)" for a given <func>, return only the
   * <args> part, otherwise the full string.
   */
  private static String stripSurroundingFunctionCall(String s, String partToBeStripped) {
    String toBeStripped = partToBeStripped + "(";
    if (s.startsWith(toBeStripped)) {
      return s.substring(toBeStripped.length(), s.length() - 1);
    }
    return s;
  }

  /**
   * Return a collector that groups values by keys into a map, and sorts both keys and the values
   * per key.
   */
  private static <T, K> Collector<T, ?, SortedMap<K, List<T>>> groupingBySorted(
      Function<? super T, ? extends K> classifier,
      Comparator<? super K> keyComparator,
      Comparator<? super T> valueComparator) {
    Function<List<T>, List<T>> listSortFinisher =
        list -> {
          list.sort(valueComparator);
          return list;
        };
    Collector<T, ?, List<T>> toSortedList =
        Collectors.collectingAndThen(Collectors.toCollection(ArrayList::new), listSortFinisher);
    return Collectors.groupingBy(classifier, () -> new TreeMap<>(keyComparator), toSortedList);
  }

  abstract static class AnnotationInfo {

    /** The annotated element or class. */
    abstract AnnotatedElement element();

    /** The name for this annotation. */
    abstract String name();

    abstract Class<?> owningClass();
  }

  @AutoValue
  abstract static class OptionInfo extends AnnotationInfo {

    static OptionInfo createForField(Field field, String name, String defaultValue) {
      return new AutoValue_OptionCollector_OptionInfo(field, name, field.getType(), defaultValue);
    }

    static OptionInfo createForMethod(Method method, String name) {
      // methods with @Option have no usable default value
      return new AutoValue_OptionCollector_OptionInfo(method, name, method.getReturnType(), "");
    }

    abstract Class<?> type();

    abstract String defaultValue();

    @Override
    final Class<?> owningClass() {
      return ((Member) element()).getDeclaringClass();
    }
  }

  @AutoValue
  abstract static class OptionsInfo extends AnnotationInfo {

    static OptionsInfo create(Class<?> c, String prefix) {
      return new AutoValue_OptionCollector_OptionsInfo(prefix, c);
    }

    @Override
    abstract Class<?> element();

    @Override
    final Class<?> owningClass() {
      return element();
    }
  }
}
