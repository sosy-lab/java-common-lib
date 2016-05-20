/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;

import org.sosy_lab.common.configuration.OptionCollector.AnnotationInfo;
import org.sosy_lab.common.configuration.OptionCollector.OptionInfo;

import java.io.PrintStream;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that creates a plain-text documentation of options.
 */
class OptionPlainTextWriter {

  private static final int CHARS_PER_LINE = 75; // for description

  private final boolean verbose;
  private final PrintStream out;

  // Keep state between different items:
  // We want to group consecutive options with the same description,
  // and we want to show identical option infos only once.
  private String lastDescription = "";
  private String lastInfo = "";

  OptionPlainTextWriter(boolean pVerbose, PrintStream pOut) {
    verbose = pVerbose;
    out = checkNotNull(pOut);
  }

  /**
   * Write output for a single option.
   * @param allInstances All appearances of this option with the same name.
   */
  void writeOption(Iterable<AnnotationInfo> allInstances) {
    boolean first = true;
    for (AnnotationInfo annotation : allInstances) {
      String description = getOptionDescription(annotation.element());
      if (!description.isEmpty() && !lastDescription.equals(description)) {
        if (first) {
          out.append("\n");
          first = false;
        }
        out.append(description);
        lastDescription = description;
      }
    }
    for (OptionInfo option : from(allInstances).filter(OptionInfo.class)) {
      String infoText = getOptionInfo(option);
      if (!lastInfo.equals(infoText)) {
        out.append(infoText);
        lastInfo = infoText;
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
    optionInfo.append(getAllowedValues(info.element(), info.type()));

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

  /** This function returns the allowed values or interval for a field.
   *
   * @param field field with the {@link Option}-annotation
   */
  private String getAllowedValues(final AnnotatedElement field, final Class<?> type) {
    String allowedValues = "";

    // if the type is enum,
    // the allowed values can be extracted the enum-class
    if (type.isEnum()) {
      final Object[] enums = type.getEnumConstants();
      final String[] enumTitles = new String[enums.length];
      for (int i = 0; i < enums.length; i++) {
        enumTitles[i] = ((Enum<?>) enums[i]).name();
      }
      allowedValues =
          "  enum:     "
              + formatText(java.util.Arrays.toString(enumTitles), "             ", false);
    }

    allowedValues += getOptionValues(field);
    allowedValues += getClassOptionValues(field);
    allowedValues += getFileOptionValues(field);
    allowedValues += getIntegerOptionValues(field);
    allowedValues += getTimeSpanOptionValues(field);

    return allowedValues;
  }

  /** This method returns text representing the values,
   * that are defined in the {@link Option}-annotation. */
  private String getOptionValues(AnnotatedElement field) {
    final Option option = field.getAnnotation(Option.class);
    assert option != null;
    String str = "";
    if (option.values().length != 0) {
      str += "  allowed values: " + java.util.Arrays.toString(option.values()) + "\n";
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
  private String getClassOptionValues(AnnotatedElement field) {
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
  private String getFileOptionValues(AnnotatedElement field) {
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
  private String getIntegerOptionValues(AnnotatedElement field) {
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
  private String getTimeSpanOptionValues(AnnotatedElement field) {
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
}
