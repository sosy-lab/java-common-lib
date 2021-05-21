// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.io;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.StandardSystemProperty;
import com.google.common.base.Strings;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Utilities for temporary files. */
public class TempFile {

  private static final Path TMPDIR = Paths.get(StandardSystemProperty.JAVA_IO_TMPDIR.value());

  private TempFile() {}

  /** Create a builder for temporary files. */
  public static TempFileBuilder builder() {
    return new TempFileBuilder();
  }

  public static final class TempFileBuilder {

    private Path dir = TMPDIR;
    private String suffix = ".tmp";
    private @Nullable String prefix;
    private @Nullable Object content;
    private @Nullable Charset charset;
    private boolean deleteOnJvmExit = true;
    private FileAttribute<?>[] fileAttributes = new FileAttribute<?>[0];

    private TempFileBuilder() {}

    /** The directory where the file will be created, default is JVM's temp directory. */
    @CanIgnoreReturnValue
    public TempFileBuilder dir(Path pDir) {
      dir = checkNotNull(pDir);
      return this;
    }

    /** Prefix of randomly-generated file name. */
    @CanIgnoreReturnValue
    public TempFileBuilder prefix(String pPrefix) {
      prefix = checkNotNull(pPrefix);
      return this;
    }

    /** Suffix of randomly generated file name, default is <code>.tmp</code>. */
    @CanIgnoreReturnValue
    public TempFileBuilder suffix(String pSuffix) {
      suffix = checkNotNull(pSuffix);
      return this;
    }

    /** Content to write to temp file immediately after creation. */
    @CanIgnoreReturnValue
    public TempFileBuilder initialContent(Object pContent, Charset pCharset) {
      checkNotNull(pContent);
      checkNotNull(pCharset);
      content = pContent;
      charset = pCharset;
      return this;
    }

    /** Do not automatically delete the file on JVM exit with {@link File#deleteOnExit()}. */
    @CanIgnoreReturnValue
    public TempFileBuilder noDeleteOnJvmExit() {
      deleteOnJvmExit = false;
      return this;
    }

    /** Use the specified {@link FileAttribute}s for creating the file. */
    @CanIgnoreReturnValue
    public TempFileBuilder fileAttributes(FileAttribute<?>... pFileAttributes) {
      fileAttributes = pFileAttributes.clone();
      return this;
    }

    /**
     * Create a fresh temporary file according to the specifications set on this builder.
     *
     * <p>If the temporary file should be removed after some specific code is executed, use {@link
     * #createDeleteOnClose()}.
     *
     * <p>This instance can be safely used again afterwards.
     */
    public Path create() throws IOException {
      Path file;
      try {
        file = Files.createTempFile(dir, prefix, suffix, fileAttributes);
      } catch (IOException e) {
        throw improveIoExceptionMessage(e, dir, prefix + "*" + suffix);
      }

      if (deleteOnJvmExit) {
        file.toFile().deleteOnExit();
      }

      if (content != null) {
        try {
          IO.writeFile(file, charset, content);
        } catch (IOException e) {
          // creation was successful, but writing failed
          // -> delete file
          try {
            Files.delete(file);
          } catch (IOException deleteException) {
            e.addSuppressed(deleteException);
          }
          throw e;
        }
      }
      return file;
    }

    /**
     * Create a fresh temporary file according to the specifications set on this builder.
     *
     * <p>The resulting {@link Path} object is wrapped in a {@link DeleteOnCloseFile}, which deletes
     * the file as soon as {@link DeleteOnCloseFile#close()} is called.
     *
     * <p>It is recommended to use the following pattern: <code>
     * try (DeleteOnCloseFile tempFile =
     *     TempFile.builder()[.. adjust builder ..].createDeleteOnClose()) {
     *   // use tempFile.toPath() for writing and reading of the temporary file
     * }
     * </code> The file can be opened and closed multiple times, potentially from different
     * processes.
     *
     * <p>This instance can be safely used again afterwards.
     */
    public DeleteOnCloseFile createDeleteOnClose() throws IOException {
      return new DeleteOnCloseFile(create());
    }
  }

  /**
   * A simple wrapper around {@link Path} that calls {@link Files#deleteIfExists(Path)} from {@link
   * AutoCloseable#close()}.
   */
  @SuppressWarnings("deprecation")
  @Immutable
  public static final class DeleteOnCloseFile extends MoreFiles.DeleteOnCloseFile
      implements AutoCloseable {

    private DeleteOnCloseFile(Path pFile) {
      super(pFile);
    }

    public ByteSource toByteSource() {
      return com.google.common.io.MoreFiles.asByteSource(toPath());
    }

    public ByteSink toByteSink() {
      return com.google.common.io.MoreFiles.asByteSink(toPath());
    }

    public CharSource toCharSource(Charset charset) {
      return com.google.common.io.MoreFiles.asCharSource(toPath(), charset);
    }

    public CharSink toCharSink(Charset charset) {
      return com.google.common.io.MoreFiles.asCharSink(toPath(), charset);
    }

    @Override
    @SuppressWarnings("RedundantOverride") // to avoid deprecation warning when method is called
    public Path toPath() {
      return super.toPath();
    }

    @Override
    @SuppressWarnings("RedundantOverride") // to avoid deprecation warning when method is called
    public void close() throws IOException {
      super.close();
    }
  }

  /**
   * Create a fresh temporary directory in JVM's temp directory.
   *
   * <p>The resulting {@link Path} object is wrapped in a {@link DeleteOnCloseDir}, which deletes
   * the directory recursively including its contents as soon as {@link DeleteOnCloseDir#close()} is
   * called.
   *
   * <p>It is recommended to use the following pattern: <code>
   * try (DeleteOnCloseDir tempDir = TempFile.createDeleteOnCloseDir()) {
   *   // use tempDir.toPath() to get the Path object denoting the temporary directory
   * }
   * </code>
   *
   * @param pPrefix The prefix of the randomly-generated directory name.
   * @param pFileAttributes The {@link FileAttribute}s used for creating the directory.
   */
  public static DeleteOnCloseDir createDeleteOnCloseDir(
      String pPrefix, FileAttribute<?>... pFileAttributes) throws IOException {
    checkNotNull(pPrefix);
    Path tempDir;
    try {
      tempDir = Files.createTempDirectory(TMPDIR, pPrefix, pFileAttributes);
    } catch (IOException e) {
      throw improveIoExceptionMessage(e, TMPDIR, pPrefix + "*");
    }
    return new DeleteOnCloseDir(tempDir);
  }

  /**
   * A simple wrapper around {@link Path} that calls {@link
   * com.google.common.io.MoreFiles#deleteRecursively(Path,
   * com.google.common.io.RecursiveDeleteOption...)} recursively from {@link AutoCloseable#close()}
   * to delete the directory including its contents.
   */
  @Immutable
  public static final class DeleteOnCloseDir implements AutoCloseable {

    private final Path path;

    private DeleteOnCloseDir(Path pDir) {
      path = pDir;
    }

    public Path toPath() {
      return path;
    }

    /**
     * Recursively delete all files and directories in the directory represented by this instance.
     */
    @Override
    public void close() throws IOException {
      com.google.common.io.MoreFiles.deleteRecursively(path);
    }
  }

  private static IOException improveIoExceptionMessage(
      IOException pException, Path pDir, String pPathString) {
    // The message of this exception is often quite unhelpful,
    // improve it by adding the path were we attempted to write.
    if (pException.getMessage() != null && pException.getMessage().contains(pDir.toString())) {
      return pException;
    }

    String fileName = pDir.resolve(pPathString).toString();
    if (Strings.nullToEmpty(pException.getMessage()).isEmpty()) {
      return new IOException(fileName, pException);
    } else {
      return new IOException(fileName + " (" + pException.getMessage() + ")", pException);
    }
  }
}
