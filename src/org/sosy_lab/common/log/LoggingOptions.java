// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.log;

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

@Options(
    prefix = "log",
    description =
        "Possible log levels in descending order "
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
public class LoggingOptions {

  @Option(
      secure = true,
      name = "level",
      toUppercase = true,
      description = "log level of file output")
  private Level fileLevel = Level.OFF;

  @Option(secure = true, toUppercase = true, description = "log level of console output")
  private Level consoleLevel = Level.INFO;

  @Option(
      secure = true,
      toUppercase = true,
      description = "single levels to be excluded from being logged")
  private ImmutableList<Level> fileExclude = ImmutableList.of();

  @Option(
      secure = true,
      toUppercase = true,
      description = "single levels to be excluded from being logged")
  private ImmutableList<Level> consoleExclude = ImmutableList.of();

  @Option(secure = true, name = "file", description = "name of the log file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path outputFile = Path.of("CPALog.txt");

  @Option(
      secure = true,
      description =
          "Maximum size of log output strings before they will be truncated."
              + " Note that truncation is not precise and truncation to small values has no effect."
              + " Use 0 for disabling truncation completely.")
  @IntegerOption(min = 0)
  private int truncateSize = 10000;

  @Option(secure = true, description = "use colors for log messages on console")
  private boolean useColors = true;

  public LoggingOptions(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  /**
   * This constructor is for inheritance, thus allowing users to use this class without sosy-lab's
   * {@link Configuration}.
   */
  protected LoggingOptions() {}

  public Level getFileLevel() {
    return fileLevel;
  }

  public Level getConsoleLevel() {
    return consoleLevel;
  }

  public List<Level> getFileExclude() {
    return fileExclude;
  }

  public List<Level> getConsoleExclude() {
    return consoleExclude;
  }

  public Path getOutputFile() {
    return outputFile;
  }

  public int getTruncateSize() {
    return truncateSize;
  }

  public boolean useColors() {
    return useColors;
  }
}
