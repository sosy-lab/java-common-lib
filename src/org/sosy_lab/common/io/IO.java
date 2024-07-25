// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.io;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.CharSource;
import com.google.common.io.MoreFiles;
import com.google.errorprone.annotations.Var;
import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.Console;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPOutputStream;
import org.sosy_lab.common.Appenders;

/**
 * Provides helper functions for file access, in addition to {@link Files} and {@link
 * com.google.common.io.MoreFiles}.
 */
public final class IO {

  private IO() {
    /* utility class */
  }

  private static final Charset NATIVE_CHARSET;

  static {
    String nativeEncoding = System.getProperty("native.encoding");
    @Var Charset charset = Charset.defaultCharset();
    if (nativeEncoding != null) {
      // Java 17 or newer
      try {
        charset = Charset.forName(nativeEncoding);
      } catch (IllegalCharsetNameException | UnsupportedCharsetException ignored) {
        // unlikely that system property set by JVM has illegal charset,
        // and fallback to Charset.defaultCharset() is better than crashing the whole application.
      }
    }
    NATIVE_CHARSET = charset;
  }

  /**
   * Return the {@link Charset} that is used by the underlying platform, i.e., configured as the OS
   * default. On modern systems this is typically {@link Charsets#UTF_8} with the exception of
   * Windows.
   *
   * <p>The native encoding should typically used when outputting something to the console (which is
   * handled by Java automatically) and is also the common agreement when processes are
   * communicating via pipes, i.e., for {@link Process#getInputStream()}, {@link
   * Process#getOutputStream()}, and {@link Process#getErrorStream()}.
   *
   * <p>Note that Java 17 also provides {@code Console#charset()} which should always return the
   * same value, this method is a convenience method for simplifying code that has to work with both
   * old and new Java versions.
   *
   * <p>Before Java 18 the method {@link Charset#defaultCharset()} also returns the native charset,
   * but it was <a href="https://openjdk.java.net/jeps/400">changed</a> to return UTF-8 in Java 18.
   *
   * <p>If the charset of the JVM is overwritten by setting some system properties like {@code
   * file.encoding} this method may return the specified charset instead of the "real" native
   * charset (just like {@link Charset#defaultCharset()} already did).
   *
   * <p>More information can be found in <a href="https://openjdk.java.net/jeps/400">JEP 400</a>.
   */
  public static Charset getNativeCharset() {
    return NATIVE_CHARSET;
  }

  /**
   * Check whether {@link System#console()} represents a terminal. Java until version 21 and Java
   * 22+ represent this differently and this method works for all Java versions.
   *
   * <p>Cf. the <a href="https://errorprone.info/bugpattern/SystemConsoleNull">Error Prone docs</a>,
   * where this code is taken from.
   */
  @SuppressWarnings("SystemConsoleNull")
  static boolean systemConsoleIsTerminal() {
    Console systemConsole = System.console();
    if (systemConsole == null) {
      return false;
    }
    if (Runtime.version().feature() < 22) {
      return true;
    } else {
      try {
        return (Boolean) Console.class.getMethod("isTerminal").invoke(systemConsole);
      } catch (ReflectiveOperationException e) {
        throw new LinkageError(e.getMessage(), e);
      }
    }
  }

  /**
   * Determine whether it is advisable to use color (via escape sequences) for output on
   * stdout/stderr. This method checks for output redirection, the OS, and the NO_COLOR environment
   * variable.
   */
  public static boolean mayUseColorForOutput() {
    // Using colors is only good if both stdout/stderr are connected to a terminal and not
    // redirected into a file.
    // AFAIK there is no way to determine this from Java, but at least there
    // is a way to determine whether stdout is connected to a terminal.
    // We assume that most users only redirect stderr if they also redirect
    // stdout, so this should be ok.
    return IO.systemConsoleIsTerminal()
        // Windows terminal does not support colors
        && !System.getProperty("os.name", "").startsWith("Windows")
        // https://no-color.org/
        && Strings.isNullOrEmpty(System.getenv("NO_COLOR"));
  }

  /** Read the full content of a {@link CharSource} to a new {@link StringBuilder}. */
  public static StringBuilder toStringBuilder(CharSource source) throws IOException {
    StringBuilder sb = new StringBuilder();
    source.copyTo(sb);
    return sb;
  }

  /** Read the full content of a {@link CharSource} to a char array. */
  public static char[] toCharArray(CharSource source) throws IOException {
    // On newer Java, StringBuilder internally uses byte[] instead of char[].
    // CharArrayWriter uses char[], so copying into the result array can be optimized more easily.
    // Code from https://github.com/google/guava/issues/2713#issuecomment-516574887
    CharArrayWriter writer = new CharArrayWriter();
    source.copyTo(writer);
    return writer.toCharArray();
  }

  /**
   * Writes content to a file.
   *
   * @param file The file.
   * @param content The content which shall be written.
   */
  public static void writeFile(Path file, Charset charset, Object content) throws IOException {
    checkNotNull(content);
    try (Writer w = openOutputFile(file, charset)) {
      Appenders.appendTo(w, content);
    }
  }

  /**
   * Writes content to a file compressed in GZIP format.
   *
   * @param file The file.
   * @param content The content which shall be written.
   */
  @SuppressWarnings("MemberName")
  public static void writeGZIPFile(Path file, Charset charset, Object content) throws IOException {
    checkNotNull(content);
    checkNotNull(charset);
    MoreFiles.createParentDirectories(file);
    try (OutputStream outputStream = Files.newOutputStream(file);
        OutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
        Writer outputStreamWriter = new OutputStreamWriter(gzipOutputStream, charset);
        Writer w = new BufferedWriter(outputStreamWriter)) {
      Appenders.appendTo(w, content);
    }
  }

  /**
   * Open a buffered Writer to a file. This method creates necessary parent directories beforehand.
   */
  public static Writer openOutputFile(Path file, Charset charset, OpenOption... options)
      throws IOException {
    checkNotNull(charset);
    checkNotNull(options);
    MoreFiles.createParentDirectories(file);
    return MoreFiles.asCharSink(file, charset, options).openBufferedStream();
  }

  /**
   * Appends content to a file (without overwriting the file, but creating it if necessary).
   *
   * @param file The file.
   * @param content The content which will be written to the end of the file.
   */
  public static void appendToFile(Path file, Charset charset, Object content) throws IOException {
    checkNotNull(content);
    try (Writer w = openOutputFile(file, charset, StandardOpenOption.APPEND)) {
      Appenders.appendTo(w, content);
    }
  }

  /**
   * Verifies if a file exists, is a normal file and is readable. If this is not the case, a
   * FileNotFoundException with a nice message is thrown.
   *
   * @param path The file to check.
   * @throws FileNotFoundException If one of the conditions is not true.
   */
  public static void checkReadableFile(Path path) throws FileNotFoundException {
    checkNotNull(path);

    if (!Files.exists(path)) {
      throw new FileNotFoundException("File " + path.toAbsolutePath() + " does not exist!");
    }

    if (!Files.isRegularFile(path)) {
      throw new FileNotFoundException("File " + path.toAbsolutePath() + " is not a normal file!");
    }

    if (!Files.isReadable(path)) {
      throw new FileNotFoundException("File " + path.toAbsolutePath() + " is not readable!");
    }
  }
}
