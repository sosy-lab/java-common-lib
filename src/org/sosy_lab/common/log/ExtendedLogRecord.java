// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.log;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.checkerframework.checker.nullness.qual.Nullable;

/** An extension of {@link LogRecord} that stores additional information. */
public class ExtendedLogRecord extends LogRecord {

  private static final long serialVersionUID = -2531000268930566255L;

  private String componentName = "";

  public ExtendedLogRecord(Level pLevel, @Nullable String pMsg) {
    super(pLevel, pMsg);
  }

  public void setSourceComponentName(String pComponentName) {
    componentName = checkNotNull(pComponentName);
  }

  public String getSourceComponentName() {
    return componentName;
  }

  @Override
  @SuppressWarnings("deprecation") // Java 16 replaces getThreadID() with getLongThreadID()
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("sequenceNumber", getSequenceNumber())
        .add("millis", getMillis())
        .add("loggerName", getLoggerName())
        .add("componentName", componentName)
        .add("sourceClassName", getSourceClassName())
        .add("sourceMethodName", getSourceMethodName())
        .add("threadID", getThreadID())
        .add("level", getLevel())
        .add("message", getMessage())
        .add("parameters", getParameters())
        .add("thrown", getThrown())
        .toString();
  }
}
