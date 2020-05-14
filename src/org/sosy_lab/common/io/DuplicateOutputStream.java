// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.io;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.OutputStream;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class is an OutputStream implementation that sends everything to two other OutputStreams.
 *
 * <p>Exceptions thrown by any of the streams will be relayed to the caller.
 */
public class DuplicateOutputStream extends OutputStream {

  private final OutputStream stream1;
  private final OutputStream stream2;

  public DuplicateOutputStream(OutputStream pStream1, OutputStream pStream2) {
    stream1 = checkNotNull(pStream1);
    stream2 = checkNotNull(pStream2);
  }

  /**
   * Create an output stream that forwards to all given output streams, ignoring null parameters.
   */
  public static OutputStream mergeStreams(
      @Nullable OutputStream stream1, @Nullable OutputStream stream2) {

    if (stream1 == null) {
      if (stream2 == null) {
        return ByteStreams.nullOutputStream();
      } else {
        return stream2;
      }

    } else {
      if (stream2 == null) {
        return stream1;
      } else {
        return new DuplicateOutputStream(stream1, stream2);
      }
    }
  }

  @Override
  public void write(int pB) throws IOException {
    try {
      stream1.write(pB);
    } finally {
      stream2.write(pB);
    }
  }

  @Override
  public void write(byte[] pB) throws IOException {
    try {
      stream1.write(pB);
    } finally {
      stream2.write(pB);
    }
  }

  @Override
  public void write(byte[] pB, int pOff, int pLen) throws IOException {
    try {
      stream1.write(pB, pOff, pLen);
    } finally {
      stream2.write(pB, pOff, pLen);
    }
  }

  @Override
  public void flush() throws IOException {
    try {
      stream1.flush();
    } finally {
      stream2.flush();
    }
  }

  @Override
  public void close() throws IOException {
    try {
      stream1.close();
    } finally {
      stream2.close();
    }
  }
}
