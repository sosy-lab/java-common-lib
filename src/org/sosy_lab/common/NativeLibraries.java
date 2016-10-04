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
package org.sosy_lab.common;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.base.Ascii;
import com.google.common.base.StandardSystemProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Helper class for loading native libraries. The methods in this class search for the library
 * binary in some more directories than those specified in the {@literal java.library.path} system
 * property.
 *
 * <p>The searched directories are:
 *
 * <ul>
 *   <li>the same directory as the JAR file of this library
 *   <li>the "native library path" as returned by {@link #getNativeLibraryPath()}, which is the
 *       directory {@literal ../native/<arch>-<os>/} relative to the JAR file of this library, with
 *       {@literal <arch>-<os>} being one of the following values depending on your system:
 *       <ul>
 *         <li>x86_64-linux
 *         <li>x86-linux
 *         <li>x86-windows
 *         <li>x86_64-windows
 *         <li>x86-macosx
 *         <li>x86_64-macosx
 *       </ul>
 *
 * </ul>
 *
 * <p>Standard usage is by calling the method {@link NativeLibraries#loadLibrary} with the library
 * name, or use {@link Classes#makeExtendedURLClassLoader()} and {@link
 * Classes.ClassLoaderBuilder#setCustomLookupNativeLibraries(java.util.function.Predicate)} if
 * third-party code loads the library.
 */
public final class NativeLibraries {

  private NativeLibraries() {}

  @Deprecated // will become private
  public enum OS {
    LINUX,
    MACOSX,
    WINDOWS;

    private static @Nullable OS currentOS = null;

    public static OS guessOperatingSystem() throws UnsatisfiedLinkError {
      if (currentOS != null) {
        return currentOS;
      }

      String prop = StandardSystemProperty.OS_NAME.value();
      if (isNullOrEmpty(prop)) {
        throw new UnsatisfiedLinkError(
            "No value for os.name, "
                + "please report this together with information about your system "
                + "(OS, architecture, JVM).");
      }

      prop = Ascii.toLowerCase(prop.replace(" ", ""));

      if (prop.startsWith("linux")) {
        currentOS = LINUX;
      } else if (prop.startsWith("windows")) {
        currentOS = WINDOWS;
      } else if (prop.startsWith("macosx")) {
        currentOS = MACOSX;
      } else {
        throw new UnsatisfiedLinkError(
            "Unknown value for os.name: '"
                + StandardSystemProperty.OS_NAME.value()
                + "', please report this together with information about your system "
                + "(OS, architecture, JVM).");
      }

      return currentOS;
    }
  }

  @Deprecated // will become private
  public enum Architecture {
    X86,
    X86_64;

    private static @Nullable Architecture currentArch = null;

    public static Architecture guessVmArchitecture() throws UnsatisfiedLinkError {
      if (currentArch != null) {
        return currentArch;
      }

      String prop = System.getProperty("os.arch.data.model");
      if (isNullOrEmpty(prop)) {
        prop = System.getProperty("sun.arch.data.model");
      }

      if (!isNullOrEmpty(prop)) {
        switch (prop) {
          case "32":
            currentArch = Architecture.X86;
            break;
          case "64":
            currentArch = Architecture.X86_64;
            break;
          default:
            throw new UnsatisfiedLinkError(
                "Unknown value for os.arch.data.model: '"
                    + prop
                    + "', please report this together with information about your system "
                    + "(OS, architecture, JVM).");
        }
      } else {

        prop = StandardSystemProperty.JAVA_VM_NAME.value();
        if (!isNullOrEmpty(prop)) {
          prop = Ascii.toLowerCase(prop);

          if (prop.contains("32-bit") || prop.contains("32bit") || prop.contains("i386")) {

            currentArch = Architecture.X86;
          } else if (prop.contains("64-bit")
              || prop.contains("64bit")
              || prop.contains("x64")
              || prop.contains("x86_64")
              || prop.contains("amd64")) {

            currentArch = Architecture.X86_64;
          } else {
            throw new UnsatisfiedLinkError(
                "Unknown value for java.vm.name: '"
                    + prop
                    + "', please report this together with information about your system "
                    + "(OS, architecture, JVM).");
          }
        } else {
          throw new UnsatisfiedLinkError("Could not detect system architecture");
        }
      }

      return currentArch;
    }
  }

  private static @Nullable Path nativePath = null;

  /**
   * Return the "native library path" as defined in the documentation of {@link NativeLibraries},
   * i.e., a directory where members of this class expect native binaries.
   *
   * <p>It is usually recommended to use the high-level method {@link #loadLibrary(String)} instead.
   */
  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public static Path getNativeLibraryPath() {
    if (nativePath == null) {
      String arch = Ascii.toLowerCase(Architecture.guessVmArchitecture().name());
      String os = Ascii.toLowerCase(OS.guessOperatingSystem().name());

      nativePath =
          getPathToJar().getParent().getParent().resolve(Paths.get("native", arch + "-" + os));
    }
    return nativePath;
  }

  /** @return Path to <b>this</b> JAR, holding SoSy Lab-Common library. */
  private static Path getPathToJar() {
    URI pathToJar;
    try {
      pathToJar = NativeLibraries.class.getProtectionDomain().getCodeSource().getLocation().toURI();
    } catch (URISyntaxException e) {
      throw new AssertionError(e);
    }
    return checkNotNull(Paths.get(pathToJar).getParent());
  }

  /**
   * Load a native library. This is similar to {@link System#loadLibrary(String)}, but first
   * searches for the library in some other directories (as explained in {@link NativeLibraries}).
   * If the library cannot be found there, it falls back to {@link System#loadLibrary(String)},
   * which looks in the directories specified in the {@literal java.library.path} system property.
   *
   * <p>If you cannot replace the call to {@link System#loadLibrary(String)}, you can use a class
   * loader created with {@link Classes#makeExtendedURLClassLoader()} and {@link
   * Classes.ClassLoaderBuilder#setCustomLookupNativeLibraries(java.util.function.Predicate)} to let
   * {@link System#loadLibrary(String)} use the same lookup mechanism as this method.
   *
   * @param name A library name as for {@link System#loadLibrary(String)}.
   */
  public static void loadLibrary(String name) {
    Optional<Path> path = findPathForLibrary(name);
    if (path.isPresent()) {
      System.load(path.get().toAbsolutePath().toString());
    } else {
      System.loadLibrary(name);
    }
  }

  /**
   * Search for a native library in some directories as listed in the documentation of {@link
   * NativeLibraries}.
   *
   * @param libraryName A library name as for {@link System#loadLibrary(String)}.
   * @return Found path or {@code Optional.absent()}
   */
  @Deprecated // will become private
  public static Optional<Path> findPathForLibrary(String libraryName) {
    String osLibName = System.mapLibraryName(libraryName);
    Path p = getNativeLibraryPath().resolve(osLibName).toAbsolutePath();
    if (Files.exists(p)) {
      return Optional.of(p);
    }
    p = getPathToJar().resolve(osLibName).toAbsolutePath();
    if (Files.exists(p)) {
      return Optional.of(p);
    }
    return Optional.empty();
  }
}
