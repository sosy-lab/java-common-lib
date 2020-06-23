// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.log;

import com.google.common.base.Predicates;
import com.google.common.testing.AbstractPackageSanityTests;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

@SuppressWarnings("deprecation")
public class PackageSanityTest extends AbstractPackageSanityTests {

  {
    setDefault(Handler.class, new StringBuildingLogHandler());
    setDefault(Level.class, Level.ALL);
    setDefault(Formatter.class, new SimpleFormatter());
    setDefault(LogRecord.class, new LogRecord(Level.ALL, "test"));

    setDefault(Configuration.class, Configuration.defaultConfiguration());
    try {
      setDefault(LoggingOptions.class, new LoggingOptions(Configuration.defaultConfiguration()));
    } catch (InvalidConfigurationException e) {
      throw new AssertionError(e);
    }

    // NullLogManager does not do any checkNotNull checks on purpose
    ignoreClasses(Predicates.<Class<?>>equalTo(NullLogManager.class));
  }
}
