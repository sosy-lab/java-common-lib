// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2014-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.rationals;

import com.google.common.testing.AbstractPackageSanityTests;

public class PackageSanityTest extends AbstractPackageSanityTests {
  {
    setDistinctValues(Rational.class, Rational.of(1), Rational.of(2));
  }
}
