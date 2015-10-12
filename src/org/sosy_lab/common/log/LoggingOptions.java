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

import com.google.common.collect.ImmutableList;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;

import java.util.List;
import java.util.logging.Level;

@Options(prefix = "log",
    description = "Possible log levels in descending order "
    + "\n(lower levels include higher ones):"
    + "\nOFF:      no logs published"
    + "\nSEVERE:   error messages"
    + "\nWARNING:  warnings"
    + "\nINFO:     messages"
    + "\nFINE:     logs on main application level"
    + "\nFINER:    logs on central CPA algorithm level"
    + "\nFINEST:   logs published by specific CPAs"
    + "\nALL:      debugging information"
    + "\nCare must be taken with levels of FINER or lower, as output files may "
    + "become quite large and memory usage might become an issue.")
final class LoggingOptions {

  @Option(secure=true, name="level", toUppercase=true, description="log level of file output")
  private Level fileLevel = Level.OFF;

  @Option(secure=true, toUppercase=true, description="log level of console output")
  private Level consoleLevel = Level.INFO;

  @Option(secure=true, toUppercase=true,
      description="single levels to be excluded from being logged")
  private List<Level> fileExclude = ImmutableList.of();

  @Option(secure=true, toUppercase=true,
      description="single levels to be excluded from being logged")
  private List<Level> consoleExclude = ImmutableList.of();

  @Option(secure=true, name="file",
      description="name of the log file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path outputFile = Paths.get("CPALog.txt");

  @Option(secure=true,
      description="maximum size of log output strings before they will be truncated")
  @IntegerOption(min=1)
  private int truncateSize = 10000;

  LoggingOptions(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  Level getFileLevel() {
    return fileLevel;
  }

  Level getConsoleLevel() {
    return consoleLevel;
  }

  List<Level> getFileExclude() {
    return fileExclude;
  }

  List<Level> getConsoleExclude() {
    return consoleExclude;
  }

  Path getOutputFile() {
    return outputFile;
  }

  int getTruncateSize() {
    return truncateSize;
  }
}
