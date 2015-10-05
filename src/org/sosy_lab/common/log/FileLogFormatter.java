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
package org.sosy_lab.common.log;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Class to handle formatting for file output.
 */
public class FileLogFormatter extends Formatter {

  private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

  @Override
  public String format(LogRecord lr) {
    StringBuffer sb = new StringBuffer();

    dateFormat.format(new Date(lr.getMillis()), sb, new FieldPosition(0));
    sb.append("\t");

    sb.append(lr.getLevel());
    sb.append("\t");

    if (lr instanceof ExtendedLogRecord) {
      String component = ((ExtendedLogRecord) lr).getSourceComponentName();
      if (!component.isEmpty()) {
        sb.append(component);
        sb.append(":");
      }
    }
    sb.append(LogUtils.extractSimpleClassName(lr));
    sb.append(".");
    sb.append(lr.getSourceMethodName());
    sb.append("\t");

    sb.append(lr.getMessage());

    sb.append("\n\n");
    return sb.toString();
  }
}