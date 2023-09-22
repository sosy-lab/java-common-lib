// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2012-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: Yidong Fang <fangyidong@yahoo.com.cn>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common;

import com.google.common.base.Ascii;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.Var;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.io.IO;

/**
 * This class is based on code from the library JSON.simple in version 1.1
 * (https://code.google.com/p/json-simple/).
 *
 * <p>Significant performance improvements were made compared to the library.
 */
@SuppressWarnings("MemberName")
public final class JSON {

  private JSON() {}

  /** Encode an object into JSON text and write it to a file. */
  public static void writeJSONString(@Nullable Object value, Path file) throws IOException {
    // We escape everything, so pure ASCII remains
    try (Writer out = IO.openOutputFile(file, StandardCharsets.US_ASCII)) {
      writeJSONString(value, out);
    }
  }

  /** Encode an object into JSON text and write it to out. */
  public static void writeJSONString(@Nullable Object value, Appendable out) throws IOException {
    if (value == null) {
      out.append("null");

    } else if (value instanceof CharSequence) {
      out.append('\"');
      escape((CharSequence) value, out);
      out.append('\"');

    } else if (value instanceof Double) {
      if (((Double) value).isInfinite() || ((Double) value).isNaN()) {
        out.append("null");
      } else {
        out.append(value.toString());
      }

    } else if (value instanceof Float) {
      if (((Float) value).isInfinite() || ((Float) value).isNaN()) {
        out.append("null");
      } else {
        out.append(value.toString());
      }

    } else if ((value instanceof Number) || (value instanceof Boolean)) {
      out.append(value.toString());

    } else if (value instanceof Map<?, ?>) {
      writeJSONString((Map<?, ?>) value, out);

    } else if (value instanceof Path) {
      // Path is also an Iterable, but typically desired to be written as a single String.
      writeJSONString(value.toString(), out);

    } else if (value instanceof Iterable<?>) {
      writeJSONString((Iterable<?>) value, out);

    } else {
      throw new NotSerializableException(
          "Object of class " + value.getClass().getName() + " cannot be written as JSON");
    }
  }

  /** Encode an list into JSON text and write it to out. */
  private static void writeJSONString(Iterable<?> list, Appendable out) throws IOException {
    @Var boolean first = true;

    out.append('[');
    for (Object value : list) {
      if (first) {
        first = false;
      } else {
        out.append(',');
      }

      JSON.writeJSONString(value, out);
    }
    out.append(']');
  }

  /** Encode a map into JSON text and write it to out. */
  private static void writeJSONString(Map<?, ?> map, Appendable out) throws IOException {
    @Var boolean first = true;

    out.append('{');
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (first) {
        first = false;
      } else {
        out.append(',');
      }
      out.append('\"');
      escape(String.valueOf(entry.getKey()), out);
      out.append('\"');
      out.append(':');
      writeJSONString(entry.getValue(), out);
    }
    out.append('}');
  }

  /**
   * Escape quotes, \, /, \r, \n, \b, \f, \t and other control characters (U+0000 through U+001F).
   */
  private static void escape(CharSequence s, Appendable out) throws IOException {
    for (int i = 0; i < s.length(); i++) {
      char ch = s.charAt(i);
      switch (ch) {
        case '"':
          out.append("\\\"");
          break;
        case '\\':
          out.append("\\\\");
          break;
        case '\b':
          out.append("\\b");
          break;
        case '\f':
          out.append("\\f");
          break;
        case '\n':
          out.append("\\n");
          break;
        case '\r':
          out.append("\\r");
          break;
        case '\t':
          out.append("\\t");
          break;
        case '/':
          out.append("\\/");
          break;
        default:
          // Reference: http://www.unicode.org/versions/Unicode5.1.0/
          if ((ch <= '\u001F')
              || (ch >= '\u007F' && ch <= '\u009F')
              || (ch >= '\u2000' && ch <= '\u20FF')) {
            String ss = Ascii.toUpperCase(Integer.toHexString(ch));
            out.append("\\u");
            out.append(Strings.padStart(ss, 4, '0'));
          } else {
            out.append(ch);
          }
      }
    }
  }
}
