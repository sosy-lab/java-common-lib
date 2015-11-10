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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verifyNotNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.io.Resources;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/** This class collects all {@link Option}s of a program. */
public class OptionCollector {

  private static final Pattern IGNORED_CLASSES =
      Pattern.compile("^org\\.sosy_lab\\.common\\..*Test(\\$.*)?$");
  private static final int CHARS_PER_LINE = 75; // for description

  /** The main-method collects all classes of a program and
   * then it searches for all {@link Option}s.
   *
   * @param args use '-v' for verbose output */
  public static void main(final String[] args) {

    // parse args
    boolean verbose = false;
    for (String arg : args) {
      if ("-v".equals(arg) || "-verbose".equals(arg)) {
        verbose = true;
      }
    }

    System.out.println(getCollectedOptions(verbose));
  }

  /** This function collect options from all classes
   * and return a formatted String.
   *
   * @param verbose short or long output? */
  public static String getCollectedOptions(final boolean verbose) {
    return new OptionCollector(verbose).getCollectedOptions();
  }

  private final Set<String> errorMessages = new LinkedHashSet<>();
  private final boolean verbose;

  // The map where we will collect all options.
  // TreeMap for alphabetical order of keys
  private final Multimap<String, AnnotationInfo> options =
      Multimaps.newMultimap(
          new TreeMap<String, Collection<AnnotationInfo>>(),
          new Supplier<Collection<AnnotationInfo>>() {
            @Override
            public Collection<AnnotationInfo> get() {
              return new ArrayList<>();
            }
          });

  /**
   * @param pVerbose short or long output?
   */
  public OptionCollector(boolean pVerbose) {
    verbose = pVerbose;
  }

  /**
   * This function collects options from all classes
   * and returns a formatted String.
   */
  public String getCollectedOptions() {
    // redirect stdout to stderr so that error messages that are printed
    // when classes are loaded appear in stderr
    PrintStream originalStdOut = System.out;
    System.setOut(System.err);

    boolean appendCommonOptions = true;

    for (Class<?> c : getClasses()) {
      if (c.isAnnotationPresent(Options.class)) {
        collectOptions(c);
      }
      if (c.getPackage() != null && c.getPackage().getName().startsWith("org.sosy_lab.common")) {
        appendCommonOptions = false;
      }
    }

    // reset stdout redirection
    System.setOut(originalStdOut);

    for (String error : errorMessages) {
      System.err.println(error);
    }

    final StringBuilder content = new StringBuilder();

    // add options of this library
    if (appendCommonOptions) {
      try {
        URL resource = Resources.getResource("org/sosy_lab/common/ConfigurationOptions.txt");
        content.append(Resources.toString(resource, StandardCharsets.UTF_8));
      } catch (Exception e) {
        System.err.println("Could not find options of org.sosy-lab.common classes: "
            + e.getMessage());
      }
    }

    // Dump all options, avoiding repeated information.
    String lastDescription = "";
    String lastInfo = "";
    for (Collection<AnnotationInfo> allInstances : options.asMap().values()) {
      boolean first = true;
      for (AnnotationInfo annotation : allInstances) {
        String description = getOptionDescription(annotation.element());
        if (!description.isEmpty() && !lastDescription.equals(description)) {
          if (first) {
            content.append("\n");
            first = false;
          }
          content.append(description);
          lastDescription = description;
        }
      }
      for (OptionInfo option : from(allInstances).filter(OptionInfo.class)) {
        String infoText = getOptionInfo(option);
        if (!lastInfo.equals(infoText)) {
          content.append(infoText);
          lastInfo = infoText;
        }
      }
    }

    return content.toString();
  }

  /** This method tries to get Source-Path. This path is used
   * to get default values for options without instantiating the classes. */
  private static String getSourcePath(Class<?> cls) {
    String sourcePath = cls.getProtectionDomain().getCodeSource().getLocation().getPath();

    // in case we have spaces in the filename these have to be fixed:
    sourcePath = sourcePath.replace("%20", " ");

    // check the folders known as source, depending on the current folder
    // structure for the class files

    // this could be a usual eclipse environment, therefore src is the appropriate
    // folder to search for sources
    if (sourcePath.endsWith("bin/")) {
      sourcePath = sourcePath.substring(0, sourcePath.length() - 4);

      // this is a typical project layout for gradle, the sources should be in
      // src/main/java/
    } else if (sourcePath.endsWith("build/classes/main/")) {
      sourcePath = sourcePath.substring(0, sourcePath.length() - 19);
    }

    // gradle projects do also in eclipse have another folder for sources
    // so check which folder is the actual source folder
    if (new File(sourcePath + "src/main/java/").isDirectory()) {
      return sourcePath + "src/main/java/";
    } else if (new File(sourcePath + "src/").isDirectory()) {
      return sourcePath + "src/";
    }

    return "";
  }

  /** This function returns the contextClassLoader-Resources. */
  @Nullable
  private static Iterator<URL> getClassLoaderResources() {
    final ClassLoader classLoader =
        Thread.currentThread().getContextClassLoader();
    assert classLoader != null;

    try {
      return Iterators.forEnumeration(classLoader.getResources(""));
    } catch (IOException e) {
      System.err.println("Could not get recources of classloader.");
    }
    return Collections.emptyIterator();
  }

  /** This method collects every {@link Option} of a class.
   *
   * @param c class where to take the Option from
   */
  private void collectOptions(final Class<?> c) {
    String classSource = getContentOfFile(c);

    final Options classOption = c.getAnnotation(Options.class);
    verifyNotNull(classOption, "Class without @Options annotation");
    options.put(classOption.prefix(), OptionsInfo.create(c));

    for (final Field field : c.getDeclaredFields()) {
      if (field.isAnnotationPresent(Option.class)) {
        Option option = field.getAnnotation(Option.class);
        final String optionName = Configuration.getOptionName(classOption, field, option);
        final String defaultValue = getDefaultValue(field, classSource);
        options.put(optionName, OptionInfo.createForField(field, optionName, defaultValue));
      }
    }

    for (final Method method : c.getDeclaredMethods()) {
      if (method.isAnnotationPresent(Option.class)) {
        Option option = method.getAnnotation(Option.class);
        final String optionName = Configuration.getOptionName(classOption, method, option);
        options.put(optionName, OptionInfo.createForMethod(method, optionName));
      }
    }
  }

  /** This function returns the formatted description of an {@link Option}.
   *
   * @param element field with the option */
  static String getOptionDescription(final AnnotatedElement element) {
    String text;
    if (element.isAnnotationPresent(Option.class)) {
      text = element.getAnnotation(Option.class).description();
    } else if (element.isAnnotationPresent(Options.class)) {
      text = element.getAnnotation(Options.class).description();
    } else {
      throw new AssertionError();
    }

    if (element.isAnnotationPresent(Deprecated.class)) {
      text = "DEPRECATED: " + text;
    }

    return formatText(text);
  }

  /** This function returns the formatted information about an {@link Option}. */
  private String getOptionInfo(OptionInfo info) {
    final StringBuilder optionInfo = new StringBuilder();
    optionInfo.append(info.name());

    if (verbose) {
      if (info.element() instanceof Field) {
        optionInfo.append("\n  field:    " + ((Field) info.element()).getName() + "\n");
      } else if (info.element() instanceof Method) {
        optionInfo.append("\n  method:   " + ((Method) info.element()).getName() + "\n");
      }

      Class<?> cls = ((Member) info.element()).getDeclaringClass();
      optionInfo.append("  class:    " + cls.toString().substring(6) + "\n");
      optionInfo.append("  type:     " + info.type().getSimpleName() + "\n");
      optionInfo.append("  default value: ");
      if (!info.defaultValue().isEmpty()) {
        optionInfo.append(info.defaultValue());
      } else {
        optionInfo.append("not available");
      }

    } else {
      if (!info.defaultValue().isEmpty()) {
        optionInfo.append(" = " + info.defaultValue());
      } else {
        optionInfo.append(" = no default value");
      }
    }
    optionInfo.append("\n");
    optionInfo.append(getAllowedValues(info.element(), info.type(), verbose));

    return optionInfo.toString();
  }

  /** This function formats text and splits lines, if they are too long.
   * This functions adds "#" before each line.*/
  private static String formatText(final String text) {
    return formatText(text, "# ", true);
  }

  /** This function formats text and splits lines, if they are too long. */
  private static String formatText(
      final String text, final String lineStart, final boolean useLineStartInFirstLine) {
    checkNotNull(lineStart);
    if (text.isEmpty()) {
      return text;
    }

    // split description into lines
    final String[] lines = text.split("\n");

    // split lines into more lines, if they are too long
    final List<String> splittedLines = new ArrayList<>();
    for (final String fullLine : lines) {
      String remainingLine = fullLine;
      while (remainingLine.length() > CHARS_PER_LINE) {

        int spaceIndex = remainingLine.lastIndexOf(" ", CHARS_PER_LINE);
        if (spaceIndex == -1) {
          spaceIndex = remainingLine.indexOf(" ");
        }
        if (spaceIndex == -1) {
          spaceIndex = remainingLine.length() - 1;
        }

        final String start = remainingLine.substring(0, spaceIndex);
        if (!start.isEmpty()) {
          splittedLines.add(start);
        }
        remainingLine = remainingLine.substring(spaceIndex + 1);
      }
      splittedLines.add(remainingLine);
    }

    // remove last element, if empty (useful if previous line is too long)
    if (splittedLines.get(splittedLines.size() - 1).isEmpty()) {
      splittedLines.remove(splittedLines.size() - 1);
    }

    // add "# " before each line
    StringBuilder formattedLines = new StringBuilder();
    if (!useLineStartInFirstLine && splittedLines.size() > 0) {
      formattedLines.append(splittedLines.remove(0));
      formattedLines.append('\n');
    }
    for (String line : splittedLines) {
      formattedLines.append(lineStart);
      formattedLines.append(line);
      formattedLines.append('\n');
    }

    return formattedLines.toString();
  }

  /** This function searches for the default field value of an {@link Option}
   * in the sourcefile of the actual field/class and returns it
   * or an emtpy String, if the value not found.
   *
   * This part only works, if you have the source code.
   *
   * @param field where to get the default value */
  private static String getDefaultValue(final Field field, final String classSource) {

    // get declaration of field from file
    // example fieldString: 'private boolean shouldCheck'
    String fieldString = Modifier.toString(field.getModifiers());

    // genericType: "boolean" or "java.util.List<java.util.logging.Level>"
    String typeString = field.getGenericType().toString();
    if (typeString.matches(".*<.*>")) {

      // remove package-definition at front:
      // java.util.List<?> --> List<?>
      typeString = typeString.replaceAll("^[^<]*\\.", "");

      // remove package-definition in middle:
      // List<package.name.X> --> List<X>
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

    fieldString += "\\s+" + typeString;
    fieldString += "\\s+" + field.getName();

    String defaultValue = getDefaultValueFromContent(classSource, fieldString);

    // enums can be written with the whole classname, example:
    // 'Waitlist.TraversalMethod traversalMethod = ...;'
    // then fieldString is different.
    if (field.getType().isEnum()) {
      if (defaultValue.isEmpty()) {
        String type = field.getType().toString();
        type = type.substring(type.lastIndexOf(".") + 1).replace("$", ".");
        fieldString =
            Modifier.toString(field.getModifiers()) + "\\s+" + type + "\\s+"
                + field.getName();
        defaultValue = getDefaultValueFromContent(classSource, fieldString);
      }
      if (defaultValue.contains(".")) {
        defaultValue =
            defaultValue.substring(defaultValue.lastIndexOf(".") + 1);
      }
    }

    if (defaultValue.equals("null")) { // do we need this??
      defaultValue = "";
    }
    return defaultValue;
  }

  /** This function returns the content of a sourcefile as String.
   *
   * @param cls the class whose sourcefile should be retrieved
   */
  private String getContentOfFile(final Class<?> cls) {

    // get name of sourcefile
    String filename = cls.getName().replace(".", "/");

    // encapsulated classes have a "$" in filename
    if (filename.contains("$")) {
      filename = filename.substring(0, filename.indexOf("$"));
    }

    // get name of source file
    filename = getSourcePath(cls) + filename + ".java";

    try {
      return com.google.common.io.Files.toString(new File(filename),
          Charset.defaultCharset());
    } catch (IOException e) {
      errorMessages.add("INFO: Could not read sourcefiles "
          + "for getting the default values.");
      return "";
    }
  }

  /** This function searches for fieldstring in content and
   * returns the value of the field.
   *
   * @param content sourcecode where to search
   * @param fieldPattern regexp specifying the name of the field, whose value is returned
   */
  private static String getDefaultValueFromContent(final String content,
      final String fieldPattern) {
    // search for fieldString and get the whole content after it (=rest),
    // in 'rest' search for ';' and return all before it (=defaultValue)
    String defaultValue = "";
    String[] splitted = content.split(fieldPattern);
    if (splitted.length > 1) { // first part is before fieldString, second part is after it
      final String rest = splitted[1];
      defaultValue =
          rest.substring(0, rest.indexOf(";")).trim();

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
        defaultValue = stripSurroundingFunctionCall(defaultValue,
            "PathCounterTemplate.ofFormatString");

        if (defaultValue.startsWith("TimeSpan.ofNanos(")) {
          defaultValue = defaultValue.substring(
              "TimeSpan.ofNanos(".length(), defaultValue.length() - 1) + "ns";
        }
        if (defaultValue.startsWith("TimeSpan.ofMillis(")) {
          defaultValue = defaultValue.substring(
              "TimeSpan.ofMillis(".length(), defaultValue.length() - 1) + "ms";
        }
        if (defaultValue.startsWith("TimeSpan.ofSeconds(")) {
          defaultValue = defaultValue.substring(
              "TimeSpan.ofSeconds(".length(), defaultValue.length() - 1) + "s";
        }

        if (defaultValue.startsWith("ImmutableSet.of(")) {
          defaultValue = "{" + defaultValue.substring(
              "ImmutableSet.of(".length(), defaultValue.length() - 1) + "}";
        }

        if (defaultValue.startsWith("ImmutableList.of(")) {
          defaultValue = "[" + defaultValue.substring(
              "ImmutableList.of(".length(), defaultValue.length() - 1) + "]";
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
   * If the string matches something like "<func>(<args>)" for a given <func>,
   * return only the <args> part, otherwise the full string.
   */
  private static String stripSurroundingFunctionCall(String s, String partToBeStripped) {
    String toBeStripped = partToBeStripped + "(";
    if (s.startsWith(toBeStripped)) {
      return s.substring(toBeStripped.length(), s.length() - 1);
    }
    return s;
  }

  /** This function returns the allowed values or interval for a field.
   *
   * @param field field with the {@link Option}-annotation
   * @param verbose short or long output? */
  private static String getAllowedValues(
      final AnnotatedElement field, final Class<?> type, final boolean verbose) {
    String allowedValues = "";

    // if the type is enum,
    // the allowed values can be extracted the enum-class
    if (type.isEnum()) {
      final Object[] enums = type.getEnumConstants();
      final String[] enumTitles = new String[enums.length];
      for (int i = 0; i < enums.length; i++) {
        enumTitles[i] = ((Enum<?>) enums[i]).name();
      }
      allowedValues = "  enum:     "
          + formatText(java.util.Arrays.toString(enumTitles), "             ", false);
    }

    allowedValues += getOptionValues(field, verbose);
    allowedValues += getClassOptionValues(field, verbose);
    allowedValues += getFileOptionValues(field, verbose);
    allowedValues += getIntegerOptionValues(field, verbose);
    allowedValues += getTimeSpanOptionValues(field, verbose);

    return allowedValues;
  }

  /** This method returns text representing the values,
   * that are defined in the {@link Option}-annotation. */
  private static String getOptionValues(AnnotatedElement field, boolean verbose) {
    final Option option = field.getAnnotation(Option.class);
    assert option != null;
    String str = "";
    if (option.values().length != 0) {
      str += "  allowed values: "
          + java.util.Arrays.toString(option.values()) + "\n";
    }

    if (verbose && !option.regexp().isEmpty()) {
      str += "  regexp:   " + option.regexp() + "\n";
    }

    if (verbose && option.toUppercase()) {
      str += "  uppercase: true\n";
    }
    return str;
  }

  /** This method returns text representing the values,
   * that are defined in the {@link ClassOption}-annotation. */
  private static String getClassOptionValues(AnnotatedElement field, boolean verbose) {
    final ClassOption classOption = field.getAnnotation(ClassOption.class);
    String str = "";
    if (classOption != null) {
      if (verbose && classOption.packagePrefix().length != 0) {
        str += "  packagePrefix: " + Joiner.on(", ").join(classOption.packagePrefix()) + "\n";
      }
    }
    return str;
  }

  /** This method returns text representing the values,
   * that are defined in the {@link FileOption}-annotation. */
  private static String getFileOptionValues(AnnotatedElement field, boolean verbose) {
    final FileOption fileOption = field.getAnnotation(FileOption.class);
    String str = "";
    if (fileOption != null) {
      if (verbose) {
        str += "  type of file: " + fileOption.value() + "\n";
      }
    }
    return str;
  }

  /** This method returns text representing the values,
   * that are defined in the {@link IntegerOption}-annotation. */
  private static String getIntegerOptionValues(AnnotatedElement field, boolean verbose) {
    final IntegerOption intOption = field.getAnnotation(IntegerOption.class);
    String str = "";
    if (intOption != null) {
      if (verbose) {
        if (intOption.min() == Long.MIN_VALUE) {
          str += "  min:      Long.MIN_VALUE\n";
        } else {
          str += "  min:      " + intOption.min() + "\n";
        }
        if (intOption.max() == Long.MAX_VALUE) {
          str += "  max:      Long.MAX_VALUE\n";
        } else {
          str += "  max:      " + intOption.max() + "\n";
        }
      }
    }
    return str;
  }

  /** This method returns text representing the values,
   * that are defined in the {@link TimeSpanOption}-annotation. */
  private static String getTimeSpanOptionValues(AnnotatedElement field, boolean verbose) {
    final TimeSpanOption timeSpanOption = field.getAnnotation(TimeSpanOption.class);
    String str = "";
    if (timeSpanOption != null) {
      if (verbose) {
        str += "  code unit:     " + timeSpanOption.codeUnit() + "\n";
        str += "  default unit:  " + timeSpanOption.defaultUserUnit() + "\n";
        if (timeSpanOption.min() == Long.MIN_VALUE) {
          str += "  time min:      Long.MIN_VALUE\n";
        } else {
          str += "  time min:      " + timeSpanOption.min() + "\n";
        }
        if (timeSpanOption.max() == Long.MAX_VALUE) {
          str += "  time max:      Long.MAX_VALUE\n";
        } else {
          str += "  time max:      " + timeSpanOption.max() + "\n";
        }
      }
    }
    return str;
  }

  /**
   * Collects classes accessible from the context class loader.
   * Ignores classes that are packaged inside JARs, certain blacklisted classes,
   * and interfaces.
   * @return list of classes
   */
  private List<Class<?>> getClasses() {
    final ClassPath classPath;
    try {
      classPath = ClassPath.from(Thread.currentThread().getContextClassLoader());
    } catch (IOException e) {
      errorMessages.add(
          "INFO: Could not scan class path for getting Option annotations: " + e.getMessage());
      return ImmutableList.of();
    }

    final List<Class<?>> classes = new ArrayList<>();

    for (ClassInfo cls : classPath.getAllClasses()) {
      // Ignore classes in JAR files etc, we want only classes of this project.
      if (!cls.url().getProtocol().equals("file")) {
        continue;
      }
      if (IGNORED_CLASSES.matcher(cls.getName()).matches()) {
        continue;
      }
      final Class<?> foundClass;

      try {
        foundClass = cls.load();
      } catch (LinkageError e) {
        // Because ClassInfo.load() does not link or initialize the class
        // like Class.forName() does, most common problems with class loading
        // actually never occur, e.g., ExceptionInInitializerError and UnsatisfiedLinkError.
        // Currently no case is know why a LinkageError would occur..
        errorMessages.add(
            String.format(
                "INFO: Could not load '%s' for getting Option annotations: %s: %s",
                cls.getResourceName(),
                e.getClass().getName(),
                e.getMessage()));
        continue;
      }

      if (Modifier.isInterface(foundClass.getModifiers())) {
        continue; // ignore interfaces
      }
      classes.add(foundClass);
    }
    return classes;
  }

  private static abstract class AnnotationInfo {

    /**
     * The annotated element or class.
     */
    abstract AnnotatedElement element();
  }

  @AutoValue
  static abstract class OptionInfo extends AnnotationInfo {

    static OptionInfo createForField(Field field, String name, String defaultValue) {
      return new AutoValue_OptionCollector_OptionInfo(field, name, field.getType(), defaultValue);
    }

    static OptionInfo createForMethod(Method method, String name) {
      // methods with @Option have no usable default value
      return new AutoValue_OptionCollector_OptionInfo(method, name, method.getReturnType(), "");
    }

    @Override
    abstract AnnotatedElement element();

    abstract String name();

    abstract Class<?> type();

    abstract String defaultValue();
  }

  @AutoValue
  static abstract class OptionsInfo extends AnnotationInfo {

    static OptionsInfo create(Class<?> c) {
      return new AutoValue_OptionCollector_OptionsInfo(c);
    }

    @Override
    abstract Class<?> element();
  }
}
