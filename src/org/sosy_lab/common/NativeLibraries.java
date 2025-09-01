// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

import com.google.common.base.Ascii;
import com.google.common.base.StandardSystemProperty;
import com.google.errorprone.annotations.Var;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

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
 * </ul>
 *
 * <p>Standard usage is by calling the method {@link NativeLibraries#loadLibrary} with the library
 * name, or use {@link Classes#makeExtendedURLClassLoader()} and {@link
 * Classes.ClassLoaderBuilder#setCustomLookupNativeLibraries(java.util.function.Predicate)} if
 * third-party code loads the library.
 */
@SuppressWarnings("NonFinalStaticField")
public final class NativeLibraries {

  private static final String REPORT_MESSAGE =
      ", please report this together with information "
          + "about your system (OS, architecture, JVM).";

  private NativeLibraries() {}

  @Deprecated // will become private
  @SuppressWarnings("IdentifierName") // cannot rename while public
  public enum OS {
    LINUX,
    MACOSX,
    WINDOWS;

    @SuppressWarnings("MemberName")
    private static @Nullable OS currentOS = null;

    public static OS guessOperatingSystem() {
      if (currentOS != null) {
        return currentOS;
      }

      @Var String prop = StandardSystemProperty.OS_NAME.value();
      if (isNullOrEmpty(prop)) {
        throw new UnsatisfiedLinkError("No value for os.name" + REPORT_MESSAGE);
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
                + "'"
                + REPORT_MESSAGE);
      }

      return currentOS;
    }
  }

  @Deprecated // will become private
  public enum Architecture {
    // We only consider the mainly used architectures and ignore older ones like PPC or MIPS.
    X86,
    X86_64,
    ARM64;

    private static @Nullable Architecture currentArch = null;

    /**
     * Get the architecture of the current JVM, e.g., as required to load binary libraries.
     *
     * <p>In most cases, the JVM architecture matches system architecture. In some cases, the system
     * architecture is not sufficient. For example, we could execute a 32bit JVM on a 64bit system.
     * We cannot check each possible combination, but check only the important ones like X86_64.
     */
    public static Architecture guessVmArchitecture() {
      if (currentArch != null) {
        return currentArch;
      }

      Architecture osArch = guessOsArchitecture();
      if (osArch == X86_64 && isVm32Bit()) {
        currentArch = X86;
      } else {
        currentArch = osArch;
      }

      return currentArch;
    }

    private static Architecture guessOsArchitecture() {
      @Var String osArch = StandardSystemProperty.OS_ARCH.value();
      if (isNullOrEmpty(osArch)) {
        throw new UnsatisfiedLinkError("No value for os.arch" + REPORT_MESSAGE);
      }

      osArch = Ascii.toLowerCase(osArch.replace(" ", ""));

      switch (osArch) {
        case "i386":
        case "i686":
        case "x86":
          return Architecture.X86;
        case "amd64":
        case "x86_64":
          return Architecture.X86_64;
        case "aarch64":
          return Architecture.ARM64;
        default:
          throw new UnsatisfiedLinkError(
              "Unknown value for os.arch: '"
                  + StandardSystemProperty.OS_ARCH.value()
                  + "'"
                  + REPORT_MESSAGE);
      }
    }

    /** Check whether the JVM is executing in 32 bit version. */
    private static boolean isVm32Bit() {
      return Ascii.toLowerCase(nullToEmpty(StandardSystemProperty.JAVA_VM_NAME.value()))
              .matches(".*(32-?bit|i386).*")
          || Objects.equals(System.getProperty("os.arch.data.model"), "32")
          || Objects.equals(System.getProperty("sun.arch.data.model"), "32");
    }
  }

  private static @Nullable Path nativePath = null;

  /**
   * Return the "native library path" as defined in the documentation of {@link NativeLibraries},
   * i.e., a directory where members of this class expect native binaries.
   *
   * <p>It is usually recommended to use the high-level method {@link #loadLibrary(String)} instead.
   */
  public static Path getNativeLibraryPath() {
    if (nativePath == null) {
      String arch = Ascii.toLowerCase(Architecture.guessVmArchitecture().name());
      String os = Ascii.toLowerCase(OS.guessOperatingSystem().name());

      nativePath =
          Classes.getCodeLocation(NativeLibraries.class)
              .getParent()
              .getParent()
              .getParent()
              .resolve(Path.of("native", arch + "-" + os));
    }
    return nativePath;
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
      System.load(path.orElseThrow().toAbsolutePath().toString());
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
    @Var Path p = getNativeLibraryPath().resolve(osLibName).toAbsolutePath();
    if (Files.exists(p)) {
      return Optional.of(p);
    }
    p = Classes.getCodeLocation(NativeLibraries.class).resolveSibling(osLibName).toAbsolutePath();
    if (Files.exists(p)) {
      return Optional.of(p);
    }
    return Optional.empty();
  }
}
