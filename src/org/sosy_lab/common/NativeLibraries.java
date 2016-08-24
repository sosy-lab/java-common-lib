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
 * Helper class for loading native libraries.
 *
 * <p>Standard usage is by calling the method {@link NativeLibraries#loadLibrary}
 * with the library name.
 * The following paths are tried in order:
 *
 * <ul>
 *  <li>Standard VM path (as set by the property {@code java.library.path})</li>
 *  <li>{@code native} folder, which is assumed to be two levels above the directory containing the JAR.
 *  One of the following directories is searched within the {@code native} folder:
 *  <ul>
 *      <li>x86_64-linux</li>
 *      <li>x86-linux</li>
 *      <li>x86-windows</li>
 *      <li>x86_64-windows</li>
 *      <li>x86-macosx</li>
 *      <li>x86_64-macosx</li>
 *  </ul>
 *  <li>Same directory as the JAR file holding {@code NativeLibraries.class}.</li>
 * </ul>
 */
public final class NativeLibraries {

  private NativeLibraries() {}

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

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public static Path getNativeLibraryPath() {
    // We expected the libraries to be in the directory lib/native/<arch>-<os>
    // relative to the parent of the code.
    // When the code resides in a JAR file, the JAR file needs to be in the same
    // directory as the "lib" directory.
    // When the code is in .class files, those .class files need to be in a
    // sub-directory of the one with the "lib" directory (e.g., in a "bin" directory).

    if (nativePath == null) {
      String arch = Ascii.toLowerCase(Architecture.guessVmArchitecture().name());
      String os = Ascii.toLowerCase(OS.guessOperatingSystem().name());

      nativePath =
          getPathToJar().getParent().getParent().resolve(Paths.get("native", arch + "-" + os));
    }
    return nativePath;
  }

  /**
   * @return Path to <b>this</b> JAR, holding SoSy Lab-Common library.
   */
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
   * Load a native library.
   * This is similar to {@link System#loadLibrary(String)},
   * but additionally tries more directories for the search path of the library.
   *
   * <p>We first try to load the library via the normal VM way.
   * This way one can use the java.library.path property to point the VM
   * to another file.
   * Only if this fails (which is expected if the user did not specify the library path)
   * we try to load the file from the architecture-specific directory under "lib/native/",
   * <b>assuming</b> that the JAR representing this library is in {@code lib} that the JAR
   * representing this library is in {@code lib}.
   *
   * <p>Finally, if that fails as well, we search in the same directory this {@code JAR} is.
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
   * Find a path for library.
   *
   * <p>We first try an architecture-dependent folder under "lib/native",
   * <b>assuming</b> that the JAR representing this library is in the folder "lib",
   * and if that fails, we try the same folder this JAR is in.
   *
   * @return Found path or {@code Optional.absent()}
   */
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
