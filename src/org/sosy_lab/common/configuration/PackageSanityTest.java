// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.configuration;

import com.google.common.io.CharSource;
import com.google.common.testing.AbstractPackageSanityTests;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.sosy_lab.common.Classes;

public class PackageSanityTest extends AbstractPackageSanityTests {

  {
    ignoreClasses(Classes.IS_GENERATED);

    setDefault(String[].class, new String[] {"test"});
    setDefault(Path.class, Paths.get("test"));
    setDefault(Configuration.class, Configuration.defaultConfiguration());
    setDefault(CharSource.class, CharSource.wrap("test"));
    setDefault(Optional.class, Optional.empty());
  }
}
